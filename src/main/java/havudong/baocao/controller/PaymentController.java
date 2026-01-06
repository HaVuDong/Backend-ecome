package havudong.baocao.controller;

import havudong.baocao.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Payment Controller - API thanh toán QR
 * 
 * GIẢI THÍCH KHI BẢO VỆ:
 * 
 * Các API endpoints:
 * 
 * 1. POST /api/payments/qr/{orderId}
 *    - Tạo mã QR thanh toán cho đơn hàng
 *    - Input: orderId
 *    - Output: qrCodeUrl, transactionId, expiredAt, bankInfo
 * 
 * 2. GET /api/payments/qr/{orderId}/status
 *    - Kiểm tra trạng thái thanh toán
 *    - Frontend gọi polling mỗi 5-10 giây
 *    - Output: paymentStatus, isExpired, paidAt
 * 
 * 3. POST /api/payments/qr/{orderId}/cancel
 *    - Hủy thanh toán QR (quay về COD)
 *    - Gọi khi user nhấn nút quay lại
 * 
 * 4. POST /api/payments/qr/{orderId}/confirm (Internal/Admin)
 *    - Xác nhận thanh toán thủ công
 *    - Trong thực tế sử dụng webhook từ ngân hàng
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Tạo mã QR thanh toán cho đơn hàng
     * 
     * Flow:
     * 1. User chọn thanh toán QR ở checkout
     * 2. Frontend gọi API này với orderId
     * 3. Backend tạo QR VietQR với thông tin bank + amount + transactionId
     * 4. Frontend hiển thị QR và bắt đầu countdown + polling
     */
    @PostMapping("/qr/{orderId}")
    public ResponseEntity<?> generateQrPayment(@PathVariable Long orderId) {
        try {
            log.info("API: Generate QR payment for order {}", orderId);
            Map<String, Object> result = paymentService.generateQrPayment(orderId);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            log.error("Error generating QR payment: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Kiểm tra trạng thái thanh toán
     * 
     * Frontend polling API này mỗi 5-10 giây để check:
     * - PENDING: Chờ thanh toán (tiếp tục polling)
     * - PAID: Thanh toán thành công → navigate to success screen
     * - FAILED/EXPIRED: QR hết hạn → show error và option tạo QR mới
     */
    @GetMapping("/qr/{orderId}/status")
    public ResponseEntity<?> checkPaymentStatus(@PathVariable Long orderId) {
        try {
            Map<String, Object> result = paymentService.checkPaymentStatus(orderId);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            log.error("Error checking payment status: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Hủy thanh toán QR
     * 
     * Gọi khi:
     * - User nhấn nút "Quay lại" hoặc "Hủy"
     * - QR hết hạn và user chọn đổi phương thức thanh toán
     */
    @PostMapping("/qr/{orderId}/cancel")
    public ResponseEntity<?> cancelQrPayment(@PathVariable Long orderId) {
        try {
            log.info("API: Cancel QR payment for order {}", orderId);
            Map<String, Object> result = paymentService.cancelQrPayment(orderId);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            log.error("Error canceling QR payment: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Xác nhận thanh toán thành công (Mock cho demo)
     * 
     * QUAN TRỌNG - GIẢI THÍCH KHI BẢO VỆ:
     * 
     * Trong môi trường PRODUCTION:
     * - API này sẽ được thay thế bằng Webhook từ ngân hàng
     * - Ngân hàng gửi callback khi nhận được tiền
     * - Verify signature để đảm bảo request hợp lệ
     * 
     * Trong môi trường DEMO (luận văn):
     * - Sử dụng Scheduler tự động confirm sau 30-60 giây
     * - Hoặc Admin có thể confirm thủ công qua API này
     * - Mục đích: Demo flow thanh toán hoạt động
     */
    @PostMapping("/qr/{orderId}/confirm")
    public ResponseEntity<?> confirmPayment(@PathVariable Long orderId) {
        try {
            log.info("API: Confirm payment for order {}", orderId);
            Map<String, Object> result = paymentService.confirmPayment(orderId);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            log.error("Error confirming payment: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Lấy thông tin ngân hàng để hiển thị (backup nếu QR không load)
     */
    @GetMapping("/bank-info")
    public ResponseEntity<?> getBankInfo() {
        return ResponseEntity.ok(Map.of(
            "bankId", "MB",
            "bankName", "Ngân hàng Quân đội (MB Bank)",
            "accountNumber", "037189928",
            "accountName", "SHOP ECOMMERCE",
            "note", "Nội dung CK: Mã đơn hàng của bạn"
        ));
    }
}
