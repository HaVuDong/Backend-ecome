package havudong.baocao.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {
    
    private Long id;
    private Integer quantity;
    private BigDecimal price;
    private Boolean selected;
    
    // Product info (simplified)
    private ProductInfo product;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductInfo {
        private Long id;
        private String name;
        private BigDecimal price;
        private Integer stock;
        private String mainImage;
        private Boolean isActive;
    }
}
