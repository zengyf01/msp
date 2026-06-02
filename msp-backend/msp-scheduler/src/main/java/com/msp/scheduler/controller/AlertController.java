package com.msp.scheduler.controller;

import com.msp.common.core.ApiResponse;
import com.msp.scheduler.service.AlertService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 告警管理REST接口
 */
@RestController
@RequestMapping("/api/v1/msp/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    /**
     * 获取告警历史
     */
    @GetMapping
    public ApiResponse<List<AlertService.AlertRecord>> getAlertHistory(
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        List<AlertService.AlertRecord> alerts = alertService.getAlertHistory(limit);
        return ApiResponse.success(alerts);
    }

    /**
     * 获取特定告警详情
     */
    @GetMapping("/{alertId}")
    public ApiResponse<AlertService.AlertRecord> getAlert(@PathVariable(name = "alertId") String alertId) {
        List<AlertService.AlertRecord> history = alertService.getAlertHistory(100);
        AlertService.AlertRecord alert = history.stream()
                .filter(a -> a.getAlertId().equals(alertId))
                .findFirst()
                .orElse(null);
        if (alert == null) {
            return ApiResponse.error("404", "Alert not found");
        }
        return ApiResponse.success(alert);
    }

    /**
     * 标记告警为已解决
     */
    @PostMapping("/{alertId}/resolve")
    public ApiResponse<Boolean> resolveAlert(@PathVariable(name = "alertId") String alertId) {
        List<AlertService.AlertRecord> history = alertService.getAlertHistory(100);
        AlertService.AlertRecord alert = history.stream()
                .filter(a -> a.getAlertId().equals(alertId))
                .findFirst()
                .orElse(null);
        if (alert == null) {
            return ApiResponse.error("404", "Alert not found");
        }
        alert.setResolved(true);
        return ApiResponse.success(true);
    }

    /**
     * 清除已解决的告警
     */
    @DeleteMapping("/resolved")
    public ApiResponse<Boolean> clearResolvedAlerts() {
        alertService.clearResolvedAlerts();
        return ApiResponse.success(true);
    }

    /**
     * 获取告警统计
     */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getAlertStats() {
        List<AlertService.AlertRecord> history = alertService.getAlertHistory(1000);

        long total = history.size();
        long resolved = history.stream().filter(AlertService.AlertRecord::isResolved).count();
        long unresolved = total - resolved;
        long critical = history.stream()
                .filter(a -> a.getLevel() == AlertService.AlertLevel.CRITICAL && !a.isResolved())
                .count();
        long error = history.stream()
                .filter(a -> a.getLevel() == AlertService.AlertLevel.ERROR && !a.isResolved())
                .count();
        long warning = history.stream()
                .filter(a -> a.getLevel() == AlertService.AlertLevel.WARNING && !a.isResolved())
                .count();

        Map<String, Object> stats = Map.of(
                "total", total,
                "resolved", resolved,
                "unresolved", unresolved,
                "critical", critical,
                "error", error,
                "warning", warning
        );

        return ApiResponse.success(stats);
    }
}