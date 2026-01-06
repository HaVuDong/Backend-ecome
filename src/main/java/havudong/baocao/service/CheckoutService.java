package havudong.baocao.service;

import havudong.baocao.dto.CheckoutRequest;
import havudong.baocao.dto.OrderResponse;
import havudong.baocao.entity.*;
import havudong.baocao.entity.enums.PaymentMethod;
import havudong.baocao.entity.enums.PaymentStatus;
import havudong.baocao.entity.enums.ShippingStatus;
import havudong.baocao.exception.BadRequestException;
import havudong.baocao.exception.OutOfStockException;
import havudong.baocao.exception.ResourceNotFoundException;
import havudong.baocao.mapper.OrderMapper;
import havudong.baocao.repository.CartItemRepository;
import havudong.baocao.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service xử lý checkout và tạo đơn hàng
 * - Tự động tách đơn hàng theo seller
 * - Validate và lock stock
 * - Tính phí platform commission
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutService {
    
    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final OrderMapper orderMapper;
    
    // Phí platform (commission) - mặc định 5%
    @Value("${app.platform.fee-rate:0.05}")
    private BigDecimal platformFeeRate;
    
    // Phí ship mặc định
    @Value("${app.shipping.default-fee:30000}")
    private BigDecimal defaultShippingFee;
    
    /**
     * Checkout từ giỏ hàng - tự động tách đơn theo seller
     * @return Danh sách các đơn hàng đã tạo (mỗi seller 1 đơn)
     */
    @Transactional
    public List<OrderResponse> checkout(User customer, CheckoutRequest request) {
        log.info("Starting checkout for user: {}, cart items: {}", 
                customer.getId(), request.getCartItemIds());
        
        // 1. Lấy các cart items được chọn
        List<CartItem> cartItems = cartItemRepository.findAllById(request.getCartItemIds());
        
        if (cartItems.isEmpty()) {
            throw new BadRequestException("Không tìm thấy sản phẩm trong giỏ hàng");
        }
        
        // Validate ownership
        for (CartItem item : cartItems) {
            if (!item.getUser().getId().equals(customer.getId())) {
                throw new BadRequestException("Cart item không thuộc về bạn");
            }
        }
        
        // 2. Group cart items theo seller
        Map<Long, List<CartItem>> itemsBySeller = cartItems.stream()
                .collect(Collectors.groupingBy(item -> item.getProduct().getSeller().getId()));
        
        log.info("Splitting order into {} seller groups", itemsBySeller.size());
        
        // 3. Validate stock cho tất cả sản phẩm trước
        validateStock(cartItems);
        
        // 4. Tạo order cho mỗi seller
        List<Order> createdOrders = new ArrayList<>();
        
        for (Map.Entry<Long, List<CartItem>> entry : itemsBySeller.entrySet()) {
            Long sellerId = entry.getKey();
            List<CartItem> sellerItems = entry.getValue();
            
            Order order = createOrderForSeller(customer, sellerItems, request);
            createdOrders.add(order);
            
            log.info("Created order {} for seller {} with {} items", 
                    order.getId(), sellerId, sellerItems.size());
        }
        
        // 5. Trừ stock sau khi tất cả orders được tạo thành công
        for (CartItem item : cartItems) {
            productService.decreaseStock(item.getProduct().getId(), item.getQuantity());
        }
        
        // 6. Xóa cart items đã checkout
        cartItemRepository.deleteAll(cartItems);
        
        log.info("Checkout completed. Created {} orders for user {}", 
                createdOrders.size(), customer.getId());
        
        return createdOrders.stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Validate stock cho tất cả items trước khi tạo order
     */
    private void validateStock(List<CartItem> cartItems) {
        for (CartItem item : cartItems) {
            Product product = productService.getProductByIdWithLock(item.getProduct().getId());
            
            if (!product.getIsActive()) {
                throw new BadRequestException(
                        String.format("Sản phẩm '%s' đã ngừng bán", product.getName()));
            }
            
            if (product.getStock() < item.getQuantity()) {
                throw new OutOfStockException(product.getName(), item.getQuantity(), product.getStock());
            }
        }
    }
    
    /**
     * Tạo order cho 1 seller từ danh sách cart items
     */
    private Order createOrderForSeller(User customer, List<CartItem> items, CheckoutRequest request) {
        // Lấy seller từ product đầu tiên (tất cả items có cùng seller)
        User seller = items.get(0).getProduct().getSeller();
        
        Order order = new Order();
        order.setUser(customer);
        order.setSeller(seller);
        
        // Thông tin giao hàng
        order.setShippingAddress(request.getShippingAddress());
        order.setShippingPhone(request.getShippingPhone());
        order.setShippingName(request.getShippingName());
        
        // Chuyển đổi paymentMethod từ String sang Enum
        try {
            PaymentMethod method = PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase());
            order.setPaymentMethod(method);
        } catch (IllegalArgumentException e) {
            // Mặc định là COD nếu không hợp lệ
            order.setPaymentMethod(PaymentMethod.COD);
        }
        
        order.setNote(request.getNote());
        
        // Trạng thái ban đầu
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setShippingStatus(ShippingStatus.PENDING);
        
        // Tạo order items và tính tổng tiền
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (CartItem cartItem : items) {
            Product product = cartItem.getProduct();
            
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setProductName(product.getName());
            orderItem.setPrice(product.getPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            
            BigDecimal subtotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            orderItem.setSubtotal(subtotal);
            
            orderItems.add(orderItem);
            totalAmount = totalAmount.add(subtotal);
        }
        
        order.setOrderItems(orderItems);
        order.setTotalAmount(totalAmount);
        
        // Phí ship (có thể tính theo seller hoặc khoảng cách)
        order.setShippingFee(defaultShippingFee);
        
        // Discount (có thể implement voucher system sau)
        order.setDiscountAmount(BigDecimal.ZERO);
        
        // Tính phí platform commission
        BigDecimal platformFee = totalAmount.multiply(platformFeeRate)
                .setScale(0, RoundingMode.HALF_UP);
        order.setPlatformFee(platformFee);
        
        // Số tiền seller nhận được
        BigDecimal sellerAmount = totalAmount.subtract(platformFee);
        order.setSellerAmount(sellerAmount);
        
        // Tổng tiền khách hàng phải trả
        BigDecimal finalAmount = totalAmount
                .add(order.getShippingFee())
                .subtract(order.getDiscountAmount());
        order.setFinalAmount(finalAmount);
        
        return orderRepository.save(order);
    }
}
