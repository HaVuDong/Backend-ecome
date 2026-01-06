package havudong.baocao.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception khi sản phẩm hết hàng hoặc không đủ số lượng
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class OutOfStockException extends RuntimeException {
    
    public OutOfStockException(String productName, int requested, int available) {
        super(String.format("Sản phẩm '%s' không đủ số lượng. Yêu cầu: %d, Còn lại: %d", 
                productName, requested, available));
    }
    
    public OutOfStockException(String message) {
        super(message);
    }
}
