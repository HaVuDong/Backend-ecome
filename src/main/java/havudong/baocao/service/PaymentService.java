package havudong.baocao.service;

import havudong.baocao.entity.Order;
import havudong.baocao.entity.enums.PaymentMethod;
import havudong.baocao.entity.enums.PaymentStatus;
import havudong.baocao.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Payment Service - Xử lý thanh toán QR
 * 
 * GIẢI THÍCH KHI BẢO VỆ:
 * 
 * 1. Quy trình thanh toán QR:
 *    - Customer chọn thanh toán QR → Backend sinh mã QR VietQR
 *    - QR chứa: STK ngân hàng, số tiền, nội dung chuyển khoản
 *    - Customer quét mã QR bằng app ngân hàng → Chuyển khoản
 *    - System tự động xác nhận sau 30-60 giây (mock cho demo)
 * 
 * 2. VietQR Format:
 *    - Sử dụng VietQR API (https://vietqr.io)
 *    - Tích hợp với hầu hết ngân hàng Việt Nam
 *    - QR có thể scan bằng app ngân hàng hoặc ví điện tử
 * 
 * 3. Bảo mật:
 *    - Mỗi đơn hàng có mã giao dịch riêng (paymentTransactionId)
 *    - QR có thời hạn 5 phút để tránh gian lận
 *    - Kiểm tra trạng thái trước khi confirm
 * 
 * 4. Mock Auto-Confirm:
 *    - Trong thực tế: sử dụng webhook từ ngân hàng
 *    - Demo: scheduler tự động confirm sau 30-60s
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final OrderRepository orderRepository;

    // ============ THÔNG TIN NGÂN HÀNG ============
    // MB Bank - Military Commercial Joint Stock Bank
    private static final String BANK_ID = "MB";  // Mã ngân hàng MB Bank
    private static final String BANK_ACCOUNT = "037189928";  // Số tài khoản
    private static final String ACCOUNT_NAME = "SHOP ECOMMERCE";  // Tên tài khoản
    
    // QR hết hạn sau 5 phút
    private static final int QR_EXPIRY_MINUTES = 5;
    
    /**
     * Tạo mã QR thanh toán cho đơn hàng
     * 
     * @param orderId ID đơn hàng
     * @return Map chứa thông tin QR (url, transactionId, expiredAt)
     */
    @Transactional
    public Map<String, Object> generateQrPayment(Long orderId) {
        log.info("Generating QR payment for order: {}", orderId);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));
        
        // Kiểm tra trạng thái đơn hàng
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new RuntimeException("Đơn hàng đã được thanh toán");
        }
        
        if (order.getPaymentStatus() == PaymentStatus.CANCELLED) {
            throw new RuntimeException("Đơn hàng đã bị hủy");
        }
        
        // Tạo mã giao dịch unique
        String transactionId = generateTransactionId(orderId);
        
        // Tính thời gian hết hạn
        LocalDateTime expiredAt = LocalDateTime.now().plusMinutes(QR_EXPIRY_MINUTES);
        
        // Tạo URL QR VietQR
        String qrUrl = generateVietQrUrl(order.getFinalAmount().longValue(), transactionId);
        
        // Cập nhật order
        order.setPaymentMethod(PaymentMethod.QR_TRANSFER);
        order.setQrCodeUrl(qrUrl);
        order.setQrExpiredAt(expiredAt);
        order.setPaymentTransactionId(transactionId);
        orderRepository.save(order);
        
        log.info("QR generated successfully - Order: {}, TransactionId: {}", orderId, transactionId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("orderId", orderId);
        result.put("qrCodeUrl", qrUrl);
        result.put("transactionId", transactionId);
        result.put("expiredAt", expiredAt.toString());
        result.put("expiryMinutes", QR_EXPIRY_MINUTES);
        result.put("amount", order.getFinalAmount());
        result.put("bankId", BANK_ID);
        result.put("bankAccount", BANK_ACCOUNT);
        result.put("accountName", ACCOUNT_NAME);
        
        return result;
    }
    
    /**
     * Kiểm tra trạng thái thanh toán
     * 
     * @param orderId ID đơn hàng
     * @return Map chứa trạng thái và thông tin thanh toán
     */
    public Map<String, Object> checkPaymentStatus(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));
        
        Map<String, Object> result = new HashMap<>();
        result.put("orderId", orderId);
        result.put("paymentStatus", order.getPaymentStatus().name());
        result.put("paymentMethod", order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null);
        
        // Kiểm tra QR hết hạn
        if (order.getQrExpiredAt() != null) {
            boolean isExpired = LocalDateTime.now().isAfter(order.getQrExpiredAt());
            result.put("isQrExpired", isExpired);
            result.put("qrExpiredAt", order.getQrExpiredAt().toString());
            
            // Nếu QR hết hạn và chưa thanh toán → đánh dấu FAILED
            if (isExpired && order.getPaymentStatus() == PaymentStatus.PENDING) {
                order.setPaymentStatus(PaymentStatus.FAILED);
                orderRepository.save(order);
                result.put("paymentStatus", PaymentStatus.FAILED.name());
            }
        }
        
        if (order.getPaidAt() != null) {
            result.put("paidAt", order.getPaidAt().toString());
        }
        
        result.put("transactionId", order.getPaymentTransactionId());
        result.put("amount", order.getFinalAmount());
        
        return result;
    }
    
    /**
     * Xác nhận thanh toán thành công (mock cho demo)
     * 
     * Trong thực tế:
     * - Sử dụng webhook từ ngân hàng/payment gateway
     * - Kiểm tra mã giao dịch với database ngân hàng
     * - Verify signature để đảm bảo request từ nguồn tin cậy
     * 
     * @param orderId ID đơn hàng
     * @return Map chứa kết quả confirm
     */
    @Transactional
    public Map<String, Object> confirmPayment(Long orderId) {
        log.info("Confirming payment for order: {}", orderId);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));
        
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new RuntimeException("Đơn hàng đã được thanh toán trước đó");
        }
        
        // Cập nhật trạng thái
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setPaidAt(LocalDateTime.now());
        orderRepository.save(order);
        
        log.info("Payment confirmed successfully - Order: {}", orderId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("orderId", orderId);
        result.put("paymentStatus", PaymentStatus.PAID.name());
        result.put("paidAt", order.getPaidAt().toString());
        result.put("message", "Thanh toán thành công!");
        
        return result;
    }
    
    /**
     * Hủy thanh toán QR (khi hết hạn hoặc user hủy)
     */
    @Transactional
    public Map<String, Object> cancelQrPayment(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));
        
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new RuntimeException("Không thể hủy - đơn hàng đã thanh toán");
        }
        
        // Reset về COD hoặc để user chọn lại
        order.setPaymentMethod(PaymentMethod.COD);
        order.setQrCodeUrl(null);
        order.setQrExpiredAt(null);
        order.setPaymentTransactionId(null);
        order.setPaymentStatus(PaymentStatus.PENDING);
        orderRepository.save(order);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("orderId", orderId);
        result.put("message", "Đã hủy thanh toán QR");
        
        return result;
    }
    
    /**
     * Tìm đơn hàng cần auto-confirm (cho scheduler)
     */
    public Optional<Order> findOrderPendingAutoConfirm() {
        // Tìm đơn hàng QR_TRANSFER, PENDING, đã tạo QR từ 30-60 giây
        // Mock: auto confirm sau 30 giây
        LocalDateTime thirtySecondsAgo = LocalDateTime.now().minusSeconds(30);
        
        return orderRepository.findAll().stream()
                .filter(o -> o.getPaymentMethod() == PaymentMethod.QR_TRANSFER)
                .filter(o -> o.getPaymentStatus() == PaymentStatus.PENDING)
                .filter(o -> o.getQrExpiredAt() != null)
                .filter(o -> o.getQrExpiredAt().isAfter(LocalDateTime.now())) // Chưa hết hạn
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isBefore(thirtySecondsAgo))
                .findFirst();
    }
    
    // ============ PRIVATE METHODS ============
    
    /**
     * Tạo mã giao dịch unique
     * Format: DH{orderId}_{timestamp}
     */
    private String generateTransactionId(Long orderId) {
        return "DH" + orderId + "_" + System.currentTimeMillis();
    }
    
    /**
     * Tạo URL mã QR VietQR
     * 
     * VietQR API: https://img.vietqr.io/image/{BANK_ID}-{ACCOUNT}-{TEMPLATE}.png
     * Parameters:
     * - amount: Số tiền
     * - addInfo: Nội dung chuyển khoản
     * - accountName: Tên tài khoản
     */
    private String generateVietQrUrl(long amount, String transactionId) {
        try {
            String template = "compact2";  // Template QR (compact, compact2, qr_only, print)
            String encodedAddInfo = URLEncoder.encode(transactionId, StandardCharsets.UTF_8.toString());
            String encodedAccountName = URLEncoder.encode(ACCOUNT_NAME, StandardCharsets.UTF_8.toString());
            
            // VietQR URL format
            String url = String.format(
                "https://img.vietqr.io/image/%s-%s-%s.png?amount=%d&addInfo=%s&accountName=%s",
                BANK_ID,
                BANK_ACCOUNT,
                template,
                amount,
                encodedAddInfo,
                encodedAccountName
            );
            
            return url;
        } catch (Exception e) {
            log.error("Error generating VietQR URL", e);
            throw new RuntimeException("Lỗi tạo mã QR");
        }
    }
}
