package havudong.baocao.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO Request để tạo review
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRequest {
    
    @NotNull(message = "Product ID không được để trống")
    private Long productId;
    
    @NotNull(message = "Order ID không được để trống")
    private Long orderId;
    
    @NotNull(message = "Rating không được để trống")
    @Min(value = 1, message = "Rating phải từ 1-5")
    @Max(value = 5, message = "Rating phải từ 1-5")
    private Integer rating;
    
    private String comment;
    
    private String imageUrls;
}
