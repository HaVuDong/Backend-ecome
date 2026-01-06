package havudong.baocao.mapper;

import havudong.baocao.dto.OrderRequest;
import havudong.baocao.dto.OrderResponse;
import havudong.baocao.entity.Order;
import havudong.baocao.entity.OrderItem;
import havudong.baocao.entity.Product;
import havudong.baocao.entity.User;
import havudong.baocao.entity.enums.PaymentMethod;
import havudong.baocao.entity.enums.PaymentStatus;
import havudong.baocao.entity.enums.ShippingStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderMapper {
    
    public Order toEntity(OrderRequest request, User user, List<Product> products) {
        Order order = new Order();
        order.setUser(user);
        order.setTotalAmount(request.getTotalAmount());
        order.setShippingFee(request.getShippingFee());
        order.setDiscountAmount(request.getDiscountAmount());
        
        // Calculate final amount
        BigDecimal finalAmount = request.getTotalAmount()
                .add(request.getShippingFee())
                .subtract(request.getDiscountAmount());
        order.setFinalAmount(finalAmount);
        
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setShippingStatus(ShippingStatus.PENDING);
        order.setShippingAddress(request.getShippingAddress());
        order.setShippingPhone(request.getShippingPhone());
        order.setShippingName(request.getShippingName());
        
        // Convert String to PaymentMethod enum
        try {
            PaymentMethod method = PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase());
            order.setPaymentMethod(method);
        } catch (IllegalArgumentException | NullPointerException e) {
            order.setPaymentMethod(PaymentMethod.COD);
        }
        
        order.setNote(request.getNote());
        
        // Create order items
        List<OrderItem> orderItems = new ArrayList<>();
        for (OrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            Product product = products.stream()
                    .filter(p -> p.getId().equals(itemRequest.getProductId()))
                    .findFirst()
                    .orElseThrow();
            
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setProductName(product.getName());
            orderItem.setPrice(product.getPrice());
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setSubtotal(product.getPrice().multiply(new BigDecimal(itemRequest.getQuantity())));
            
            orderItems.add(orderItem);
        }
        
        order.setOrderItems(orderItems);
        return order;
    }
    
    public OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .totalAmount(order.getTotalAmount())
                .shippingFee(order.getShippingFee())
                .discountAmount(order.getDiscountAmount())
                .platformFee(order.getPlatformFee())
                .sellerAmount(order.getSellerAmount())
                .finalAmount(order.getFinalAmount())
                .paymentStatus(order.getPaymentStatus())
                .shippingStatus(order.getShippingStatus())
                .shippingAddress(order.getShippingAddress())
                .shippingPhone(order.getShippingPhone())
                .shippingName(order.getShippingName())
                .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : "COD")
                .note(order.getNote())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .user(OrderResponse.UserInfo.builder()
                        .id(order.getUser().getId())
                        .fullName(order.getUser().getFullName())
                        .email(order.getUser().getEmail())
                        .phone(order.getUser().getPhone())
                        .avatarUrl(order.getUser().getAvatarUrl())
                        .build())
                .seller(order.getSeller() != null ? OrderResponse.UserInfo.builder()
                        .id(order.getSeller().getId())
                        .fullName(order.getSeller().getFullName())
                        .email(order.getSeller().getEmail())
                        .phone(order.getSeller().getPhone())
                        .avatarUrl(order.getSeller().getAvatarUrl())
                        .build() : null)
                .items(order.getOrderItems().stream()
                        .map(item -> OrderResponse.OrderItemInfo.builder()
                                .id(item.getId())
                                .productId(item.getProduct().getId())
                                .productName(item.getProductName())
                                .productImage(item.getProduct().getMainImage())
                                .price(item.getPrice())
                                .quantity(item.getQuantity())
                                .subtotal(item.getSubtotal())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
