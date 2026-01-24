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
    private final havudong.baocao.repository.UserRepository userRepository;
    private final org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder passwordEncoder;

    @org.springframework.beans.factory.annotation.Value("${aibox.service-key:}")
    private String aiboxServiceKey;
    
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

    /**
     * Endpoint nội bộ cho AIbox gửi assistant message.
     * Header required: X-AIBOX-KEY
     */
    @PostMapping("/system/messages")
    public ResponseEntity<ApiResponse<MessageResponse>> sendSystemMessage(
            @RequestHeader(value = "X-AIBOX-KEY", required = false) String key,
            @Valid @RequestBody havudong.baocao.dto.SystemMessageRequest request
    ) {
        // verify key
        if (aiboxServiceKey == null || aiboxServiceKey.isBlank() || !aiboxServiceKey.equals(key)) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Invalid service key"));
        }

        // Ensure AI user exists (create on demand)
        String aiEmail = "ai@ecome.local";
        User aiUser = userRepository.findByEmail(aiEmail).orElseGet(() -> {
            User u = new User();
            u.setEmail(aiEmail);
            u.setPasswordHash(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
            u.setFullName("AI Assistant");
            u.setRole(havudong.baocao.entity.enums.UserRole.SELLER);
            u.setStatus(havudong.baocao.entity.enums.UserStatus.ACTIVE);
            return userRepository.save(u);
        });

        // Build MessageRequest and use chatService to save
        MessageRequest mr = new MessageRequest();
        mr.setReceiverId(request.getReceiverId());
        mr.setContent(request.getContent());
        mr.setMessageType(request.getMessageType());
        mr.setConversationId(request.getConversationId());
        mr.setProductId(request.getProductId());

        MessageResponse resp = chatService.sendMessage(aiUser, mr);
        return ResponseEntity.ok(ApiResponse.success("Assistant message created", resp));
    }
}
