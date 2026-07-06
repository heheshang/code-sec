package com.codesec.domain.service.export;

import com.codesec.domain.entity.AuditRecordEntity;
import com.codesec.domain.entity.VulnFindingEntity;
import com.codesec.domain.entity.VulnTicketEntity;
import com.lowagie.text.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * PdfReportRenderer — renders structured report content into an iText/OpenPDF Document.
 *
 * Separated from PdfExportService (SRP) so that:
 * - All rendering logic is testable in isolation
 * - PdfExportService focuses on data retrieval and orchestration
 * - New section types (e.g. charts, attachments) can be added without touching service
 */
public final class PdfReportRenderer {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private PdfReportRenderer() {}

    // ======================== Section / Field helpers ========================

    /** Renders a section header with underline. */
    public static void addSection(Document document, String title) throws DocumentException {
        Chunk chunk = new Chunk(title, PdfReportTemplate.SECTION_FONT);
        chunk.setUnderline(0.5f, -2f);
        document.add(new Paragraph(chunk));
    }

    /** Renders a label: value pair. */
    public static void addField(Document document, String label, String value) throws DocumentException {
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + ": ", PdfReportTemplate.LABEL_FONT));
        p.add(new Chunk(value != null ? value : "N/A", PdfReportTemplate.VALUE_FONT));
        p.setSpacingBefore(2);
        p.setSpacingAfter(2);
        document.add(p);
    }

    // ======================== Ticket Information ========================

    /** Renders the ticket info section. */
    public static void renderTicketInfo(Document document, VulnTicketEntity ticket) throws DocumentException {
        addSection(document, "Ticket Information");
        addField(document, "Ticket ID", String.valueOf(ticket.getId()));
        addField(document, "Status", ticket.getStatus());
        addField(document, "Severity", ticket.getSeverity());
        addField(document, "Project ID", String.valueOf(ticket.getProjectId()));
        if (ticket.getCreatedAt() != null)
            addField(document, "Created", ticket.getCreatedAt().format(FMT));
        if (ticket.getClosedAt() != null)
            addField(document, "Closed", ticket.getClosedAt().format(FMT));
        if (ticket.getDeadline() != null)
            addField(document, "Deadline", ticket.getDeadline().toString());
    }

    // ======================== Vulnerability Details ========================

    /** Renders vulnerability details (if vuln is available). */
    public static void renderVulnDetails(Document document, VulnFindingEntity vuln) throws DocumentException {
        if (vuln == null) return;

        document.add(new Paragraph(" "));
        addSection(document, "Vulnerability Details");
        addField(document, "Rule ID", vuln.getRuleId());
        addField(document, "Title", vuln.getTitle());
        addField(document, "Severity", vuln.getSeverity());
        addField(document, "CWE", vuln.getCwe());
        addField(document, "Exploitability", vuln.getExploitability());
        addField(document, "File", vuln.getFilePath());
        addField(document, "Lines", vuln.getLineStart() + " - " + vuln.getLineEnd());
        addField(document, "Engine", vuln.getEngine());

        if (vuln.getDescription() != null && !vuln.getDescription().isEmpty()) {
            document.add(new Paragraph(" "));
            addSection(document, "Description");
            document.add(new Paragraph(vuln.getDescription()));
        }

        if (vuln.getCodeSnippet() != null && !vuln.getCodeSnippet().isEmpty()) {
            document.add(new Paragraph(" "));
            addSection(document, "Code Snippet");
            document.add(new Paragraph(vuln.getCodeSnippet(), PdfReportTemplate.CODE_FONT));
        }
    }

    // ======================== Audit History ========================

    /** Renders audit history records. */
    public static void renderAuditHistory(Document document, List<AuditRecordEntity> records)
            throws DocumentException {
        if (records == null || records.isEmpty()) return;

        document.add(new Paragraph(" "));
        addSection(document, "Audit History");
        for (AuditRecordEntity record : records) {
            addField(document, "Action", record.getAction());
            addField(document, "Auditor", String.valueOf(record.getAuditorId()));
            addField(document, "Date",
                record.getAuditedAt() != null ? record.getAuditedAt().format(FMT) : "N/A");
            if (record.getExploitCondition() != null && !record.getExploitCondition().isEmpty())
                addField(document, "Exploit Condition", record.getExploitCondition());
            if (record.getFixSuggestion() != null && !record.getFixSuggestion().isEmpty())
                addField(document, "Fix Suggestion", record.getFixSuggestion());
            document.add(new Paragraph(" "));
        }
    }
}
