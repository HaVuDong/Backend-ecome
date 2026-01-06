package havudong.baocao.entity.enums;

/**
 * Phương thức thanh toán
 * 
 * GIẢI THÍCH KHI BẢO VỆ:
 * - COD: Thanh toán khi nhận hàng (Cash On Delivery)
 * - QR_TRANSFER: Thanh toán chuyển khoản qua mã QR
 * 
 * Trong triển khai thực tế, có thể mở rộng thêm:
 * - MOMO, ZALOPAY, VNPAY... (cổng thanh toán)
 * - CREDIT_CARD (thẻ tín dụng)
 */
public enum PaymentMethod {
    COD,            // Thanh toán khi nhận hàng
    QR_TRANSFER     // Chuyển khoản qua QR
}
