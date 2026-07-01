package com.codesec.api.module.export;

import com.codesec.api.domain.entity.AuditRecordEntity;
import com.codesec.api.domain.entity.VulnFindingEntity;
import com.codesec.api.domain.entity.VulnTicketEntity;
import com.codesec.api.domain.repository.AuditRecordRepository;
import com.codesec.api.domain.repository.VulnFindingRepository;
import com.codesec.api.domain.repository.VulnTicketRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import java.awt.Color;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class PdfExportService {
    private final VulnFindingRepository vulnRepo;
    private final VulnTicketRepository ticketRepo;
    private final AuditRecordRepository auditRepo;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public byte[] exportSingle(Long ticketId) {
        var ticket = ticketRepo.findById(ticketId)
            .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));
        var vuln = vulnRepo.findById(ticket.getVulnId())
            .orElseThrow(() -> new RuntimeException("Vuln finding not found: " + ticket.getVulnId()));
        var auditRecords = auditRepo.findByVulnIdOrderByAuditedAtDesc(vuln.getId());

        return generatePdf(ticket, vuln, auditRecords);
    }

    public byte[] exportByVulnId(Long vulnId) {
        var vuln = vulnRepo.findById(vulnId)
            .orElseThrow(() -> new RuntimeException("Vuln finding not found: " + vulnId));
        var tickets = ticketRepo.findByVulnId(vulnId);
        if (tickets.isEmpty()) {
            throw new RuntimeException("No ticket found for vuln: " + vulnId);
        }
        var auditRecords = auditRepo.findByVulnIdOrderByAuditedAtDesc(vulnId);
        return generatePdf(tickets.get(0), vuln, auditRecords);
    }

    public byte[] exportBatch(List<Long> ticketIds) {
        try {
            var baos = new ByteArrayOutputStream();
            var zos = new ZipOutputStream(baos);

            for (Long ticketId : ticketIds) {
                try {
                    var ticket = ticketRepo.findById(ticketId)
                        .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));
                    var vuln = vulnRepo.findById(ticket.getVulnId()).orElse(null);
                    var auditRecords = vuln != null
                        ? auditRepo.findByVulnIdOrderByAuditedAtDesc(vuln.getId())
                        : List.<AuditRecordEntity>of();

                    byte[] pdf = generatePdf(ticket, vuln, auditRecords);
                    zos.putNextEntry(new ZipEntry("ticket-" + ticketId + ".pdf"));
                    zos.write(pdf);
                    zos.closeEntry();
                } catch (Exception e) {
                    log.warn("Failed to generate PDF for ticket {}: {}", ticketId, e.getMessage());
                }
            }

            zos.finish();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate batch export", e);
        }
    }

    private byte[] generatePdf(VulnTicketEntity ticket, VulnFindingEntity vuln, List<AuditRecordEntity> auditRecords) {
        var baos = new ByteArrayOutputStream();
        var document = new Document(PageSize.A4, 50, 50, 50, 50);

        try {
            var writer = PdfWriter.getInstance(document, baos);
            writer.setPageEvent(new PdfPageEventHelper() {
                @Override
                public void onEndPage(PdfWriter w, Document d) {
                    var cb = w.getDirectContent();
                    var font = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, Color.GRAY);
                    ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                        new Phrase("code-sec · Security Audit Report · Page " + w.getCurrentPageNumber(), font),
                        d.getPageSize().getRight(50), 20, 0);
                }
            });
            document.open();

            // Title
            var titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.DARK_GRAY);
            document.add(new Paragraph("Security Audit Report", titleFont));
            document.add(new Paragraph(" "));

            // Ticket info
            addSection(document, "Ticket Information");
            addField(document, "Ticket ID", String.valueOf(ticket.getId()));
            addField(document, "Status", ticket.getStatus());
            addField(document, "Severity", ticket.getSeverity());
            addField(document, "Project ID", String.valueOf(ticket.getProjectId()));
            if (ticket.getCreatedAt() != null) addField(document, "Created", ticket.getCreatedAt().format(FMT));
            if (ticket.getClosedAt() != null) addField(document, "Closed", ticket.getClosedAt().format(FMT));
            if (ticket.getDeadline() != null) addField(document, "Deadline", ticket.getDeadline().toString());

            document.add(new Paragraph(" "));

            if (vuln != null) {
                // Vulnerability details
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
                    var codeFont = FontFactory.getFont(FontFactory.COURIER, 9, Font.NORMAL, Color.BLACK);
                    document.add(new Paragraph(vuln.getCodeSnippet(), codeFont));
                }
            }

            // Audit records
            if (auditRecords != null && !auditRecords.isEmpty()) {
                document.add(new Paragraph(" "));
                addSection(document, "Audit History");
                for (var record : auditRecords) {
                    addField(document, "Action", record.getAction());
                    addField(document, "Auditor", String.valueOf(record.getAuditorId()));
                    addField(document, "Date", record.getAuditedAt() != null ? record.getAuditedAt().format(FMT) : "N/A");
                    if (record.getExploitCondition() != null && !record.getExploitCondition().isEmpty()) {
                        addField(document, "Exploit Condition", record.getExploitCondition());
                    }
                    if (record.getFixSuggestion() != null && !record.getFixSuggestion().isEmpty()) {
                        addField(document, "Fix Suggestion", record.getFixSuggestion());
                    }
                    document.add(new Paragraph(" "));
                }
            }

            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }

        return baos.toByteArray();
    }

    private void addSection(Document document, String title) throws DocumentException {
        var font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, Color.DARK_GRAY);
        var chunk = new Chunk(title, font);
        chunk.setUnderline(0.5f, -2f);
        document.add(new Paragraph(chunk));
    }

    private void addField(Document document, String label, String value) throws DocumentException {
        var labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.DARK_GRAY);
        var valueFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
        var p = new Paragraph();
        p.add(new Chunk(label + ": ", labelFont));
        p.add(new Chunk(value != null ? value : "N/A", valueFont));
        p.setSpacingBefore(2);
        p.setSpacingAfter(2);
        document.add(p);
    }
}
