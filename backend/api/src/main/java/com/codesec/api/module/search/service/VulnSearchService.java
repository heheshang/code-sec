package com.codesec.api.module.search.service;

import com.codesec.api.module.search.dto.SearchRequest;
import com.codesec.api.module.search.dto.SearchResponse;
import com.codesec.api.module.search.model.VulnDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Service
public class VulnSearchService {

    private static final Logger log = LoggerFactory.getLogger(VulnSearchService.class);

    private final NamedParameterJdbcTemplate jdbc;

    public VulnSearchService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public SearchResponse<VulnDocument> search(SearchRequest request) {
        long startMs = System.currentTimeMillis();
        List<String> warnings = new ArrayList<>();
        List<String> conditions = new ArrayList<>();
        var params = new MapSqlParameterSource();

        validateRequest(request);

        int pageSize = Math.min(request.getPageSize(), 100);
        if (request.getPageSize() > 100) {
            warnings.add("page_size_clamped: 100");
        }

        String q = sanitizeQuery(request.getQ());
        boolean hasQuery = q != null && !q.isBlank();

        if (hasQuery) {
            params.addValue("q", q);
            conditions.add("(v.tsv_title_desc @@ plainto_tsquery('codesec_cfg', :q)" +
                         " OR v.tsv_code_snippet @@ plainto_tsquery('codesec_cfg', :q))");
        }

        addInCondition(params, conditions, "v.severity", "severity", request.getSeverity());
        addInCondition(params, conditions, "v.exploitability", "exploitability", request.getExploitability());
        addInCondition(params, conditions, "v.project_id", "projectId", request.getProjectId());
        addInCondition(params, conditions, "v.engine", "engine", request.getEngine());

        if (request.getDiscoveredAtFrom() != null || request.getDiscoveredAtTo() != null) {
            String range = buildDateRange(request, params);
            if (range != null) conditions.add(range);
        }

        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);

        String countSql = "SELECT COUNT(*) FROM vuln_finding v" + where;
        long total = jdbc.queryForObject(countSql, params, Long.class);

        String orderBy;
        if ("discovered_at".equals(request.getSortBy())) {
            orderBy = "v.discovered_at " + ("asc".equals(request.getSortOrder()) ? "ASC" : "DESC");
        } else if (hasQuery) {
            orderBy = "ts_rank(v.tsv_title_desc, plainto_tsquery('codesec_cfg', :q)) DESC, v.discovered_at DESC";
        } else {
            orderBy = "v.discovered_at DESC";
        }

        String hlExpr = hasQuery
            ? ", COALESCE(ts_headline('codesec_cfg', v.title, plainto_tsquery('codesec_cfg', :q), 'StartSel=<em>, StopSel=</em>, MaxWords=50, MinWords=20'), v.title) as hl_title" +
              ", COALESCE(ts_headline('codesec_cfg', v.description, plainto_tsquery('codesec_cfg', :q), 'StartSel=<em>, StopSel=</em>, MaxWords=80, MinWords=30'), v.description) as hl_desc" +
              ", COALESCE(ts_headline('codesec_cfg', v.code_snippet, plainto_tsquery('codesec_cfg', :q), 'StartSel=<em>, StopSel=</em>, MaxWords=100, MinWords=40'), v.code_snippet) as hl_snippet"
            : ", v.title as hl_title, v.description as hl_desc, v.code_snippet as hl_snippet";

        int offset = (request.getPage() - 1) * pageSize;
        params.addValue("limit", pageSize);
        params.addValue("offset", offset);

        String dataSql = "SELECT v.id, v.project_id, v.rule_id, v.severity, v.exploitability," +
                        " v.title, v.description, v.code_snippet, v.file_path, v.cwe," +
                        " v.engine, to_char(v.discovered_at, 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"') as discovered_at," +
                        " v.discovered_by" + hlExpr +
                        " FROM vuln_finding v" + where +
                        " ORDER BY " + orderBy +
                        " LIMIT :limit OFFSET :offset";

