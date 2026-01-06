package havudong.baocao.dto;

import havudong.baocao.entity.enums.PaymentStatus;
import havudong.baocao.entity.enums.ShippingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    
    private Long id;
    private BigDecimal totalAmount;
    private BigDecimal shippingFee;
    private BigDecimal discountAmount;
    private BigDecimal platformFee;
    private BigDecimal sellerAmount;
    private BigDecimal finalAmount;
    private PaymentStatus paymentStatus;
    private ShippingStatus shippingStatus;
    private String shippingAddress;
    private String shippingPhone;
    private String shippingName;
    private String paymentMethod;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // User (customer) info
    private UserInfo user;
    
    // Seller info
    private UserInfo seller;
    
    // Order items
    private List<OrderItemInfo> items;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String fullName;
        private String email;
        private String phone;
        private String avatarUrl;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemInfo {
        private Long id;
        private Long productId;
        private String productName;
        private String productImage;
        private BigDecimal price;
        private Integer quantity;
        private BigDecimal subtotal;
    }
}
