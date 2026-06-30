package com.codesec.gitlab;

import com.codesec.gitlab.model.GitLabApiException;
import com.codesec.gitlab.model.MrChangesResponse;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Core GitLab API client using OkHttp.
 *
 * <p>Provides authenticated access to GitLab REST API with automatic error
 * handling and retry for retryable errors (429 / 5xx).
 */
public class GitLabClient {
    private static final Logger log = LoggerFactory.getLogger(GitLabClient.class);

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final GitLabProperties properties;
    private final String baseApiUrl;

    public GitLabClient(GitLabProperties properties) {
        this.properties = properties;
        this.baseApiUrl = properties.baseUrl().replaceAll("/+$", "") + "/api/v4";

        this.objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(properties.connectTimeoutSeconds()))
            .readTimeout(Duration.ofSeconds(properties.readTimeoutSeconds()))
            .writeTimeout(Duration.ofSeconds(properties.readTimeoutSeconds()))
            .addInterceptor(this::authInterceptor)
            .addInterceptor(this::logInterceptor)
            .build();
    }

    /** Add Private-Token header to every request. */
    private Response authInterceptor(Interceptor.Chain chain) throws IOException {
        Request request = chain.request().newBuilder()
            .header("PRIVATE-TOKEN", properties.privateToken())
            .build();
        return chain.proceed(request);
    }

    /** Log request/response at DEBUG level. */
    private Response logInterceptor(Interceptor.Chain chain) throws IOException {
        Request request = chain.request();
        log.debug("GitLab API {} {}", request.method(), request.url());
        long start = System.nanoTime();
        Response response = chain.proceed(request);
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        log.debug("GitLab API {} {} → {} ({}ms)",
            request.method(), request.url(), response.code(), elapsedMs);
        return response;
    }

    // ─── Public API methods ────────────────────────────────────────────

    /**
     * Probes GitLab availability by calling GET /api/v4/version.
     * @return true if GitLab is reachable
     */
    public boolean probeVersion() {
        try {
            Request request = new Request.Builder()
                .url(baseApiUrl + "/version")
                .get()
                .build();
            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (IOException e) {
            log.debug("GitLab version probe failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * GET /api/v4/projects/{id}/merge_requests/{iid}/changes
     */
    public MrChangesResponse getMrChanges(long projectId, long mrIid) throws GitLabApiException {
        String url = baseApiUrl + "/projects/" + projectId + "/merge_requests/" + mrIid + "/changes";
        Request request = new Request.Builder().url(url).get().build();
        return executeWithRetry(request, MrChangesResponse.class, 3);
    }

    /**
     * POST /api/v4/projects/{id}/merge_requests/{iid}/notes
     */
    public void postMrNote(long projectId, long mrIid, String body) throws GitLabApiException {
        String url = baseApiUrl + "/projects/" + projectId + "/merge_requests/" + mrIid + "/notes";

        RequestBody requestBody = RequestBody.create(
            body, MediaType.parse("application/json; charset=utf-8"));

        // Build JSON body: {"body": "..."}
        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(Map.of("body", body));
            requestBody = RequestBody.create(
                jsonBody, MediaType.parse("application/json; charset=utf-8"));
        } catch (IOException e) {
            throw new GitLabApiException(0, "Failed to serialize note body", e);
        }

        Request request = new Request.Builder()
            .url(url)
            .post(requestBody)
            .build();

        executeWithRetry(request, Map.class, 3);
    }

    // ─── Internal ──────────────────────────────────────────────────────

    private <T> T executeWithRetry(Request request, Class<T> responseType, int retriesLeft)
        throws GitLabApiException {

        try {
            Response response = httpClient.newCall(request).execute();
            int code = response.code();

            if (code >= 200 && code < 300) {
                String body = response.body() != null ? response.body().string() : "{}";
                response.close();
                try {
                    return objectMapper.readValue(body, responseType);
                } catch (IOException e) {
                    // For void-ish responses, return null
                    if (responseType == Map.class && body.contains("\"id\"")) {
                        @SuppressWarnings("unchecked")
                        T result = (T) objectMapper.readValue(body, Map.class);
                        return result;
                    }
                    log.debug("Response body (non-JSON): {}...", body.substring(0, Math.min(200, body.length())));
                    if (responseType == Map.class) {
                        @SuppressWarnings("unchecked")
                        T empty = (T) Map.of();
                        return empty;
                    }
                    throw new GitLabApiException(code, "Failed to parse API response", e);
                }
            }

            String errorBody = "";
            try {
                if (response.body() != null) {
                    errorBody = response.body().string();
                }
            } catch (IOException ignored) {}
            response.close();

            GitLabApiException ex = new GitLabApiException(code,
                "GitLab API " + request.method() + " " + request.url() + " → " + code + " " + errorBody);

            if (ex.isRetryable() && retriesLeft > 0) {
                long delayMs = retryDelayMs(3 - retriesLeft + 1);
                log.warn("GitLab API {} → retrying in {}ms ({} retries left)",
                    code, delayMs, retriesLeft);
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw ex;
                }
                return executeWithRetry(request, responseType, retriesLeft - 1);
            }

            throw ex;

        } catch (IOException e) {
            if (retriesLeft > 0) {
                log.warn("GitLab API I/O error, retrying ({} retries left): {}", retriesLeft, e.getMessage());
                try {
                    Thread.sleep(retryDelayMs(4 - retriesLeft));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new GitLabApiException(0, "Retry interrupted", e);
                }
                return executeWithRetry(request, responseType, retriesLeft - 1);
            }
            throw new GitLabApiException(0, "GitLab API I/O error: " + e.getMessage(), e);
        }
    }

    /** Exponential backoff: 1s, 4s, 16s */
    private long retryDelayMs(int attempt) {
        return (long) Math.pow(4, attempt - 1) * 1000L;
    }

    /** For testing: get the base API URL. */
    String baseApiUrl() {
        return baseApiUrl;
    }
}
