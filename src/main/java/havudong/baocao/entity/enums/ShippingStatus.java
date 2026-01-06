package havudong.baocao.entity.enums;

public enum ShippingStatus {
    PENDING,        // Chờ xử lý
    PROCESSING,     // Đang chuẩn bị hàng
    SHIPPED,        // Đã giao cho vận chuyển
    IN_TRANSIT,     // Đang vận chuyển
    DELIVERED,      // Đã giao hàng
    CANCELLED,      // Đã hủy
    RETURNED        // Đã trả hàng
}
