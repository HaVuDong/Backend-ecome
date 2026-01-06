package havudong.baocao.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO cho dashboard thống kê seller
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    
    // Tổng quan
    private BigDecimal totalRevenue;           // Tổng doanh thu (sau khi trừ phí platform)
    private BigDecimal totalPlatformFee;       // Tổng phí platform đã trả
    private Long totalOrders;                  // Tổng số đơn hàng
    private Long totalProducts;                // Tổng số sản phẩm
    private Long totalReviews;                 // Tổng số đánh giá nhận được
    private Double averageRating;              // Điểm đánh giá trung bình
    
    // Thống kê đơn hàng theo trạng thái
    private OrderStats orderStats;
    
    // Top sản phẩm bán chạy
    private List<TopProduct> topProducts;
    
    // Doanh thu theo thời gian (7 ngày gần nhất)
    private List<RevenueByDate> revenueByDate;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderStats {
        private Long pendingOrders;      // Chờ xử lý
        private Long processingOrders;   // Đang xử lý
        private Long shippingOrders;     // Đang giao
        private Long deliveredOrders;    // Đã giao
        private Long cancelledOrders;    // Đã hủy
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopProduct {
        private Long productId;
        private String productName;
        private String productImage;
        private Long soldCount;
        private BigDecimal revenue;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueByDate {
        private String date;
        private BigDecimal revenue;
        private Long orderCount;
    }
}
