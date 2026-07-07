package com.codesec.domain.service.search;

import com.codesec.common.dto.SearchRequest;
import com.codesec.common.dto.SearchResponse;
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

        String q = request.getQ();
        boolean contentSearch = q != null && !q.isBlank() && q.contains(" ");

        List<String> conditions = new ArrayList<>();

        if (q != null && !q.isBlank()) {
            if (contentSearch) {
                conditions.add("v.tsv_code_snippet @@ plainto_tsquery('codesec_cfg', ?1)");
            } else {
                conditions.add("v.file_path LIKE ?1 || '%' ESCAPE '\\'");
            }
        }

        if (request.getProjectId() != null && !request.getProjectId().isEmpty()) {
            conditions.add("v.project_id IN (?2)");
        }

        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);

        String countSql = "SELECT COUNT(*) FROM vuln_finding v" + where;
        Query countQuery = entityManager.createNativeQuery(countSql);
        if (q != null && !q.isBlank()) {
            countQuery.setParameter(1, contentSearch ? q : escapeLike(q));
        }
        if (request.getProjectId() != null && !request.getProjectId().isEmpty()) {
            countQuery.setParameter(2, request.getProjectId());
        }
        long total = ((Number) countQuery.getSingleResult()).longValue();

        int offset = (request.getPage() - 1) * pageSize;

        String headlineExpr;
        if (contentSearch) {
            headlineExpr = ", ts_headline('codesec_cfg', v.code_snippet, plainto_tsquery('codesec_cfg', ?1), 'MaxWords=30,MinWords=15,StartSel=<em>,StopSel=</em>') as snippet_headline";
        } else {
            headlineExpr = ", NULL::text as snippet_headline";
        }

        String langCase = "CASE" +
            " WHEN v.file_path LIKE '%.java' THEN 'java'" +
            " WHEN v.file_path LIKE '%.kt' THEN 'kotlin'" +
            " WHEN v.file_path LIKE '%.py' THEN 'python'" +
            " WHEN v.file_path LIKE '%.js' THEN 'javascript'" +
            " WHEN v.file_path LIKE '%.ts' THEN 'typescript'" +
            " WHEN v.file_path LIKE '%.go' THEN 'go'" +
            " WHEN v.file_path LIKE '%.rs' THEN 'rust'" +
            " WHEN v.file_path LIKE '%.rb' THEN 'ruby'" +
            " WHEN v.file_path LIKE '%.php' THEN 'php'" +
            " WHEN v.file_path LIKE '%.c' OR v.file_path LIKE '%.h' THEN 'c'" +
            " WHEN v.file_path LIKE '%.cpp' OR v.file_path LIKE '%.hpp' THEN 'cpp'" +
            " WHEN v.file_path LIKE '%.cs' THEN 'csharp'" +
            " WHEN v.file_path LIKE '%.swift' THEN 'swift'" +
            " WHEN v.file_path LIKE '%.scala' THEN 'scala'" +
            " WHEN v.file_path LIKE '%.sql' THEN 'sql'" +
            " WHEN v.file_path LIKE '%.sh' THEN 'shell'" +
            " WHEN v.file_path LIKE '%.yaml' OR v.file_path LIKE '%.yml' THEN 'yaml'" +
            " WHEN v.file_path LIKE '%.xml' THEN 'xml'" +
            " WHEN v.file_path LIKE '%.json' THEN 'json'" +
            " WHEN v.file_path LIKE '%.html' OR v.file_path LIKE '%.htm' THEN 'html'" +
            " ELSE 'unknown' END";

        String dataSql = "SELECT DISTINCT v.file_path, v.project_id," +
                        langCase + " as language, v.line_start" +
                        headlineExpr +
                        ", to_char(v.discovered_at, 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"') as indexed_at" +
                        " FROM vuln_finding v" + where +
                        " ORDER BY v.file_path" +
                        " LIMIT ?3 OFFSET ?4";

        Query dataQuery = entityManager.createNativeQuery(dataSql);
        if (q != null && !q.isBlank()) {
            dataQuery.setParameter(1, contentSearch ? q : escapeLike(q));
        }
        if (request.getProjectId() != null && !request.getProjectId().isEmpty()) {
            dataQuery.setParameter(2, request.getProjectId());
        }
        dataQuery.setParameter(3, pageSize);
        dataQuery.setParameter(4, offset);

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
                .warnings(warnings.isEmpty() ? null : warnings)
                .build();
    }

    private SnippetDocument mapSnippetDocument(Object[] row) {
        int i = 0;
        SnippetDocument doc = new SnippetDocument();
        doc.setFilePath((String) row[i++]);
        doc.setProjectId((String) row[i++]);
        doc.setLanguage((String) row[i++]);
        doc.setLineStart(row[i] != null ? ((Number) row[i]).intValue() : null); i++;
        doc.setCodeSnippet((String) row[i++]);
        doc.setIndexedAt(row[i] != null ? row[i].toString() : java.time.LocalDateTime.now().toString());
        return doc;
    }

    private String escapeLike(String raw) {
        return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private void validateRequest(SearchRequest request) {
        if ((request.getQ() == null || request.getQ().isBlank())
                && (request.getProjectId() == null || request.getProjectId().isEmpty())) {
            throw new IllegalArgumentException("SEARCH_QUERY_EMPTY: provide q or project_id filter");
        }
    }
}
