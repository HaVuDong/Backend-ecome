package havudong.baocao.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Profile sở thích người dùng - dùng cho ML model
 */
@Entity
@Table(name = "user_preferences")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPreference {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;
    
    // Category preferences (JSON string hoặc separate columns)
    @Column(columnDefinition = "TEXT")
    private String favoriteCategories; // JSON: {"electronics":0.8, "fashion":0.6}
    
    // Price range preferences
    @Column
    private Double avgPriceRange;
    
    @Column
    private Double maxPricePaid;
    
    // Brand preferences
    @Column(columnDefinition = "TEXT")
    private String favoriteSellers; // JSON: {"seller1":10, "seller2":5} - số lần mua
    
    // Shopping time patterns
    @Column(length = 50)
    private String preferredShoppingTime; // morning, afternoon, evening, night
    
    // Device preference
    @Column(length = 50)
    private String preferredDevice; // mobile, desktop
    
    // Location
    @Column(length = 100)
    private String province;
    
    // Engagement score (0-100)
    @Column
    private Integer engagementScore;
    
    // Last updated
    @Column
    private java.time.LocalDateTime lastUpdated;
    
    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = java.time.LocalDateTime.now();
    }
}
