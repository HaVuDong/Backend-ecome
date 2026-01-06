package havudong.baocao.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Lưu hành vi người dùng để phân tích và gợi ý
 */
@Entity
@Table(name = "user_behaviors")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserBehavior {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false, length = 50)
    private String action; // VIEW, ADD_TO_CART, SEARCH, PURCHASE, WISHLIST
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;
    
    @Column(length = 255)
    private String searchQuery;
    
    @Column(length = 100)
    private String deviceType; // mobile, desktop, tablet
    
    @Column(length = 100)
    private String province; // Tỉnh/thành phố
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }
}
