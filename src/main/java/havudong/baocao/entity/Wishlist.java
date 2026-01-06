package havudong.baocao.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity Wishlist - Danh sách yêu thích của customer
 */
@Entity
@Table(name = "wishlists", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "product_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wishlist {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
}
