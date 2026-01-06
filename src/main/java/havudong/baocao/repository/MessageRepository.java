package havudong.baocao.repository;

import havudong.baocao.entity.Conversation;
import havudong.baocao.entity.Message;
import havudong.baocao.entity.User;
import havudong.baocao.entity.enums.MessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    /**
     * Lấy tin nhắn của cuộc hội thoại (phân trang, mới nhất trước)
     */
    Page<Message> findByConversationOrderByCreatedAtDesc(Conversation conversation, Pageable pageable);
    
    /**
     * Lấy tin nhắn của cuộc hội thoại (sắp xếp theo thời gian tăng dần)
     */
    List<Message> findByConversationOrderByCreatedAtAsc(Conversation conversation);
    
    /**
     * Đếm số tin nhắn chưa đọc trong cuộc hội thoại cho receiver
     */
    long countByConversationAndReceiverAndStatus(Conversation conversation, User receiver, MessageStatus status);
    
    /**
     * Đánh dấu tất cả tin nhắn chưa đọc là đã đọc
     */
    @Modifying
    @Query("UPDATE Message m SET m.status = 'READ', m.readAt = CURRENT_TIMESTAMP " +
           "WHERE m.conversation = :conversation AND m.receiver = :receiver AND m.status != 'READ'")
    int markAllAsRead(Conversation conversation, User receiver);
    
    /**
     * Lấy tin nhắn mới nhất của cuộc hội thoại
     */
    @Query("SELECT m FROM Message m WHERE m.conversation = :conversation ORDER BY m.createdAt DESC LIMIT 1")
    Message findLatestMessage(Conversation conversation);
    
    /**
     * Đếm tổng tin nhắn chưa đọc của user
     */
    long countByReceiverAndStatus(User receiver, MessageStatus status);
}
