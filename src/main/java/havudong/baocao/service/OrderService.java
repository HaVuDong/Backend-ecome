package havudong.baocao.service;

import havudong.baocao.entity.Order;
import havudong.baocao.entity.OrderItem;
import havudong.baocao.entity.User;
import havudong.baocao.entity.enums.PaymentStatus;
import havudong.baocao.entity.enums.ShippingStatus;
import havudong.baocao.exception.BadRequestException;
import havudong.baocao.exception.ResourceNotFoundException;
import havudong.baocao.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final ProductService productService;
    
    @Transactional
    public Order createOrder(Order order) {
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setShippingStatus(ShippingStatus.PENDING);
        log.info("Creating order for user: {}, seller: {}", 
                order.getUser().getId(), order.getSeller() != null ? order.getSeller().getId() : "N/A");
        return orderRepository.save(order);
    }
    
    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }
    
    @Transactional(readOnly = true)
    public Page<Order> getAllOrders(Pageable pageable) {
        Page<Order> orders = orderRepository.findAll(pageable);
        // Force initialize lazy collections to avoid LazyInitializationException
        orders.getContent().forEach(order -> {
            if (order.getUser() != null) {
                order.getUser().getId(); // Initialize user
            }
            if (order.getSeller() != null) {
                order.getSeller().getId(); // Initialize seller
            }
            if (order.getOrderItems() != null) {
                order.getOrderItems().size(); // Initialize orderItems
                order.getOrderItems().forEach(item -> {
                    if (item.getProduct() != null) {
                        item.getProduct().getId(); // Initialize product
                    }
                });
            }
        });
        return orders;
    }
    
    public Page<Order> getUserOrders(User user, Pageable pageable) {
        return orderRepository.findByUser(user, pageable);
    }
    
    // Lọc đơn hàng theo trạng thái thanh toán
    public Page<Order> getOrdersByPaymentStatus(PaymentStatus paymentStatus, Pageable pageable) {
        return orderRepository.findByPaymentStatus(paymentStatus, pageable);
    }
    
    // Lọc đơn hàng theo trạng thái vận chuyển
    public Page<Order> getOrdersByShippingStatus(ShippingStatus shippingStatus, Pageable pageable) {
        return orderRepository.findByShippingStatus(shippingStatus, pageable);
    }
    
    public Page<Order> getSellerOrders(User seller, Pageable pageable) {
        return orderRepository.findBySeller(seller, pageable);
    }
    
    // Tính doanh thu của seller
    public BigDecimal calculateSellerRevenue(User seller) {
        BigDecimal revenue = orderRepository.calculateSellerRevenue(seller);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }
    
    // Tính doanh thu của seller theo khoảng thời gian
    public BigDecimal calculateSellerRevenue(User seller, LocalDateTime startDate, LocalDateTime endDate) {
        BigDecimal revenue = orderRepository.calculateSellerRevenue(seller, startDate, endDate);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }
    
    public BigDecimal calculateRevenue(LocalDateTime startDate, LocalDateTime endDate) {
        return orderRepository.calculateRevenue(startDate, endDate);
    }
    
    // Cập nhật trạng thái thanh toán
    @Transactional
    public Order updatePaymentStatus(Long id, PaymentStatus paymentStatus) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
        
        log.info("Updating payment status for order: {} from {} to {}", 
                id, order.getPaymentStatus(), paymentStatus);
        
        order.setPaymentStatus(paymentStatus);
        return orderRepository.save(order);
    }
    
    // Cập nhật trạng thái vận chuyển
    @Transactional
    public Order updateShippingStatus(Long id, ShippingStatus shippingStatus) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
        
        log.info("Updating shipping status for order: {} from {} to {}", 
                id, order.getShippingStatus(), shippingStatus);
        
        order.setShippingStatus(shippingStatus);
        return orderRepository.save(order);
    }
    
    /**
     * Hủy đơn hàng và hoàn stock
     */
    @Transactional
    public void cancelOrder(Long id) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
        
        // Chỉ có thể hủy nếu chưa vận chuyển
        if (order.getShippingStatus() != ShippingStatus.PENDING && 
            order.getShippingStatus() != ShippingStatus.PROCESSING) {
            throw new BadRequestException(
                    "Không thể hủy đơn hàng đã được vận chuyển. Trạng thái hiện tại: " + order.getShippingStatus());
        }
        
        log.info("Cancelling order: {}, restoring stock for {} items", id, order.getOrderItems().size());
        
        // Hoàn stock cho tất cả sản phẩm trong đơn hàng
        for (OrderItem item : order.getOrderItems()) {
            productService.restoreStock(item.getProduct().getId(), item.getQuantity());
        }
        
        order.setShippingStatus(ShippingStatus.CANCELLED);
        
        // Nếu đã thanh toán thì đánh dấu cần hoàn tiền
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.REFUNDED);
            log.info("Order {} marked for refund", id);
        } else {
            order.setPaymentStatus(PaymentStatus.CANCELLED);
        }
        
        orderRepository.save(order);
        log.info("Order {} cancelled successfully", id);
    }
    
    /**
     * Tính doanh thu platform (tổng phí hoa hồng)
     */
    public BigDecimal calculatePlatformRevenue(LocalDateTime startDate, LocalDateTime endDate) {
        BigDecimal revenue = orderRepository.calculatePlatformRevenue(startDate, endDate);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }
    
    /**
     * Lấy đơn hàng chi tiết với EntityGraph (tối ưu N+1)
     */
    @Transactional(readOnly = true)
    public Optional<Order> getOrderByIdWithDetails(Long id) {
        return orderRepository.findByIdWithDetails(id);
    }
    
    /**
     * Đếm số đơn hàng của seller
     */
    public long countSellerOrders(User seller) {
        return orderRepository.countBySeller(seller);
    }
}
