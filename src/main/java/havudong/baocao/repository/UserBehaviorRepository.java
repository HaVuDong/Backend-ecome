package havudong.baocao.repository;

import havudong.baocao.entity.UserBehavior;
import havudong.baocao.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserBehaviorRepository extends JpaRepository<UserBehavior, Long> {
    
    List<UserBehavior> findByUserOrderByTimestampDesc(User user);
    
    List<UserBehavior> findByUserAndTimestampAfter(User user, LocalDateTime since);
    
    // New method for improved recommendation system
    List<UserBehavior> findByUserAndTimestampAfterOrderByTimestampDesc(User user, LocalDateTime since);
    
    // Get all behaviors in a time period for trending analysis
    List<UserBehavior> findByTimestampAfter(LocalDateTime since);
    
    @Query("SELECT ub.product.id, COUNT(ub) as cnt FROM UserBehavior ub " +
           "WHERE ub.timestamp >= :since AND ub.action IN ('VIEW', 'ADD_TO_CART') " +
           "GROUP BY ub.product.id ORDER BY cnt DESC")
    List<Object[]> findTrendingProducts(@Param("since") LocalDateTime since, @Param("limit") int limit);
    
    @Query("SELECT ub.category.id, COUNT(ub) as cnt FROM UserBehavior ub " +
           "WHERE ub.user = :user AND ub.timestamp >= :since " +
           "GROUP BY ub.category.id ORDER BY cnt DESC")
    List<Object[]> findTopCategoriesByUser(@Param("user") User user, @Param("since") LocalDateTime since);
}
