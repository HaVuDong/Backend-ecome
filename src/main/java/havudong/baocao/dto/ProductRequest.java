package havudong.baocao.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {
    
    @NotNull(message = "Seller ID không được để trống")
    private Long sellerId;
    
    @NotNull(message = "Category ID không được để trống")
    private Long categoryId;
    
    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(min = 3, max = 255, message = "Tên sản phẩm phải từ 3-255 ký tự")
    private String name;
    
    @Size(max = 5000, message = "Mô tả không được quá 5000 ký tự")
    private String description;
    
    @NotNull(message = "Giá sản phẩm không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá phải lớn hơn 0")
    private BigDecimal price;
    
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá gốc phải lớn hơn 0")
    private BigDecimal originalPrice;
    
    @NotNull(message = "Số lượng tồn kho không được để trống")
    @Min(value = 0, message = "Số lượng tồn kho không được âm")
    private Integer stock;
    
    private String mainImage;
    
    private Boolean isActive = true;
}
