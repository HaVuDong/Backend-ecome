package havudong.baocao.scheduler;

import havudong.baocao.entity.Order;
import havudong.baocao.entity.enums.PaymentMethod;
import havudong.baocao.entity.enums.PaymentStatus;
import havudong.baocao.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * QR Payment Scheduler - Auto confirm và xử lý expired
 * 
 * GIẢI THÍCH KHI BẢO VỆ:
 * 
 * 1. Auto Confirm (Mock):
 *    - Tự động xác nhận thanh toán sau 30-60 giây
 *    - Mô phỏng việc ngân hàng thông báo đã nhận tiền
 *    - Trong thực tế: sử dụng webhook từ payment gateway
 * 
 * 2. Auto Expire:
 *    - Kiểm tra các QR đã hết hạn (quá 5 phút)
 *    - Đánh dấu FAILED để user tạo QR mới hoặc đổi COD
 * 
 * 3. Tại sao dùng Scheduler?
 *    - Đơn giản hóa demo cho luận văn
 *    - Không cần setup payment gateway thật
 *    - Dễ dàng test flow thanh toán
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QrPaymentScheduler {

    private final OrderRepository orderRepository;

    /**
     * Auto Confirm Payment (chạy mỗi 10 giây)
     * 
     * Logic:
     * - Tìm đơn hàng QR_TRANSFER + PENDING
     * - Đã tạo QR từ 30-60 giây trước
     * - Tự động chuyển sang PAID
     * 
     * Random delay 30-60s để tạo cảm giác tự nhiên
     */
    @Scheduled(fixedRate = 10000)  // Chạy mỗi 10 giây
    @Transactional
    public void autoConfirmPayment() {
        // Tìm đơn hàng cần auto confirm
        List<Order> pendingOrders = orderRepository.findAll().stream()
                .filter(o -> o.getPaymentMethod() == PaymentMethod.QR_TRANSFER)
                .filter(o -> o.getPaymentStatus() == PaymentStatus.PENDING)
                .filter(o -> o.getQrExpiredAt() != null)
                .filter(o -> o.getQrExpiredAt().isAfter(LocalDateTime.now())) // Chưa hết hạn
                .filter(o -> o.getCreatedAt() != null)
                .toList();
        
        for (Order order : pendingOrders) {
            // Random confirm sau 30-60 giây
            LocalDateTime qrCreatedAt = order.getQrExpiredAt().minusMinutes(5); // QR tạo = expired - 5 phút
            long secondsSinceQrCreated = java.time.Duration.between(qrCreatedAt, LocalDateTime.now()).getSeconds();
            
            // Random threshold từ 30-60 giây cho mỗi order
            int randomThreshold = 30 + (int)(order.getId() % 31); // 30-60 based on order ID
            
            if (secondsSinceQrCreated >= randomThreshold) {
                log.info("Auto confirming payment for order {} after {} seconds", 
                        order.getId(), secondsSinceQrCreated);
                
                order.setPaymentStatus(PaymentStatus.PAID);
                order.setPaidAt(LocalDateTime.now());
                orderRepository.save(order);
                
                log.info("Payment auto-confirmed for order {}", order.getId());
            }
        }
    }

    /**
     * Auto Expire QR (chạy mỗi 30 giây)
     * 
     * Logic:
     * - Tìm đơn hàng có QR đã hết hạn
     * - Trạng thái vẫn PENDING
     * - Chuyển sang FAILED
     */
    @Scheduled(fixedRate = 30000)  // Chạy mỗi 30 giây
    @Transactional
    public void autoExpireQr() {
        List<Order> expiredOrders = orderRepository.findAll().stream()
                .filter(o -> o.getPaymentMethod() == PaymentMethod.QR_TRANSFER)
                .filter(o -> o.getPaymentStatus() == PaymentStatus.PENDING)
                .filter(o -> o.getQrExpiredAt() != null)
                .filter(o -> o.getQrExpiredAt().isBefore(LocalDateTime.now())) // Đã hết hạn
                .toList();
        
        for (Order order : expiredOrders) {
            log.info("QR expired for order {}. Marking as FAILED.", order.getId());
            order.setPaymentStatus(PaymentStatus.FAILED);
            orderRepository.save(order);
        }
        
        if (!expiredOrders.isEmpty()) {
            log.info("Expired {} QR payments", expiredOrders.size());
        }
    }
}
