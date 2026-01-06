package havudong.baocao.entity;

import havudong.baocao.entity.enums.MessageStatus;
import havudong.baocao.entity.enums.MessageType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity đại diện cho một tin nhắn
 */
@Entity
@Table(name = "messages", indexes = {
    @Index(name = "idx_message_conversation", columnList = "conversation_id"),
    @Index(name = "idx_message_sender", columnList = "sender_id"),
    @Index(name = "idx_message_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Cuộc hội thoại chứa tin nhắn
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;
    
    // Người gửi
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;
    
    // Người nhận
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;
    
    // Nội dung tin nhắn
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    
    // Loại tin nhắn (TEXT, IMAGE, PRODUCT)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MessageType messageType = MessageType.TEXT;
    
    // URL hình ảnh (nếu là tin nhắn hình)
    private String imageUrl;
    
    // ID sản phẩm (nếu là tin nhắn chia sẻ sản phẩm)
    private Long productId;
    
    // Trạng thái tin nhắn
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MessageStatus status = MessageStatus.SENT;
    
    // Thời gian đã đọc
    private LocalDateTime readAt;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    /**
     * Đánh dấu tin nhắn đã đọc
     */
    public void markAsRead() {
        if (this.status != MessageStatus.READ) {
            this.status = MessageStatus.READ;
            this.readAt = LocalDateTime.now();
        }
    }
}
