package com.codesec.domain.service.export;

import com.codesec.domain.entity.AuditRecordEntity;
import com.codesec.domain.entity.VulnFindingEntity;
import com.codesec.domain.entity.VulnTicketEntity;
import com.codesec.domain.repository.AuditRecordRepository;
import com.codesec.domain.repository.VulnFindingRepository;
import com.codesec.domain.repository.VulnTicketRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * PdfExportService — orchestrates PDF report generation.
 *
 * Responsibility: fetch data from repositories, delegate rendering to
 * PdfReportRenderer, handle document lifecycle via PdfReportTemplate.
 * No font/color/page-event logic — those live in PdfReportTemplate.
 * No content-rendering logic — that lives in PdfReportRenderer.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PdfExportService {

    private final VulnFindingRepository vulnRepo;
    private final VulnTicketRepository ticketRepo;
    private final AuditRecordRepository auditRepo;

    // ======================== Public API ========================

    public byte[] exportSingle(Long ticketId) {
        VulnTicketEntity ticket = ticketRepo.findById(ticketId)
            .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));
        VulnFindingEntity vuln = vulnRepo.findById(ticket.getVulnId())
            .orElseThrow(() -> new RuntimeException("Vuln finding not found: " + ticket.getVulnId()));
        List<AuditRecordEntity> audits = auditRepo.findByVulnIdOrderByAuditedAtDesc(vuln.getId());
        return generatePdf(ticket, vuln, audits);
    }

    public byte[] exportByVulnId(Long vulnId) {
        VulnFindingEntity vuln = vulnRepo.findById(vulnId)
            .orElseThrow(() -> new RuntimeException("Vuln finding not found: " + vulnId));
        List<VulnTicketEntity> tickets = ticketRepo.findByVulnId(vulnId);
        if (tickets.isEmpty()) {
            throw new RuntimeException("No ticket found for vuln: " + vulnId);
        }
        List<AuditRecordEntity> audits = auditRepo.findByVulnIdOrderByAuditedAtDesc(vulnId);
        return generatePdf(tickets.get(0), vuln, audits);
    }

    public byte[] exportBatch(List<Long> ticketIds) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(baos);

            for (Long ticketId : ticketIds) {
                try {
                    VulnTicketEntity ticket = ticketRepo.findById(ticketId)
                        .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));
                    VulnFindingEntity vuln = vulnRepo.findById(ticket.getVulnId()).orElse(null);
                    List<AuditRecordEntity> audits = vuln != null
                        ? auditRepo.findByVulnIdOrderByAuditedAtDesc(vuln.getId())
                        : List.of();

                    byte[] pdf = generatePdf(ticket, vuln, audits);
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

    // ======================== PDF Generation (delegates to Template + Renderer) ========================

    private byte[] generatePdf(VulnTicketEntity ticket, VulnFindingEntity vuln,
                               List<AuditRecordEntity> auditRecords) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = PdfReportTemplate.createDocument();

        try {
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            writer.setPageEvent(PdfReportTemplate.createPageEvent());
            document.open();

            // Title
            document.add(new Paragraph("Security Audit Report", PdfReportTemplate.TITLE_FONT));
            document.add(new Paragraph(" "));

            // Delegated rendering
            PdfReportRenderer.renderTicketInfo(document, ticket);
            PdfReportRenderer.renderVulnDetails(document, vuln);
            PdfReportRenderer.renderAuditHistory(document, auditRecords);

            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }

        return baos.toByteArray();
    }
}
