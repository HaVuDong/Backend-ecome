package havudong.baocao.dto;

import havudong.baocao.entity.Conversation;
import havudong.baocao.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response cho cuộc hội thoại
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationResponse {
    
    private Long id;
    
    // Thông tin người còn lại trong cuộc hội thoại
    private Long otherUserId;
    private String otherUserName;
    private String otherUserAvatar;
    private String otherUserRole;
    
    // Tin nhắn cuối
    private String lastMessage;
    private Long lastSenderId;
    private LocalDateTime lastMessageAt;
    
    // Số tin nhắn chưa đọc
    private Integer unreadCount;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * Chuyển đổi từ Entity sang Response
     */
    public static ConversationResponse fromEntity(Conversation conversation, User currentUser) {
        User otherUser = conversation.getOtherUser(currentUser);
        
        return ConversationResponse.builder()
                .id(conversation.getId())
                .otherUserId(otherUser.getId())
                .otherUserName(otherUser.getFullName())
                .otherUserAvatar(otherUser.getAvatarUrl())
                .otherUserRole(otherUser.getRole().name())
                .lastMessage(conversation.getLastMessage())
                .lastSenderId(conversation.getLastSender() != null ? conversation.getLastSender().getId() : null)
                .lastMessageAt(conversation.getLastMessageAt())
                .unreadCount(conversation.getUnreadCount(currentUser))
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }
}
