package havudong.baocao.repository;

import havudong.baocao.entity.Order;
import havudong.baocao.entity.OrderItem;
import havudong.baocao.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    
    List<OrderItem> findByOrder(Order order);
    
    List<OrderItem> findByProduct(Product product);
}
