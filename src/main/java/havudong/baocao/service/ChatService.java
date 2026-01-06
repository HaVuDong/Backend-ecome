package havudong.baocao.service;

import havudong.baocao.dto.ConversationResponse;
import havudong.baocao.dto.MessageRequest;
import havudong.baocao.dto.MessageResponse;
import havudong.baocao.entity.Conversation;
import havudong.baocao.entity.Message;

import havudong.baocao.entity.User;
import havudong.baocao.entity.enums.MessageStatus;
import havudong.baocao.entity.enums.MessageType;
import havudong.baocao.exception.ResourceNotFoundException;
import havudong.baocao.repository.ConversationRepository;
import havudong.baocao.repository.MessageRepository;
import havudong.baocao.repository.ProductRepository;
import havudong.baocao.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;


/**
 * Service xử lý chat
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {
    
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    
    /**
     * Lấy hoặc tạo cuộc hội thoại với user khác
     */
    @Transactional
    public ConversationResponse getOrCreateConversation(User currentUser, Long otherUserId) {
        User otherUser = userRepository.findById(otherUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        
        // Không thể chat với chính mình
        if (currentUser.getId().equals(otherUserId)) {
            throw new IllegalArgumentException("Không thể tạo cuộc hội thoại với chính mình");
        }
        
        // Tìm cuộc hội thoại đã có
        Conversation conversation = conversationRepository.findByUsers(currentUser, otherUser)
                .orElseGet(() -> {
                    // Tạo mới nếu chưa có
                    Conversation newConversation = Conversation.builder()
                            .user1(currentUser)
                            .user2(otherUser)
                            .build();
                    return conversationRepository.save(newConversation);
                });
        
        return ConversationResponse.fromEntity(conversation, currentUser);
    }
    
    /**
     * Lấy danh sách cuộc hội thoại của user
     */
    @Transactional(readOnly = true)
    public Page<ConversationResponse> getConversations(User currentUser, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Conversation> conversations = conversationRepository.findByUser(currentUser, pageable);
        
        return conversations.map(c -> ConversationResponse.fromEntity(c, currentUser));
    }
    
    /**
     * Lấy chi tiết cuộc hội thoại
     */
    @Transactional(readOnly = true)
    public ConversationResponse getConversation(User currentUser, Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cuộc hội thoại"));
        
        // Kiểm tra quyền truy cập
        if (!conversation.getUser1().getId().equals(currentUser.getId()) && 
            !conversation.getUser2().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Bạn không có quyền truy cập cuộc hội thoại này");
        }
        
        return ConversationResponse.fromEntity(conversation, currentUser);
    }
    
    /**
     * Gửi tin nhắn
     */
    @Transactional
    public MessageResponse sendMessage(User sender, MessageRequest request) {
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người nhận"));
        
        // Không thể gửi tin nhắn cho chính mình
        if (sender.getId().equals(receiver.getId())) {
            throw new IllegalArgumentException("Không thể gửi tin nhắn cho chính mình");
        }
        
        // Lấy hoặc tạo conversation
        Conversation conversation;
        if (request.getConversationId() != null) {
            conversation = conversationRepository.findById(request.getConversationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cuộc hội thoại"));
        } else {
            conversation = conversationRepository.findByUsers(sender, receiver)
                    .orElseGet(() -> {
                        Conversation newConversation = Conversation.builder()
                                .user1(sender)
                                .user2(receiver)
                                .build();
                        return conversationRepository.save(newConversation);
                    });
        }
        
        // Tạo tin nhắn
        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .receiver(receiver)
                .content(request.getContent())
                .messageType(request.getMessageType() != null ? request.getMessageType() : MessageType.TEXT)
                .imageUrl(request.getImageUrl())
                .productId(request.getProductId())
                .status(MessageStatus.SENT)
                .build();
        
        message = messageRepository.save(message);
        
        // Cập nhật conversation
        conversation.setLastMessage(truncateMessage(request.getContent()));
        conversation.setLastSender(sender);
        conversation.setLastMessageAt(LocalDateTime.now());
        conversation.incrementUnreadCount(receiver);
        conversationRepository.save(conversation);
        
        log.info("Message sent from {} to {} in conversation {}", sender.getId(), receiver.getId(), conversation.getId());
        
        MessageResponse response = MessageResponse.fromEntity(message, sender.getId());
        
        // Thêm thông tin sản phẩm nếu có
        if (request.getMessageType() == MessageType.PRODUCT && request.getProductId() != null) {
            productRepository.findById(request.getProductId()).ifPresent(product -> {
                response.setProduct(MessageResponse.ProductInfo.builder()
                        .id(product.getId())
                        .name(product.getName())
                        .mainImage(product.getMainImage())
                        .price(product.getPrice())
                        .build());
            });
        }
        
        return response;
    }
    
    /**
     * Lấy tin nhắn của cuộc hội thoại
     */
    @Transactional
    public Page<MessageResponse> getMessages(User currentUser, Long conversationId, int page, int size) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cuộc hội thoại"));
        
        // Kiểm tra quyền truy cập
        if (!conversation.getUser1().getId().equals(currentUser.getId()) && 
            !conversation.getUser2().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Bạn không có quyền truy cập cuộc hội thoại này");
        }
        
        // Đánh dấu tin nhắn đã đọc
        markMessagesAsRead(currentUser, conversation);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Message> messages = messageRepository.findByConversationOrderByCreatedAtDesc(conversation, pageable);
        
        return messages.map(m -> {
            MessageResponse response = MessageResponse.fromEntity(m, currentUser.getId());
            
            // Thêm thông tin sản phẩm nếu có
            if (m.getMessageType() == MessageType.PRODUCT && m.getProductId() != null) {
                productRepository.findById(m.getProductId()).ifPresent(product -> {
                    response.setProduct(MessageResponse.ProductInfo.builder()
                            .id(product.getId())
                            .name(product.getName())
                            .mainImage(product.getMainImage())
                            .price(product.getPrice())
                            .build());
                });
            }
            
            return response;
        });
    }
    
    /**
     * Đánh dấu tin nhắn đã đọc
     */
    @Transactional
    public void markMessagesAsRead(User currentUser, Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cuộc hội thoại"));
        
        markMessagesAsRead(currentUser, conversation);
    }
    
    private void markMessagesAsRead(User currentUser, Conversation conversation) {
        // Đánh dấu tin nhắn đã đọc
        int updated = messageRepository.markAllAsRead(conversation, currentUser);
        
        if (updated > 0) {
            // Reset unread count
            conversation.resetUnreadCount(currentUser);
            conversationRepository.save(conversation);
            log.info("Marked {} messages as read for user {} in conversation {}", updated, currentUser.getId(), conversation.getId());
        }
    }
    
    /**
     * Đếm số tin nhắn chưa đọc
     */
    @Transactional(readOnly = true)
    public long countUnreadMessages(User currentUser) {
        return conversationRepository.countTotalUnreadMessages(currentUser);
    }
    
    /**
     * Đếm số cuộc hội thoại có tin nhắn chưa đọc
     */
    @Transactional(readOnly = true)
    public long countUnreadConversations(User currentUser) {
        return conversationRepository.countUnreadConversations(currentUser);
    }
    
    /**
     * Truncate message for preview
     */
    private String truncateMessage(String message) {
        if (message == null) return null;
        if (message.length() <= 100) return message;
        return message.substring(0, 97) + "...";
    }
}
