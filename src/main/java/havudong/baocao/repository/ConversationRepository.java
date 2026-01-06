package havudong.baocao.repository;

import havudong.baocao.entity.Conversation;
import havudong.baocao.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    
    /**
     * Tìm cuộc hội thoại giữa 2 user (không phân biệt thứ tự)
     */
    @Query("SELECT c FROM Conversation c WHERE " +
           "(c.user1 = :user1 AND c.user2 = :user2) OR " +
           "(c.user1 = :user2 AND c.user2 = :user1)")
    Optional<Conversation> findByUsers(User user1, User user2);
    
    /**
     * Lấy danh sách cuộc hội thoại của user (sắp xếp theo tin nhắn mới nhất)
     */
    @Query("SELECT c FROM Conversation c WHERE c.user1 = :user OR c.user2 = :user " +
           "ORDER BY c.lastMessageAt DESC NULLS LAST")
    Page<Conversation> findByUser(User user, Pageable pageable);
    
    /**
     * Đếm số cuộc hội thoại có tin nhắn chưa đọc của user
     */
    @Query("SELECT COUNT(c) FROM Conversation c WHERE " +
           "(c.user1 = :user AND c.unreadCountUser1 > 0) OR " +
           "(c.user2 = :user AND c.unreadCountUser2 > 0)")
    long countUnreadConversations(User user);
    
    /**
     * Tổng số tin nhắn chưa đọc của user
     */
    @Query("SELECT COALESCE(SUM(CASE WHEN c.user1 = :user THEN c.unreadCountUser1 " +
           "ELSE c.unreadCountUser2 END), 0) FROM Conversation c " +
           "WHERE c.user1 = :user OR c.user2 = :user")
    long countTotalUnreadMessages(User user);
    
    /**
     * Reset unread count khi user đọc tin nhắn
     */
    @Modifying
    @Query("UPDATE Conversation c SET c.unreadCountUser1 = 0 " +
           "WHERE c.id = :conversationId AND c.user1.id = :userId")
    void resetUnreadCountUser1(Long conversationId, Long userId);
    
    @Modifying
    @Query("UPDATE Conversation c SET c.unreadCountUser2 = 0 " +
           "WHERE c.id = :conversationId AND c.user2.id = :userId")
    void resetUnreadCountUser2(Long conversationId, Long userId);
}
