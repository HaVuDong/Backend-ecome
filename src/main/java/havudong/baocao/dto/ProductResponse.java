package havudong.baocao.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stock;
    private String mainImage;
    private BigDecimal rating;
    private Integer soldCount;
    private Boolean isActive;
    private LocalDateTime createdAt;
    
    // Seller info (không expose toàn bộ User entity)
    private SellerInfo seller;
    
    // Category info
    private CategoryInfo category;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SellerInfo {
        private Long id;
        private String fullName;
        private String email;
        private String avatarUrl;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryInfo {
        private Long id;
        private String name;
        private String icon;
    }
}
