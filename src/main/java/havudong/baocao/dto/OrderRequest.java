package havudong.baocao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {
    
    @NotNull(message = "User ID không được để trống")
    private Long userId;
    
    @NotNull(message = "Tổng tiền không được để trống")
    private BigDecimal totalAmount;
    
    private BigDecimal shippingFee = BigDecimal.ZERO;
    
    private BigDecimal discountAmount = BigDecimal.ZERO;
    
    @NotBlank(message = "Địa chỉ giao hàng không được để trống")
    @Size(max = 500, message = "Địa chỉ không được quá 500 ký tự")
    private String shippingAddress;
    
    @NotBlank(message = "Số điện thoại không được để trống")
    private String shippingPhone;
    
    @NotBlank(message = "Tên người nhận không được để trống")
    private String shippingName;
    
    @NotBlank(message = "Phương thức thanh toán không được để trống")
    private String paymentMethod;
    
    @Size(max = 500, message = "Ghi chú không được quá 500 ký tự")
    private String note;
    
    @NotNull(message = "Danh sách sản phẩm không được để trống")
    private List<OrderItemRequest> items;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemRequest {
        @NotNull(message = "Product ID không được để trống")
        private Long productId;
        
        @NotNull(message = "Số lượng không được để trống")
        private Integer quantity;
    }
}
