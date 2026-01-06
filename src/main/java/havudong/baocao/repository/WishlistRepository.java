package havudong.baocao.repository;

import havudong.baocao.entity.Product;
import havudong.baocao.entity.User;
import havudong.baocao.entity.Wishlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    
    // Lấy wishlist của user
    Page<Wishlist> findByUser(User user, Pageable pageable);
    
    List<Wishlist> findByUser(User user);
    
    // Kiểm tra product đã có trong wishlist chưa
    Optional<Wishlist> findByUserAndProduct(User user, Product product);
    
    boolean existsByUserAndProduct(User user, Product product);
    
    // Xóa theo user và product
    void deleteByUserAndProduct(User user, Product product);
    
    // Đếm số sản phẩm trong wishlist của user
    long countByUser(User user);
    
    // Đếm số người yêu thích một product
    long countByProduct(Product product);
    
    // Lấy danh sách product IDs trong wishlist của user
    @Query("SELECT w.product.id FROM Wishlist w WHERE w.user = :user")
    List<Long> findProductIdsByUser(User user);
}
