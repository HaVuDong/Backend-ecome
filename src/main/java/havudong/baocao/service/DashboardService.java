package havudong.baocao.service;

import havudong.baocao.dto.DashboardResponse;
import havudong.baocao.dto.DashboardResponse.OrderStats;
import havudong.baocao.dto.DashboardResponse.RevenueByDate;
import havudong.baocao.dto.DashboardResponse.TopProduct;
import havudong.baocao.entity.User;
import havudong.baocao.entity.enums.ShippingStatus;
import havudong.baocao.repository.OrderRepository;
import havudong.baocao.repository.ProductRepository;
import havudong.baocao.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service cho Dashboard thống kê
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {
    
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    
    /**
     * Lấy thống kê dashboard cho seller
     */
    @Transactional(readOnly = true)
    public DashboardResponse getSellerDashboard(User seller) {
        log.info("Generating dashboard for seller: {}", seller.getId());
        
        // Thống kê cơ bản
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalPlatformFee = BigDecimal.ZERO;
        Long totalOrders = 0L;
        Long totalProducts = 0L;
        Long totalReviews = 0L;
        Double avgRating = 0.0;
        OrderStats orderStats = OrderStats.builder()
                .pendingOrders(0L).processingOrders(0L).shippingOrders(0L)
                .deliveredOrders(0L).cancelledOrders(0L).build();
        List<TopProduct> topProducts = new ArrayList<>();
        List<RevenueByDate> revenueByDate = new ArrayList<>();
        
        try {
            totalRevenue = orderRepository.calculateSellerRevenue(seller);
            if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("Error calculating seller revenue: {}", e.getMessage());
        }
        
        try {
            totalPlatformFee = calculateTotalPlatformFee(seller);
        } catch (Exception e) {
            log.warn("Error calculating platform fee: {}", e.getMessage());
        }
        
        try {
            totalOrders = orderRepository.countBySeller(seller);
        } catch (Exception e) {
            log.warn("Error counting orders: {}", e.getMessage());
        }
        
        try {
            totalProducts = productRepository.countBySeller(seller);
        } catch (Exception e) {
            log.warn("Error counting products: {}", e.getMessage());
        }
        
        // Thống kê đánh giá từ sản phẩm của seller
        try {
            List<Object[]> reviewStats = reviewRepository.getSellerReviewStats(seller.getId());
            if (!reviewStats.isEmpty() && reviewStats.get(0)[0] != null) {
                totalReviews = ((Number) reviewStats.get(0)[0]).longValue();
                avgRating = ((Number) reviewStats.get(0)[1]).doubleValue();
            }
        } catch (Exception e) {
            log.warn("Error getting review stats: {}", e.getMessage());
        }
        
        // Thống kê đơn hàng theo trạng thái
        try {
            orderStats = getOrderStats(seller);
        } catch (Exception e) {
            log.warn("Error getting order stats: {}", e.getMessage());
        }
        
        // Top sản phẩm bán chạy
        try {
            topProducts = getTopSellingProducts(seller);
        } catch (Exception e) {
            log.warn("Error getting top products: {}", e.getMessage());
        }
        
        // Doanh thu 7 ngày gần nhất
        try {
            revenueByDate = getRevenueByDate(seller, 7);
        } catch (Exception e) {
            log.warn("Error getting revenue by date: {}", e.getMessage());
        }
        
        return DashboardResponse.builder()
                .totalRevenue(totalRevenue)
                .totalPlatformFee(totalPlatformFee)
                .totalOrders(totalOrders)
                .totalProducts(totalProducts)
                .totalReviews(totalReviews)
                .averageRating(avgRating)
                .orderStats(orderStats)
                .topProducts(topProducts)
                .revenueByDate(revenueByDate)
                .build();
    }
    
    /**
     * Tính tổng phí platform của seller
     */
    private BigDecimal calculateTotalPlatformFee(User seller) {
        BigDecimal total = orderRepository.calculateTotalPlatformFee(seller);
        return total != null ? total : BigDecimal.ZERO;
    }
    
    /**
     * Thống kê đơn hàng theo trạng thái
     */
    private OrderStats getOrderStats(User seller) {
        return OrderStats.builder()
                .pendingOrders(orderRepository.countBySellerAndShippingStatus(seller, ShippingStatus.PENDING))
                .processingOrders(orderRepository.countBySellerAndShippingStatus(seller, ShippingStatus.PROCESSING))
                .shippingOrders(orderRepository.countBySellerAndShippingStatus(seller, ShippingStatus.SHIPPED))
                .deliveredOrders(orderRepository.countBySellerAndShippingStatus(seller, ShippingStatus.DELIVERED))
                .cancelledOrders(orderRepository.countBySellerAndShippingStatus(seller, ShippingStatus.CANCELLED))
                .build();
    }
    
    /**
     * Top sản phẩm bán chạy của seller
     */
    private List<TopProduct> getTopSellingProducts(User seller) {
        List<Object[]> results = orderRepository.getTopSellingProductsBySeller(seller.getId(), PageRequest.of(0, 5));
        return results.stream()
                .map(row -> TopProduct.builder()
                        .productId(((Number) row[0]).longValue())
                        .productName((String) row[1])
                        .productImage((String) row[2])
                        .soldCount(((Number) row[3]).longValue())
                        .revenue((BigDecimal) row[4])
                        .build())
                .collect(Collectors.toList());
    }
    
    /**
     * Doanh thu theo ngày
     */
    private List<RevenueByDate> getRevenueByDate(User seller, int days) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(days);
        
        List<Object[]> results = orderRepository.getRevenueByDateForSeller(seller.getId(), startDate, endDate);
        
        // Map kết quả vào response
        return results.stream()
                .map(row -> RevenueByDate.builder()
                        .date(row[0].toString())
                        .revenue(row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO)
                        .orderCount(row[2] != null ? ((Number) row[2]).longValue() : 0L)
                        .build())
                .collect(Collectors.toList());
    }
    
    /**
     * Dashboard cho Admin - tổng quan platform
     */
    @Transactional(readOnly = true)
    public DashboardResponse getAdminDashboard() {
        log.info("Generating admin dashboard");
        
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();
        
        BigDecimal platformRevenue = orderRepository.calculatePlatformRevenue(startOfMonth, now);
        BigDecimal totalRevenue = orderRepository.calculateRevenue(startOfMonth, now);
        Long totalOrders = orderRepository.countByCreatedAtBetween(startOfMonth, now);
        Long totalProducts = productRepository.count();
        
        return DashboardResponse.builder()
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .totalPlatformFee(platformRevenue != null ? platformRevenue : BigDecimal.ZERO)
                .totalOrders(totalOrders)
                .totalProducts(totalProducts)
                .build();
    }
}
