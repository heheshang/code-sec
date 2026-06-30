package com.codesec.gitlab.model;

/**
 * Exception for GitLab API errors.
 */
public class GitLabApiException extends RuntimeException {

    private final int statusCode;

    public GitLabApiException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public GitLabApiException(int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int statusCode() {
        return statusCode;
    }

    /** 401 Unauthorized */
    public boolean isUnauthorized() {
        return statusCode == 401;
    }

    /** 403 Forbidden */
    public boolean isForbidden() {
        return statusCode == 403;
    }

    /** 404 Not Found */
    public boolean isNotFound() {
        return statusCode == 404;
    }

    /** 429 Too Many Requests */
    public boolean isRateLimited() {
        return statusCode == 429;
    }

    /** 5xx Server Error */
    public boolean isServerError() {
        return statusCode >= 500 && statusCode < 600;
    }

    /** Whether this error is retryable (server error or rate limit). */
    public boolean isRetryable() {
        return isServerError() || isRateLimited();
    }
}
