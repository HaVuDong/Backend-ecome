package havudong.baocao.repository;

import havudong.baocao.entity.Order;
import havudong.baocao.entity.User;
import havudong.baocao.entity.enums.PaymentStatus;
import havudong.baocao.entity.enums.ShippingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    // Customer orders
    Page<Order> findByUser(User user, Pageable pageable);
    
    // Seller orders (mỗi order giờ có seller riêng)
    Page<Order> findBySeller(User seller, Pageable pageable);
    
    // Lọc theo trạng thái thanh toán
    Page<Order> findByPaymentStatus(PaymentStatus paymentStatus, Pageable pageable);
    
    // Lọc theo trạng thái vận chuyển
    Page<Order> findByShippingStatus(ShippingStatus shippingStatus, Pageable pageable);
    
    // Lọc theo cả hai trạng thái
    Page<Order> findByPaymentStatusAndShippingStatus(PaymentStatus paymentStatus, ShippingStatus shippingStatus, Pageable pageable);
    
    // EntityGraph để tránh N+1 query
    @EntityGraph(attributePaths = {"user", "seller", "orderItems", "orderItems.product"})
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdWithDetails(Long id);
    
    // Legacy query cho backward compatibility
    @Query("SELECT o FROM Order o JOIN o.orderItems oi " +
           "WHERE oi.product.seller = :seller")
    Page<Order> findOrdersBySeller(User seller, Pageable pageable);
    
    // Tính doanh thu của seller (dùng sellerAmount thay vì subtotal)
    @Query("SELECT SUM(o.sellerAmount) FROM Order o " +
           "WHERE o.seller = :seller " +
           "AND o.shippingStatus = 'DELIVERED' " +
           "AND o.paymentStatus = 'PAID'")
    BigDecimal calculateSellerRevenue(User seller);
    
    // Tính doanh thu của seller theo khoảng thời gian
    @Query("SELECT SUM(o.sellerAmount) FROM Order o " +
           "WHERE o.seller = :seller " +
           "AND o.shippingStatus = 'DELIVERED' " +
           "AND o.paymentStatus = 'PAID' " +
           "AND o.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal calculateSellerRevenue(User seller, LocalDateTime startDate, LocalDateTime endDate);
    
    // Tính tổng doanh thu platform
    @Query("SELECT SUM(o.finalAmount) FROM Order o " +
           "WHERE o.shippingStatus = 'DELIVERED' " +
           "AND o.paymentStatus = 'PAID' " +
           "AND o.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal calculateRevenue(LocalDateTime startDate, LocalDateTime endDate);
    
    // Tính tổng phí platform thu được
    @Query("SELECT SUM(o.platformFee) FROM Order o " +
           "WHERE o.shippingStatus = 'DELIVERED' " +
           "AND o.paymentStatus = 'PAID' " +
           "AND o.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal calculatePlatformRevenue(LocalDateTime startDate, LocalDateTime endDate);
    
    // Đếm theo trạng thái thanh toán
    long countByPaymentStatus(PaymentStatus paymentStatus);
    
    // Đếm theo trạng thái vận chuyển
    long countByShippingStatus(ShippingStatus shippingStatus);
    
    // Đếm đơn hàng của seller
    long countBySeller(User seller);
    
    // Đếm đơn hàng của seller theo trạng thái shipping
    long countBySellerAndShippingStatus(User seller, ShippingStatus shippingStatus);
    
    // Tính tổng phí platform của seller
    @Query("SELECT SUM(o.platformFee) FROM Order o WHERE o.seller = :seller AND o.paymentStatus = 'PAID'")
    BigDecimal calculateTotalPlatformFee(User seller);
    
    // Top sản phẩm bán chạy của seller
    @Query("SELECT oi.product.id, oi.product.name, oi.product.mainImage, " +
           "SUM(oi.quantity), SUM(oi.price * oi.quantity) " +
           "FROM OrderItem oi " +
           "WHERE oi.product.seller.id = :sellerId " +
           "AND oi.order.paymentStatus = 'PAID' " +
           "GROUP BY oi.product.id, oi.product.name, oi.product.mainImage " +
           "ORDER BY SUM(oi.quantity) DESC")
    List<Object[]> getTopSellingProductsBySeller(Long sellerId, Pageable pageable);
    
    // Doanh thu theo ngày của seller (native query cho MySQL)
    @Query(value = "SELECT DATE(o.created_at) as date, SUM(o.seller_amount), COUNT(*) " +
           "FROM orders o " +
           "WHERE o.seller_id = :sellerId " +
           "AND o.payment_status = 'PAID' " +
           "AND o.created_at BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(o.created_at) " +
           "ORDER BY DATE(o.created_at)", nativeQuery = true)
    List<Object[]> getRevenueByDateForSeller(Long sellerId, LocalDateTime startDate, LocalDateTime endDate);
    
    // Đếm đơn trong khoảng thời gian
    long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // For recommendation system
    List<Order> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    
    // New method for improved recommendation system - get recent purchases
    List<Order> findByUserAndCreatedAtAfter(User user, LocalDateTime since);
    
    @Query("SELECT DISTINCT o FROM Order o " +
           "JOIN o.orderItems oi " +
           "WHERE oi.product.id = :productId " +
           "AND o.paymentStatus = 'PAID'")
    List<Order> findOrdersContainingProduct(Long productId);
    
    Page<Order> findByShippingAddressContaining(String province, Pageable pageable);
}
