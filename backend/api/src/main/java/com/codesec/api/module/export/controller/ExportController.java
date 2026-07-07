package com.codesec.api.module.export.controller;

import com.codesec.domain.service.export.PdfExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ExportController {
    private final PdfExportService pdfExportService;

    @GetMapping("/api/v1/tickets/{id}/export")
    @PreAuthorize("@perm.check('ticket:read')")
    public ResponseEntity<byte[]> exportSingle(@PathVariable Long id) {
        byte[] pdf = pdfExportService.exportSingle(id);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ticket-" + id + ".pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }

    @GetMapping("/api/v1/vulns/{vulnId}/export")
    @PreAuthorize("@perm.check('ticket:read')")
    public ResponseEntity<byte[]> exportByVuln(@PathVariable Long vulnId) {
        byte[] pdf = pdfExportService.exportByVulnId(vulnId);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=vuln-" + vulnId + ".pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }

    @PostMapping("/api/v1/tickets/export-batch")
    @PreAuthorize("@perm.check('ticket:read')")
    public ResponseEntity<byte[]> exportBatch(@RequestBody List<Long> ticketIds) {
        byte[] zip = pdfExportService.exportBatch(ticketIds);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tickets-export.zip")
            .contentType(MediaType.parseMediaType("application/zip"))
            .body(zip);
    }
}
