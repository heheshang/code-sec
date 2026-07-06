package com.codesec.domain.service.search;

import com.codesec.domain.dto.SearchRequest;
import com.codesec.domain.dto.SearchResponse;
import com.codesec.domain.model.SnippetDocument;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SnippetSearchService {

    private static final Logger log = LoggerFactory.getLogger(SnippetSearchService.class);

    @PersistenceContext
    private EntityManager entityManager;

    public SearchResponse<SnippetDocument> search(SearchRequest request) {
        long startMs = System.currentTimeMillis();
        List<String> warnings = new ArrayList<>();

        validateRequest(request);

        int pageSize = Math.min(request.getPageSize(), 100);
        if (request.getPageSize() > 100) {
            warnings.add("page_size_clamped: 100");
        }

        List<Object> params = new ArrayList<>();
        List<String> conditions = new ArrayList<>();
        int paramIdx = 1;

        String q = request.getQ();
        if (q != null && !q.isBlank()) {
            int qPos = paramIdx++;
            params.add(q);
            conditions.add("v.file_path LIKE ?" + qPos + " || '%'");
        }

        if (request.getProjectId() != null && !request.getProjectId().isEmpty()) {
            int pidPos = paramIdx++;
            params.add(request.getProjectId());
            conditions.add("v.project_id IN (?" + pidPos + ")");
        }

        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);

        String countSql = "SELECT COUNT(*) FROM vuln_finding v" + where;
        Query countQuery = entityManager.createNativeQuery(countSql);
        for (int i = 0; i < params.size(); i++) {
            countQuery.setParameter(i + 1, params.get(i));
        }
        long total = ((Number) countQuery.getSingleResult()).longValue();

        int offset = (request.getPage() - 1) * pageSize;
        int limitPos = paramIdx++;
        params.add(pageSize);
        int offsetPos = paramIdx++;
        params.add(offset);

        String dataSql = "SELECT DISTINCT v.file_path, v.project_id, v.engine as language" +
                        " FROM vuln_finding v" + where +
                        " ORDER BY v.file_path" +
                        " LIMIT ?" + limitPos + " OFFSET ?" + offsetPos;

        Query dataQuery = entityManager.createNativeQuery(dataSql);
        for (int i = 0; i < params.size(); i++) {
            dataQuery.setParameter(i + 1, params.get(i));
        }
        List<Object[]> rows = dataQuery.getResultList();
        List<SnippetDocument> items = new ArrayList<>();
        for (Object[] row : rows) {
            items.add(mapSnippetDocument(row));
        }

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

    private SnippetDocument mapSnippetDocument(Object[] row) {
        SnippetDocument doc = new SnippetDocument();
        doc.setFilePath((String) row[0]);
        doc.setProjectId((String) row[1]);
        doc.setLanguage((String) row[2]);
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
