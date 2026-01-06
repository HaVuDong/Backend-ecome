package havudong.baocao.controller;

import havudong.baocao.dto.ApiResponse;
import havudong.baocao.dto.ConversationResponse;
import havudong.baocao.dto.MessageRequest;
import havudong.baocao.dto.MessageResponse;
import havudong.baocao.entity.User;
import havudong.baocao.service.ChatService;
import havudong.baocao.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller xử lý API chat
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {
    
    private final ChatService chatService;
    private final SecurityUtil securityUtil;
    
    /**
     * Lấy danh sách cuộc hội thoại của user hiện tại
     * GET /api/chat/conversations?page=0&size=20
     */
    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<Page<ConversationResponse>>> getConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        User currentUser = securityUtil.getCurrentUser();
        Page<ConversationResponse> conversations = chatService.getConversations(currentUser, page, size);
        
        return ResponseEntity.ok(ApiResponse.success(conversations));
    }
    
    /**
     * Lấy hoặc tạo cuộc hội thoại với user khác
     * POST /api/chat/conversations
     * Body: { "userId": 123 }
     */
    @PostMapping("/conversations")
    public ResponseEntity<ApiResponse<ConversationResponse>> getOrCreateConversation(
            @RequestBody Map<String, Long> request) {
        
        Long otherUserId = request.get("userId");
        if (otherUserId == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("userId là bắt buộc"));
        }
        
        User currentUser = securityUtil.getCurrentUser();
        ConversationResponse conversation = chatService.getOrCreateConversation(currentUser, otherUserId);
        
        return ResponseEntity.ok(ApiResponse.success(conversation));
    }
    
    /**
     * Lấy chi tiết cuộc hội thoại
     * GET /api/chat/conversations/{id}
     */
    @GetMapping("/conversations/{id}")
    public ResponseEntity<ApiResponse<ConversationResponse>> getConversation(@PathVariable Long id) {
        User currentUser = securityUtil.getCurrentUser();
        ConversationResponse conversation = chatService.getConversation(currentUser, id);
        
        return ResponseEntity.ok(ApiResponse.success(conversation));
    }
    
    /**
     * Lấy tin nhắn của cuộc hội thoại
     * GET /api/chat/conversations/{id}/messages?page=0&size=50
     */
    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<ApiResponse<Page<MessageResponse>>> getMessages(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        User currentUser = securityUtil.getCurrentUser();
        Page<MessageResponse> messages = chatService.getMessages(currentUser, id, page, size);
        
        return ResponseEntity.ok(ApiResponse.success(messages));
    }
    
    /**
     * Gửi tin nhắn mới
     * POST /api/chat/messages
     * Body: { "receiverId": 123, "content": "Hello", "messageType": "TEXT" }
     */
    @PostMapping("/messages")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @Valid @RequestBody MessageRequest request) {
        
        User currentUser = securityUtil.getCurrentUser();
        MessageResponse message = chatService.sendMessage(currentUser, request);
        
        return ResponseEntity.ok(ApiResponse.success("Gửi tin nhắn thành công", message));
    }
    
    /**
     * Đánh dấu tin nhắn đã đọc
     * PUT /api/chat/conversations/{id}/read
     */
    @PutMapping("/conversations/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) {
        User currentUser = securityUtil.getCurrentUser();
        chatService.markMessagesAsRead(currentUser, id);
        
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu tin nhắn đã đọc", null));
    }
    
    /**
     * Lấy số tin nhắn chưa đọc
     * GET /api/chat/unread-count
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount() {
        User currentUser = securityUtil.getCurrentUser();
        long unreadMessages = chatService.countUnreadMessages(currentUser);
        long unreadConversations = chatService.countUnreadConversations(currentUser);
        
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "unreadMessages", unreadMessages,
                "unreadConversations", unreadConversations
        )));
    }
}
