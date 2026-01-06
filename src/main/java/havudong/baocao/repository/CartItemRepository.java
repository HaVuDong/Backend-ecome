package havudong.baocao.repository;

import havudong.baocao.entity.CartItem;
import havudong.baocao.entity.Product;
import havudong.baocao.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    
    List<CartItem> findByUser(User user);
    
    List<CartItem> findByUserAndSelectedTrue(User user);
    
    Optional<CartItem> findByUserAndProduct(User user, Product product);
    
    void deleteByUser(User user);
    
    void deleteByUserAndProduct(User user, Product product);
    
    @Query("SELECT SUM(c.price * c.quantity) FROM CartItem c " +
           "WHERE c.user = :user AND c.selected = true")
    BigDecimal calculateTotalAmount(User user);
}