        Map<String, List<String>> highlights = new LinkedHashMap<>();
        List<VulnDocument> items = jdbc.query(dataSql, params, (rs, rowNum) -> mapVulnDocument(rs, highlights));

        long tookMs = System.currentTimeMillis() - startMs;
        log.info("PG vuln search: q='{}', total={}, took={}ms", q, total, tookMs);

        return SearchResponse.<VulnDocument>builder()
                .total(total)
                .page(request.getPage())
                .pageSize(pageSize)
                .tookMs(tookMs)
                .items(items)
                .highlights(highlights.isEmpty() ? null : highlights)
                .warnings(warnings.isEmpty() ? null : warnings)
                .build();
    }

    private VulnDocument mapVulnDocument(ResultSet rs, Map<String, List<String>> highlights) throws SQLException {
        VulnDocument doc = new VulnDocument();
        String id = rs.getString("id");
        doc.setId(id);
        doc.setProjectId(rs.getString("project_id"));
        doc.setRuleId(rs.getString("rule_id"));
        doc.setSeverity(rs.getString("severity"));
        doc.setExploitability(rs.getString("exploitability"));
        doc.setTitle(rs.getString("title"));
        doc.setDescription(rs.getString("description"));
        doc.setCodeSnippet(rs.getString("code_snippet"));
        doc.setFilePath(rs.getString("file_path"));
        doc.setCwe(rs.getString("cwe"));
        doc.setEngine(rs.getString("engine"));
        doc.setDiscoveredAt(rs.getString("discovered_at"));
        doc.setDiscoveredBy(rs.getString("discovered_by"));

        addHighlight(highlights, id, "title", rs.getString("hl_title"));
        addHighlight(highlights, id, "description", rs.getString("hl_desc"));
        addHighlight(highlights, id, "code_snippet", rs.getString("hl_snippet"));

        return doc;
    }

    private void addHighlight(Map<String, List<String>> highlights, String id, String field, String hlText) {
        if (hlText != null && hlText.contains("<em>")) {
            highlights.put(id + ":" + field, List.of(hlText));
        }
    }

    private void validateRequest(SearchRequest request) {
        String q = request.getQ();
        if ((q == null || q.isBlank())
                && (request.getSeverity() == null || request.getSeverity().isEmpty())
                && (request.getExploitability() == null || request.getExploitability().isEmpty())
                && (request.getProjectId() == null || request.getProjectId().isEmpty())
                && (request.getEngine() == null || request.getEngine().isEmpty())) {
            throw new IllegalArgumentException("SEARCH_QUERY_EMPTY: provide q or at least one filter");
        }
    }

    private void addInCondition(MapSqlParameterSource params, List<String> conditions,
                                String column, String paramName, List<String> values) {
        if (values != null && !values.isEmpty()) {
            params.addValue(paramName, values);
            conditions.add(column + " IN (:" + paramName + ")");
        }
    }

    private String buildDateRange(SearchRequest request, MapSqlParameterSource params) {
        var parts = new ArrayList<String>();
        if (request.getDiscoveredAtFrom() != null) {
            params.addValue("from", request.getDiscoveredAtFrom());
            parts.add("v.discovered_at >= :from::timestamp");
        }
        if (request.getDiscoveredAtTo() != null) {
            params.addValue("to", request.getDiscoveredAtTo());
            parts.add("v.discovered_at <= :to::timestamp");
        }
        return parts.isEmpty() ? null : String.join(" AND ", parts);
    }

    private String sanitizeQuery(String raw) {
        if (raw == null) return null;
        String sanitized = raw.replaceAll("[+\\-=&|><!(){}\\[\\]^\"~*?/:\\\\]", " ")
                .replaceAll("\\s+", " ").trim();
        if (sanitized.isEmpty()) return null;
        return sanitized.length() > 200 ? sanitized.substring(0, 200) : sanitized;
    }
}
