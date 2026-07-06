package com.codesec.api.module.search.service;

import com.codesec.api.module.search.dto.SearchRequest;
import com.codesec.api.module.search.dto.SearchResponse;
import com.codesec.api.module.search.model.SnippetDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Service
public class SnippetSearchService {

    private static final Logger log = LoggerFactory.getLogger(SnippetSearchService.class);

    private final NamedParameterJdbcTemplate jdbc;

    public SnippetSearchService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public SearchResponse<SnippetDocument> search(SearchRequest request) {
        long startMs = System.currentTimeMillis();
        List<String> warnings = new ArrayList<>();

        validateRequest(request);

        int pageSize = Math.min(request.getPageSize(), 100);
        if (request.getPageSize() > 100) {
            warnings.add("page_size_clamped: 100");
        }

        var params = new MapSqlParameterSource();
        var conditions = new ArrayList<String>();

        String q = request.getQ();
        if (q != null && !q.isBlank()) {
            params.addValue("q", q);
            conditions.add("v.file_path LIKE :q || '%'");
        }

        if (request.getProjectId() != null && !request.getProjectId().isEmpty()) {
            params.addValue("projectId", request.getProjectId());
            conditions.add("v.project_id IN (:projectId)");
        }

        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);

        String countSql = "SELECT COUNT(*) FROM vuln_finding v" + where;
        long total = jdbc.queryForObject(countSql, params, Long.class);

        int offset = (request.getPage() - 1) * pageSize;
        params.addValue("limit", pageSize);
        params.addValue("offset", offset);

        String dataSql = "SELECT DISTINCT v.file_path, v.project_id, v.engine as language" +
                        " FROM vuln_finding v" + where +
                        " ORDER BY v.file_path" +
                        " LIMIT :limit OFFSET :offset";

        List<SnippetDocument> items = jdbc.query(dataSql, params, this::mapSnippetDocument);

        long tookMs = System.currentTimeMillis() - startMs;
        log.info("PG snippet search: q='{}', total={}, took={}ms", q, total, tookMs);

        return SearchResponse.<SnippetDocument>builder()
                .total(total)
                .page(request.getPage())
                .pageSize(pageSize)
                .tookMs(tookMs)
                .items(items)
                .build();
    }

    private SnippetDocument mapSnippetDocument(ResultSet rs, int rowNum) throws SQLException {
        SnippetDocument doc = new SnippetDocument();
        doc.setFilePath(rs.getString("file_path"));
        doc.setProjectId(rs.getString("project_id"));
        doc.setLanguage(rs.getString("language"));
        doc.setIndexedAt(java.time.LocalDateTime.now().toString());
        return doc;
    }

    private void validateRequest(SearchRequest request) {
        if ((request.getQ() == null || request.getQ().isBlank())
                && (request.getProjectId() == null || request.getProjectId().isEmpty())) {
            throw new IllegalArgumentException("SEARCH_QUERY_EMPTY: provide q or project_id filter");
        }
    }
}
