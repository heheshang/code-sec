package com.codesec.domain.service.search;

import com.codesec.common.dto.SearchRequest;
import com.codesec.common.dto.SearchResponse;
import com.codesec.domain.model.VulnDocument;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class VulnSearchService {

    private static final Logger log = LoggerFactory.getLogger(VulnSearchService.class);

    private static final Set<String> ALLOWED_SORT_BYS = Set.of("_score", "discovered_at");
    private static final Set<String> ALLOWED_SORT_ORDERS = Set.of("asc", "desc");

    @PersistenceContext
    private EntityManager entityManager;

    public SearchResponse<VulnDocument> search(SearchRequest request) {
        long startMs = System.currentTimeMillis();
        List<String> warnings = new ArrayList<>();
        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        validateRequest(request);

        int pageSize = Math.min(request.getPageSize(), 100);
        if (request.getPageSize() > 100) {
            warnings.add("page_size_clamped: 100");
        }

        String q = sanitizeQuery(request.getQ());
        boolean hasQuery = q != null && !q.isBlank();
        int qPos = -1;
        int paramIdx = 1;

        if (hasQuery) {
            qPos = paramIdx++;
            params.add(q);
            conditions.add("(v.tsv_title_desc @@ plainto_tsquery('codesec_cfg', ?" + qPos + ")" +
                         " OR v.tsv_code_snippet @@ plainto_tsquery('codesec_cfg', ?" + qPos + "))");
        }

        paramIdx = expandInParams(params, conditions, "v.severity", request.getSeverity(), paramIdx);
        paramIdx = expandInParams(params, conditions, "v.exploitability", request.getExploitability(), paramIdx);
        paramIdx = expandInParams(params, conditions, "v.project_id", request.getProjectId(), paramIdx);
        paramIdx = expandInParams(params, conditions, "v.engine", request.getEngine(), paramIdx);

        if (request.getDiscoveredAtFrom() != null || request.getDiscoveredAtTo() != null) {
            paramIdx = buildDateRange(request, params, conditions, paramIdx);
        }

        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);

        String countSql = "SELECT COUNT(*) FROM vuln_finding v" + where;
        Query countQuery = entityManager.createNativeQuery(countSql);
        for (int i = 0; i < params.size(); i++) {
            countQuery.setParameter(i + 1, params.get(i));
        }
        long total = ((Number) countQuery.getSingleResult()).longValue();

        String sortBy = request.getSortBy();
        String sortOrder = request.getSortOrder();
        if (!ALLOWED_SORT_BYS.contains(sortBy)) {
            sortBy = "_score";
        }
        if (!ALLOWED_SORT_ORDERS.contains(sortOrder)) {
            sortOrder = "desc";
        }
        String orderBy;
        if ("discovered_at".equals(sortBy)) {
            orderBy = "v.discovered_at " + ("asc".equals(sortOrder) ? "ASC" : "DESC");
        } else if (hasQuery) {
            orderBy = "ts_rank(v.tsv_title_desc, plainto_tsquery('codesec_cfg', ?" + qPos + ")) DESC, v.discovered_at DESC";
        } else {
            orderBy = "v.discovered_at DESC";
        }

        String hlExpr = hasQuery
            ? ", COALESCE(ts_headline('codesec_cfg', v.title, plainto_tsquery('codesec_cfg', ?" + qPos + "), 'StartSel=<em>, StopSel=</em>, MaxWords=50, MinWords=20'), v.title) as hl_title" +
              ", COALESCE(ts_headline('codesec_cfg', v.description, plainto_tsquery('codesec_cfg', ?" + qPos + "), 'StartSel=<em>, StopSel=</em>, MaxWords=80, MinWords=30'), v.description) as hl_desc" +
              ", COALESCE(ts_headline('codesec_cfg', v.code_snippet, plainto_tsquery('codesec_cfg', ?" + qPos + "), 'StartSel=<em>, StopSel=</em>, MaxWords=100, MinWords=40'), v.code_snippet) as hl_snippet"
            : ", v.title as hl_title, v.description as hl_desc, v.code_snippet as hl_snippet";

        int offset = (request.getPage() - 1) * pageSize;
        int limitPos = paramIdx++;
        params.add(pageSize);
        int offsetPos = paramIdx++;
        params.add(offset);

        String dataSql = "SELECT v.id, v.project_id, v.rule_id, v.severity, v.exploitability," +
                        " v.title, v.description, v.code_snippet, v.file_path, v.cwe," +
                        " v.engine, to_char(v.discovered_at, 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"') as discovered_at," +
                        " v.discovered_by" + hlExpr +
                        " FROM vuln_finding v" + where +
                        " ORDER BY " + orderBy +
                        " LIMIT ?" + limitPos + " OFFSET ?" + offsetPos;

        Map<String, List<String>> highlights = new LinkedHashMap<>();
        Query dataQuery = entityManager.createNativeQuery(dataSql);
        for (int i = 0; i < params.size(); i++) {
            dataQuery.setParameter(i + 1, params.get(i));
        }
        List<Object[]> rows = dataQuery.getResultList();
        List<VulnDocument> items = new ArrayList<>();
        for (Object[] row : rows) {
            items.add(mapVulnDocument(row, highlights));
        }

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

    private VulnDocument mapVulnDocument(Object[] row, Map<String, List<String>> highlights) {
        VulnDocument doc = new VulnDocument();
        String id = (String) row[0];
        doc.setId(id);
        doc.setProjectId((String) row[1]);
        doc.setRuleId((String) row[2]);
        doc.setSeverity((String) row[3]);
        doc.setExploitability((String) row[4]);
        doc.setTitle((String) row[5]);
        doc.setDescription((String) row[6]);
        doc.setCodeSnippet((String) row[7]);
        doc.setFilePath((String) row[8]);
        doc.setCwe((String) row[9]);
        doc.setEngine((String) row[10]);
        doc.setDiscoveredAt((String) row[11]);
        doc.setDiscoveredBy((String) row[12]);

        addHighlight(highlights, id, "title", (String) row[13]);
        addHighlight(highlights, id, "description", (String) row[14]);
        addHighlight(highlights, id, "code_snippet", (String) row[15]);

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

    private int expandInParams(List<Object> params, List<String> conditions,
                                String column, List<String> values, int pos) {
        if (values != null && !values.isEmpty()) {
            List<String> placeholders = new ArrayList<>();
            for (String v : values) {
                params.add(v);
                placeholders.add("?" + pos);
                pos++;
            }
            conditions.add(column + " IN (" + String.join(", ", placeholders) + ")");
            return pos;
        }
        return pos;
    }

    private int buildDateRange(SearchRequest request, List<Object> params, List<String> conditions, int pos) {
        if (request.getDiscoveredAtFrom() != null) {
            params.add(request.getDiscoveredAtFrom());
            conditions.add("v.discovered_at >= ?" + pos + "::timestamp");
            pos++;
        }
        if (request.getDiscoveredAtTo() != null) {
            params.add(request.getDiscoveredAtTo());
            conditions.add("v.discovered_at <= ?" + pos + "::timestamp");
            pos++;
        }
        return pos;
    }

    private String sanitizeQuery(String raw) {
        if (raw == null) return null;
        String sanitized = raw.replaceAll("[+=&|><!(){}\\[\\]^\"~*?/:\\\\]", " ")
                .replaceAll("\\s+", " ").trim();
        if (sanitized.isEmpty()) return null;
        return sanitized.length() > 200 ? sanitized.substring(0, 200) : sanitized;
    }
}
