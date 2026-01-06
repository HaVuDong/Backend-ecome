package havudong.baocao.controller;

import havudong.baocao.dto.ApiResponse;
import havudong.baocao.dto.OrderResponse;
import havudong.baocao.entity.Order;
import havudong.baocao.entity.User;
import havudong.baocao.entity.enums.PaymentStatus;
import havudong.baocao.entity.enums.ShippingStatus;
import havudong.baocao.exception.ResourceNotFoundException;
import havudong.baocao.exception.UnauthorizedException;
import havudong.baocao.mapper.OrderMapper;
import havudong.baocao.service.OrderService;
import havudong.baocao.service.UserService;
import havudong.baocao.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {
    
    private final OrderService orderService;
    private final UserService userService;
    private final OrderMapper orderMapper;
    private final SecurityUtil securityUtil;
    
    /**
     * Tạo đơn hàng từ request body (dùng cho API cũ)
     * Recommend: Sử dụng /api/cart/checkout thay thế
     */
    @PostMapping
    @Deprecated
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@RequestBody Order order) {
        Order created = orderService.createOrder(order);
        OrderResponse response = orderMapper.toResponse(created);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo đơn hàng thành công", response));
    }
    
    /**
     * Admin: Lấy tất cả đơn hàng
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction
    ) {
        Sort.Direction sortDirection = direction.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<Order> orders = orderService.getAllOrders(pageable);
        Page<OrderResponse> responses = orders.map(orderMapper::toResponse);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id)
            .map(order -> ResponseEntity.ok(ApiResponse.success(orderMapper.toResponse(order))))
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Lấy đơn hàng chi tiết với EntityGraph (tối ưu N+1)
     */
    @GetMapping("/{id}/details")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderDetails(@PathVariable Long id) {
        return orderService.getOrderByIdWithDetails(id)
            .map(order -> ResponseEntity.ok(ApiResponse.success(orderMapper.toResponse(order))))
            .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
    }
    
    /**
     * Lấy đơn hàng của user hiện tại (từ JWT)
     */
    @GetMapping("/my-orders")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        User currentUser = securityUtil.getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orders = orderService.getUserOrders(currentUser, pageable);
        Page<OrderResponse> responses = orders.map(orderMapper::toResponse);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
    
    /**
     * Lấy đơn hàng của seller hiện tại (từ JWT)
     */
    @GetMapping("/seller/my-orders")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMySellerOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        User seller = securityUtil.getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orders = orderService.getSellerOrders(seller, pageable);
        Page<OrderResponse> responses = orders.map(orderMapper::toResponse);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
    
    /**
     * Tính doanh thu của seller hiện tại
     */
    @GetMapping("/seller/my-revenue")
    public ResponseEntity<ApiResponse<Map<String, BigDecimal>>> getMyRevenue(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        User seller = securityUtil.getCurrentUser();
        BigDecimal revenue;
        if (startDate != null && endDate != null) {
            LocalDateTime start = LocalDateTime.parse(startDate);
            LocalDateTime end = LocalDateTime.parse(endDate);
            revenue = orderService.calculateSellerRevenue(seller, start, end);
        } else {
            revenue = orderService.calculateSellerRevenue(seller);
        }
        return ResponseEntity.ok(ApiResponse.success(Map.of("revenue", revenue != null ? revenue : BigDecimal.ZERO)));
    }
    
    // ========== Legacy endpoints (backward compatible) ==========
    
    @GetMapping("/user/{userId}")
    @Deprecated
    public ResponseEntity<Page<OrderResponse>> getUserOrders(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return userService.getUserById(userId)
            .map(user -> {
                Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
                Page<Order> orders = orderService.getUserOrders(user, pageable);
                return ResponseEntity.ok(orders.map(orderMapper::toResponse));
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    // Lọc đơn hàng theo trạng thái thanh toán
    @GetMapping("/payment-status/{paymentStatus}")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getOrdersByPaymentStatus(
            @PathVariable PaymentStatus paymentStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orders = orderService.getOrdersByPaymentStatus(paymentStatus, pageable);
        Page<OrderResponse> responses = orders.map(orderMapper::toResponse);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
    
    // Lọc đơn hàng theo trạng thái vận chuyển
    @GetMapping("/shipping-status/{shippingStatus}")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getOrdersByShippingStatus(
            @PathVariable ShippingStatus shippingStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orders = orderService.getOrdersByShippingStatus(shippingStatus, pageable);
        Page<OrderResponse> responses = orders.map(orderMapper::toResponse);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
    
    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<Page<OrderResponse>> getSellerOrders(
            @PathVariable Long sellerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return userService.getUserById(sellerId)
            .map(seller -> {
                Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
                Page<Order> orders = orderService.getSellerOrders(seller, pageable);
                return ResponseEntity.ok(orders.map(orderMapper::toResponse));
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    // Tính doanh thu của seller
    @GetMapping("/seller/{sellerId}/revenue")
    public ResponseEntity<ApiResponse<Map<String, BigDecimal>>> calculateSellerRevenue(
            @PathVariable Long sellerId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        return userService.getUserById(sellerId)
            .map(seller -> {
                BigDecimal revenue;
                if (startDate != null && endDate != null) {
                    LocalDateTime start = LocalDateTime.parse(startDate);
                    LocalDateTime end = LocalDateTime.parse(endDate);
                    revenue = orderService.calculateSellerRevenue(seller, start, end);
                } else {
                    revenue = orderService.calculateSellerRevenue(seller);
                }
                return ResponseEntity.ok(ApiResponse.success(Map.of("revenue", revenue != null ? revenue : BigDecimal.ZERO)));
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Admin: Tính tổng doanh thu platform
     */
    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<Map<String, BigDecimal>>> calculateRevenue(
            @RequestParam String startDate,
            @RequestParam String endDate
    ) {
        LocalDateTime start = LocalDateTime.parse(startDate);
        LocalDateTime end = LocalDateTime.parse(endDate);
        BigDecimal revenue = orderService.calculateRevenue(start, end);
        BigDecimal platformRevenue = orderService.calculatePlatformRevenue(start, end);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "totalRevenue", revenue != null ? revenue : BigDecimal.ZERO,
            "platformRevenue", platformRevenue != null ? platformRevenue : BigDecimal.ZERO
        )));
    }
    
    /**
     * Seller cập nhật trạng thái thanh toán (kiểm tra quyền)
     */
    @PutMapping("/{id}/payment-status")
    public ResponseEntity<ApiResponse<OrderResponse>> updatePaymentStatus(
            @PathVariable Long id,
            @RequestParam PaymentStatus paymentStatus
    ) {
        User currentUser = securityUtil.getCurrentUser();
        Order order = orderService.getOrderById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
        
        // Kiểm tra quyền: seller của order hoặc admin
        if (order.getSeller() != null && !order.getSeller().getId().equals(currentUser.getId())) {
            // Có thể add check admin role ở đây
            throw new UnauthorizedException("Bạn không có quyền cập nhật đơn hàng này");
        }
        
        Order updated = orderService.updatePaymentStatus(id, paymentStatus);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thanh toán thành công", orderMapper.toResponse(updated)));
    }
    
    /**
     * Seller cập nhật trạng thái vận chuyển (kiểm tra quyền)
     */
    @PutMapping("/{id}/shipping-status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateShippingStatus(
            @PathVariable Long id,
            @RequestParam ShippingStatus shippingStatus
    ) {
        User currentUser = securityUtil.getCurrentUser();
        Order order = orderService.getOrderById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
        
        // Kiểm tra quyền
        if (order.getSeller() != null && !order.getSeller().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("Bạn không có quyền cập nhật đơn hàng này");
        }
        
        Order updated = orderService.updateShippingStatus(id, shippingStatus);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái vận chuyển thành công", orderMapper.toResponse(updated)));
    }
    
    /**
     * Customer hủy đơn hàng (kiểm tra quyền)
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(@PathVariable Long id) {
        User currentUser = securityUtil.getCurrentUser();
        Order order = orderService.getOrderById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
        
        // Kiểm tra quyền: chỉ customer của order hoặc admin được hủy
        if (!order.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("Bạn không có quyền hủy đơn hàng này");
        }
        
        // Chỉ được hủy khi chưa giao
        if (order.getShippingStatus() == ShippingStatus.DELIVERED) {
            throw new IllegalStateException("Không thể hủy đơn hàng đã giao");
        }
        
        orderService.cancelOrder(id);
        log.info("Order {} cancelled by user {}", id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Đã hủy đơn hàng", null));
    }
}

