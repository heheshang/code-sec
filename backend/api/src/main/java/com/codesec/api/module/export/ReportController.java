package com.codesec.api.module.export;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

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
}
