package com.codesec.search.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Maps MySQL vuln_finding rows to ES VulnDocument.
 * Explicitly SELECTs all 13 ES-indexed fields from vuln_finding table.
 * Fields NOT mapped to ES: line_start, line_end, scan_task_id, dedup_key, created_at, updated_at.
 *
 * In production, this would use MapStruct or manual JDBC mapping.
 * This standalone mapper serves as the canonical field map and can be validated at startup.
 */
@Component
public class VulnFindingEsMapper {

    private static final Logger log = LoggerFactory.getLogger(VulnFindingEsMapper.class);

    /**
     * The canonical 13-field SELECT list for ES indexing.
     * Must match vuln.json mapping exactly.
     */
    public static final String SELECT_13_FIELDS =
            "SELECT id, project_id, rule_id, severity, exploitability, " +
            "title, description, code_snippet, file_path, cwe, engine, " +
            "discovered_at, discovered_by FROM vuln_finding";

    /**
     * The expected 13 column names in order.
     * Used for startup validation to catch schema drift.
     */
    public static final List<String> EXPECTED_COLUMNS = List.of(
            "id", "project_id", "rule_id", "severity", "exploitability",
            "title", "description", "code_snippet", "file_path", "cwe", "engine",
            "discovered_at", "discovered_by"
    );

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_INSTANT;

    /**
     * Map a single ResultSet row to VulnDocument.
     * Validates that all 13 columns are present (throws if missing).
     */
    public VulnDocument mapRow(ResultSet rs) throws SQLException {
        VulnDocument doc = new VulnDocument();
        doc.setId(String.valueOf(rs.getLong("id")));
        doc.setProjectId(String.valueOf(rs.getLong("project_id")));
        doc.setRuleId(rs.getString("rule_id"));
        doc.setSeverity(rs.getString("severity"));
        doc.setExploitability(rs.getString("exploitability"));
        doc.setTitle(rs.getString("title"));
        doc.setDescription(rs.getString("description"));
        doc.setCodeSnippet(rs.getString("code_snippet"));
        doc.setFilePath(rs.getString("file_path"));
        doc.setCwe(rs.getString("cwe"));
        doc.setEngine(rs.getString("engine"));

        var ts = rs.getTimestamp("discovered_at");
        doc.setDiscoveredAt(ts != null ? DATE_FMT.format(ts.toInstant()) : null);

        doc.setDiscoveredBy(rs.getString("discovered_by"));
        return doc;
    }

    /**
     * Validate that all 13 expected columns are present in the ResultSet metadata.
     * Throws IllegalStateException if any column is missing — this is a startup-time check.
     */
    public void validateColumns(ResultSet rs) throws SQLException {
        var meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();
        List<String> actualColumns = new ArrayList<>();
        for (int i = 1; i <= columnCount; i++) {
            actualColumns.add(meta.getColumnName(i).toLowerCase());
        }

        List<String> missing = new ArrayList<>();
        for (String expected : EXPECTED_COLUMNS) {
            if (!actualColumns.contains(expected)) {
                missing.add(expected);
            }
        }

        if (!missing.isEmpty()) {
            String msg = "VulnFindingEsMapper: missing columns in vuln_finding table: " + missing +
                    ". Expected 13 columns: " + EXPECTED_COLUMNS;
            log.error(msg);
            throw new IllegalStateException(msg);
        }

        log.info("VulnFindingEsMapper validated: all 13 columns present. Actual columns: {}", actualColumns);
    }

    /**
     * Map a list of rows to VulnDocument list.
     */
    public List<VulnDocument> mapRows(ResultSet rs) throws SQLException {
        List<VulnDocument> docs = new ArrayList<>();
        while (rs.next()) {
            docs.add(mapRow(rs));
        }
        return docs;
    }
}
