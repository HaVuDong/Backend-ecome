package havudong.baocao.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import havudong.baocao.entity.enums.PaymentMethod;
import havudong.baocao.entity.enums.PaymentStatus;
import havudong.baocao.entity.enums.ShippingStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Order extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Customer đặt hàng
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"orders", "products", "password", "hibernateLazyInitializer"})
    private User user;
    
    // Seller của đơn hàng (mỗi order chỉ thuộc về 1 seller)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    @JsonIgnoreProperties({"orders", "products", "password", "hibernateLazyInitializer"})
    private User seller;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"order", "hibernateLazyInitializer"})
    private List<OrderItem> orderItems = new ArrayList<>();
    
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;
    
    @Column(name = "shipping_fee", precision = 10, scale = 2)
    private BigDecimal shippingFee = BigDecimal.ZERO;
    
    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;
    
    // Phí platform (commission) - mặc định 5%
    @Column(name = "platform_fee", precision = 10, scale = 2)
    private BigDecimal platformFee = BigDecimal.ZERO;
    
    // Số tiền seller nhận được sau khi trừ phí
    @Column(name = "seller_amount", precision = 10, scale = 2)
    private BigDecimal sellerAmount = BigDecimal.ZERO;
    
    @Column(name = "final_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal finalAmount;
    
    // Trạng thái thanh toán
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;
    
    // Trạng thái vận chuyển
    @Enumerated(EnumType.STRING)
    @Column(name = "shipping_status", nullable = false)
    private ShippingStatus shippingStatus = ShippingStatus.PENDING;
    
    @Column(name = "shipping_address", columnDefinition = "TEXT")
    private String shippingAddress;
    
    @Column(name = "shipping_phone", length = 20)
    private String shippingPhone;
    
    @Column(name = "shipping_name")
    private String shippingName;
    
    @Column(name = "payment_method", length = 50)
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod = PaymentMethod.COD;
    
    // ============ QR Payment Fields ============
    // URL của mã QR thanh toán (VietQR)
    @Column(name = "qr_code_url", columnDefinition = "TEXT")
    private String qrCodeUrl;
    
    // Thời điểm QR hết hạn (5 phút sau khi tạo)
    @Column(name = "qr_expired_at")
    private LocalDateTime qrExpiredAt;
    
    // Mã giao dịch để đối soát (nội dung chuyển khoản)
    @Column(name = "payment_transaction_id", length = 100)
    private String paymentTransactionId;
    
    // Thời điểm thanh toán thành công
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(columnDefinition = "TEXT")
    private String note;
}