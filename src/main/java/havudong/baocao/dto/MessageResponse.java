package havudong.baocao.dto;

import havudong.baocao.entity.Message;
import havudong.baocao.entity.enums.MessageStatus;
import havudong.baocao.entity.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response cho tin nhắn
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponse {
    
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    private Long receiverId;
    private String receiverName;
    private String content;
    private MessageType messageType;
    private String imageUrl;
    private Long productId;
    private ProductInfo product; // Thông tin sản phẩm nếu là tin nhắn chia sẻ sản phẩm
    private MessageStatus status;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
    private boolean isOwn; // True nếu tin nhắn này do current user gửi
    
    /**
     * Thông tin sản phẩm được chia sẻ
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductInfo {
        private Long id;
        private String name;
        private String mainImage;
        private java.math.BigDecimal price;
    }
    
    /**
     * Chuyển đổi từ Entity sang Response
     */
    public static MessageResponse fromEntity(Message message, Long currentUserId) {
        return MessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversation().getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getFullName())
                .senderAvatar(message.getSender().getAvatarUrl())
                .receiverId(message.getReceiver().getId())
                .receiverName(message.getReceiver().getFullName())
                .content(message.getContent())
                .messageType(message.getMessageType())
                .imageUrl(message.getImageUrl())
                .productId(message.getProductId())
                .status(message.getStatus())
                .readAt(message.getReadAt())
                .createdAt(message.getCreatedAt())
                .isOwn(message.getSender().getId().equals(currentUserId))
                .build();
    }
}
