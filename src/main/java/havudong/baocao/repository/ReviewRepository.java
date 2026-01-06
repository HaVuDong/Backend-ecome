package havudong.baocao.repository;

import havudong.baocao.entity.Product;
import havudong.baocao.entity.Review;
import havudong.baocao.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    // Reviews theo product
    Page<Review> findByProduct(Product product, Pageable pageable);
    
    List<Review> findByProduct(Product product);
    
    // Reviews của user
    Page<Review> findByUser(User user, Pageable pageable);
    
    List<Review> findByUser(User user);
    
    // Kiểm tra user đã review product chưa
    Optional<Review> findByUserAndProduct(User user, Product product);
    
    boolean existsByUserAndProduct(User user, Product product);
    
    // Tính rating trung bình của product
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product = :product")
    BigDecimal calculateAverageRating(Product product);
    
    // Đếm số review theo rating
    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.product = :product GROUP BY r.rating")
    List<Object[]> countByRating(Product product);
    
    // Tổng số review của product
    long countByProduct(Product product);
    
    // Thống kê review cho seller (tổng review và rating trung bình)
    @Query("SELECT COUNT(r), AVG(r.rating) FROM Review r WHERE r.product.seller.id = :sellerId")
    List<Object[]> getSellerReviewStats(Long sellerId);
}
