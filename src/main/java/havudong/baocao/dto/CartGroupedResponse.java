package havudong.baocao.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO cho giỏ hàng đã group theo seller
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartGroupedResponse {
    
    private List<SellerCartGroup> sellerGroups;
    private BigDecimal totalAmount;
    private Integer totalItems;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SellerCartGroup {
        private Long sellerId;
        private String sellerName;
        private String sellerAvatar;
        private List<CartItemDetail> items;
        private BigDecimal subtotal;
        private Integer itemCount;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemDetail {
        private Long cartItemId;
        private Long productId;
        private String productName;
        private String productImage;
        private BigDecimal price;
        private BigDecimal originalPrice;
        private Integer quantity;
        private Integer stock;
        private Boolean selected;
        private BigDecimal subtotal;
    }
}
