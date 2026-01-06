package havudong.baocao.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO cho Wishlist item
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishlistResponse {
    
    private Long id;
    private Long productId;
    private String productName;
    private String productImage;
    private BigDecimal productPrice;
    private BigDecimal productOriginalPrice;
    private Integer productStock;
    private Boolean isAvailable;          // Còn hàng và active
    private String sellerName;
    private Long sellerId;
    private LocalDateTime addedAt;
}
