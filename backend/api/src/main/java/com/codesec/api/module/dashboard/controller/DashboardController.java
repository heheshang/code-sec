package com.codesec.api.module.dashboard.controller;

import com.codesec.api.module.dashboard.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return dashboardService.stats();
    }

    @GetMapping("/trend")
    public Map<String, Object> trend(@RequestParam(name = "days", defaultValue = "14") int days) {
        return dashboardService.trend(days);
    }
}
