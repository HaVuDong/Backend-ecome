package havudong.baocao.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity đại diện cho cuộc hội thoại giữa 2 người dùng
 */
@Entity
@Table(name = "conversations", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user1_id", "user2_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // User 1 (thường là customer)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user1_id", nullable = false)
    private User user1;
    
    // User 2 (thường là seller)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user2_id", nullable = false)
    private User user2;
    
    // Tin nhắn cuối cùng (để hiển thị preview)
    @Column(columnDefinition = "TEXT")
    private String lastMessage;
    
    // Người gửi tin nhắn cuối
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_sender_id")
    private User lastSender;
    
    // Thời gian tin nhắn cuối
    private LocalDateTime lastMessageAt;
    
    // Số tin nhắn chưa đọc của user1
    @Column(nullable = false)
    @Builder.Default
    private Integer unreadCountUser1 = 0;
    
    // Số tin nhắn chưa đọc của user2
    @Column(nullable = false)
    @Builder.Default
    private Integer unreadCountUser2 = 0;
    
    // Danh sách tin nhắn
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<Message> messages = new ArrayList<>();
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    /**
     * Lấy người còn lại trong cuộc hội thoại
     */
    public User getOtherUser(User currentUser) {
        if (user1.getId().equals(currentUser.getId())) {
            return user2;
        }
        return user1;
    }
    
    /**
     * Lấy số tin nhắn chưa đọc cho user
     */
    public Integer getUnreadCount(User user) {
        if (user1.getId().equals(user.getId())) {
            return unreadCountUser1;
        }
        return unreadCountUser2;
    }
    
    /**
     * Tăng số tin nhắn chưa đọc cho người nhận
     */
    public void incrementUnreadCount(User receiver) {
        if (user1.getId().equals(receiver.getId())) {
            unreadCountUser1++;
        } else {
            unreadCountUser2++;
        }
    }
    
    /**
     * Reset số tin nhắn chưa đọc khi user đọc
     */
    public void resetUnreadCount(User user) {
        if (user1.getId().equals(user.getId())) {
            unreadCountUser1 = 0;
        } else {
            unreadCountUser2 = 0;
        }
    }
}
