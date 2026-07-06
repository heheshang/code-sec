package com.codesec.domain.service.dashboard;

import com.codesec.domain.repository.RepoRepository;
import com.codesec.domain.repository.VulnFindingRepository;
import com.codesec.domain.repository.VulnTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final VulnFindingRepository vulnRepo;
    private final VulnTicketRepository ticketRepo;
    private final RepoRepository repoRepo;

    /** Full dashboard stats — all real aggregation queries. */
    public Map<String, Object> stats() {
        long start = System.currentTimeMillis();

        // Single GROUP BY query replaces 6+1 independent COUNT queries
        java.util.Map<String, Long> severityCounts = vulnRepo.countBySeverityGrouped()
            .stream()
            .collect(java.util.stream.Collectors.toMap(
                row -> (String) row[0],
                row -> (Long) row[1]
            ));
        long total = severityCounts.values().stream().mapToLong(Long::longValue).sum();
        long critical = severityCounts.getOrDefault("critical", 0L);
        long high = severityCounts.getOrDefault("high", 0L);
        long medium = severityCounts.getOrDefault("medium", 0L);
        long low = severityCounts.getOrDefault("low", 0L);
        long info = severityCounts.getOrDefault("info", 0L);

        long open = ticketRepo.countByStatus("pending_audit")
                  + ticketRepo.countByStatus("confirmed")
                  + ticketRepo.countByStatus("pending_fix");

        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        long fixedThisWeek = ticketRepo.countClosedSince(weekAgo);

        // fixRate = closed / (closed + open + rejected) over past 30 days
        LocalDateTime monthAgo = LocalDateTime.now().minusDays(30);
        long closed30d = ticketRepo.countClosedSince(monthAgo);
        long open30d = 0;
        for (String s : List.of("pending_audit", "confirmed", "pending_fix")) {
            open30d += ticketRepo.countByStatus(s);
        }
        long rejected30d = ticketRepo.countByStatus("false_positive")
                         + ticketRepo.countByStatus("waived");
        long denominator = closed30d + open30d + rejected30d;
        double fixRate = denominator > 0 ? (double) closed30d / denominator : 0.0;

        long projectCount = repoRepo.count();

        long tookMs = System.currentTimeMillis() - start;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("critical", critical);
        result.put("high", high);
        result.put("medium", medium);
        result.put("low", low);
        result.put("info", info);
        result.put("open", open);
        result.put("fixedThisWeek", fixedThisWeek);
        result.put("fixRate", Math.round(fixRate * 100.0) / 100.0);
        result.put("severityCounts", Map.of(
            "critical", critical,
            "high", high,
            "medium", medium,
            "low", low,
            "info", info
        ));
        result.put("projectCount", projectCount);
        result.put("tookMs", tookMs);
        return result;
    }

    /** 14-day trend data — opened and closed per day. */
    public Map<String, Object> trend(int days) {
        long start = System.currentTimeMillis();

        LocalDate today = LocalDate.now();
        List<Map<String, Object>> trend = new ArrayList<>();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            LocalDateTime from = day.atStartOfDay();
            LocalDateTime to = day.plusDays(1).atStartOfDay();

            long opened = ticketRepo.countCreatedBetween(from, to);
            long closed = ticketRepo.countClosedBetween(from, to);

            trend.add(Map.of(
                "date", day.toString(),
                "opened", opened,
                "closed", closed
            ));
        }

        long tookMs = System.currentTimeMillis() - start;

        return Map.of(
            "trend", trend,
            "tookMs", tookMs
        );
    }
}
