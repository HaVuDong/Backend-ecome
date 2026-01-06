package havudong.baocao.dto;

import havudong.baocao.entity.enums.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request để gửi tin nhắn mới
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageRequest {
    
    @NotNull(message = "ID người nhận không được để trống")
    private Long receiverId;
    
    @NotBlank(message = "Nội dung tin nhắn không được để trống")
    private String content;
    
    // Loại tin nhắn (mặc định TEXT)
    @Builder.Default
    private MessageType messageType = MessageType.TEXT;
    
    // URL hình ảnh (nếu là tin nhắn hình)
    private String imageUrl;
    
    // ID sản phẩm (nếu chia sẻ sản phẩm)
    private Long productId;
    
    // ID cuộc hội thoại (optional - nếu không có sẽ tạo mới)
    private Long conversationId;
}
