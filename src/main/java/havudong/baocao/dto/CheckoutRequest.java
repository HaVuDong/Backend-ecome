package havudong.baocao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO Request để tạo đơn hàng từ giỏ hàng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest {
    
    @NotEmpty(message = "Phải chọn ít nhất 1 sản phẩm để đặt hàng")
    private List<Long> cartItemIds;
    
    @NotBlank(message = "Địa chỉ giao hàng không được để trống")
    private String shippingAddress;
    
    @NotBlank(message = "Số điện thoại không được để trống")
    private String shippingPhone;
    
    @NotBlank(message = "Tên người nhận không được để trống")
    private String shippingName;
    
    @NotNull(message = "Phương thức thanh toán không được để trống")
    private String paymentMethod;
    
    private String note;
    
    private String voucherCode;
}
