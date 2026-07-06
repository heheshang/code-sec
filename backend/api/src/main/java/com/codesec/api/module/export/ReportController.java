package com.codesec.api.module.export;

import com.codesec.domain.service.export.ReportGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportGenerationService reportGenerationService;

    private static final List<Map<String, Object>> TEMPLATES = List.of(
        template("monthly-summary", "Monthly Security Summary",
            "Aggregated vulnerability statistics, trend analysis, and remediation progress for the past month",
            "monthly"),
        template("weekly-scan-report", "Weekly Scan Report",
            "Detailed findings from all scans run in the past week, grouped by project and severity",
            "weekly"),
        template("vuln-inventory", "Vulnerability Inventory",
            "Complete inventory of all open vulnerabilities across all projects",
            "on-demand"),
        template("compliance-overview", "Compliance Overview",
            "Security compliance status mapped to industry standards (OWASP, CWE, PCI-DSS)",
            "on-demand")
    );

    private static Map<String, Object> template(String id, String name, String description, String frequency) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id);
        m.put("name", name);
        m.put("description", description);
        m.put("frequency", frequency);
        m.put("lastGeneratedAt", null);
        return m;
    }

    @GetMapping
    @PreAuthorize("@perm.check('report:read')")
    public Map<String, Object> list() {
        return Map.of("items", TEMPLATES);
    }

    @PostMapping("/{templateId}/generate")
    @PreAuthorize("@perm.check('report:read')")
    public ResponseEntity<Map<String, Object>> generate(@PathVariable String templateId) {
        boolean valid = TEMPLATES.stream().anyMatch(t -> t.get("id").equals(templateId));
        if (!valid) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Unknown template: " + templateId,
                "available", TEMPLATES.stream().map(t -> t.get("id")).toList()
            ));
        }

        Map<String, Object> result = reportGenerationService.generate(templateId);

        TEMPLATES.stream()
            .filter(t -> t.get("id").equals(templateId))
            .findFirst()
            .ifPresent(t -> t.put("lastGeneratedAt", LocalDateTime.now().toString()));

        return ResponseEntity.ok(Map.of(
            "success", true,
            "templateId", templateId,
            "message", templateId + " report generated successfully",
            "data", result
        ));
    }

    @GetMapping("/{templateId}/sample")
    @PreAuthorize("@perm.check('report:read')")
    public ResponseEntity<Map<String, Object>> sample(@PathVariable String templateId) {
        boolean valid = TEMPLATES.stream().anyMatch(t -> t.get("id").equals(templateId));
        if (!valid) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Unknown template: " + templateId,
                "available", TEMPLATES.stream().map(t -> t.get("id")).toList()
            ));
        }

        Map<String, Object> result = reportGenerationService.generate(templateId);

        return ResponseEntity.ok(Map.of(
            "templateId", templateId,
            "preview", true,
            "data", result
        ));
    }

    @GetMapping("/{templateId}/download")
    @PreAuthorize("@perm.check('report:read')")
    public ResponseEntity<byte[]> download(
            @PathVariable String templateId,
            @RequestParam(defaultValue = "json") String format) {
        boolean valid = TEMPLATES.stream().anyMatch(t -> t.get("id").equals(templateId));
        if (!valid) {
            return ResponseEntity.badRequest().body(("Unknown template: " + templateId).getBytes());
        }

        byte[] content = reportGenerationService.download(templateId, format);

        MediaType mediaType = switch (format) {
            case "html" -> MediaType.TEXT_HTML;
            case "csv" -> MediaType.parseMediaType("text/csv");
            case "pdf" -> MediaType.APPLICATION_PDF;
            default -> MediaType.APPLICATION_JSON;
        };

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=" + templateId + "-report." + format)
            .contentType(mediaType)
            .body(content);
    }
}
