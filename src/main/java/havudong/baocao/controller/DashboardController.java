package havudong.baocao.controller;

import havudong.baocao.dto.ApiResponse;
import havudong.baocao.dto.DashboardResponse;
import havudong.baocao.entity.User;
import havudong.baocao.service.DashboardService;
import havudong.baocao.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller cho Dashboard thống kê
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {
    
    private final DashboardService dashboardService;
    private final SecurityUtil securityUtil;
    
    /**
     * Lấy dashboard của seller hiện tại
     * GET /api/dashboard/seller
     */
    @GetMapping("/seller")
    public ResponseEntity<ApiResponse<DashboardResponse>> getSellerDashboard() {
        User seller = securityUtil.getCurrentUser();
        log.info("Fetching dashboard for seller: {}", seller.getId());
        
        DashboardResponse dashboard = dashboardService.getSellerDashboard(seller);
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }
    
    /**
     * Lấy dashboard tổng quan platform (Admin only)
     * GET /api/dashboard/admin
     */
    @GetMapping("/admin")
    public ResponseEntity<ApiResponse<DashboardResponse>> getAdminDashboard() {
        log.info("Fetching admin dashboard");
        
        DashboardResponse dashboard = dashboardService.getAdminDashboard();
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }
}
