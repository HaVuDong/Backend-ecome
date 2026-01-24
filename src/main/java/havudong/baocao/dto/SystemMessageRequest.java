package havudong.baocao.dto;

import havudong.baocao.entity.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for internal system messages posted by AIbox service
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemMessageRequest {
    private Long receiverId;
    private Long conversationId;
    private Long productId;
    private String content;
    private MessageType messageType = MessageType.TEXT;
}
