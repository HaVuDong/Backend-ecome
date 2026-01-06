package havudong.baocao.repository;

import havudong.baocao.entity.Category;
import havudong.baocao.entity.Product;
import havudong.baocao.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    Page<Product> findByIsActiveTrue(Pageable pageable);
    
    Page<Product> findByCategoryAndIsActiveTrue(Category category, Pageable pageable);
    
    Page<Product> findBySeller(User seller, Pageable pageable);
    
    List<Product> findBySellerAndIsActiveTrue(User seller);
    
    /**
     * Pessimistic lock để tránh race condition khi update stock
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithLock(@Param("id") Long id);
    
    @Query("SELECT p FROM Product p WHERE p.isActive = true " +
           "AND LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Product> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.isActive = true " +
           "AND p.price BETWEEN :minPrice AND :maxPrice")
    Page<Product> findByPriceRange(@Param("minPrice") BigDecimal minPrice, 
                                    @Param("maxPrice") BigDecimal maxPrice, 
                                    Pageable pageable);
    
    List<Product> findTop10ByIsActiveTrueOrderBySoldCountDesc();
    
    List<Product> findTop10ByIsActiveTrueOrderByCreatedAtDesc();
    
    // Đếm sản phẩm của seller
    long countBySeller(User seller);
    
    // Đếm sản phẩm active của seller
    long countBySellerAndIsActiveTrue(User seller);
    
    // Tìm sản phẩm theo category với filters
    @Query("SELECT p FROM Product p WHERE p.isActive = true " +
           "AND (:category IS NULL OR p.category = :category) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
           "AND (:minRating IS NULL OR p.rating >= :minRating)")
    Page<Product> findWithFilters(
            @Param("category") Category category,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("minRating") Double minRating,
            Pageable pageable);
    
    // Tìm sản phẩm theo keyword với filters
    @Query("SELECT p FROM Product p WHERE p.isActive = true " +
           "AND LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
           "AND (:minRating IS NULL OR p.rating >= :minRating)")
    Page<Product> searchWithFilters(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("minRating") Double minRating,
            Pageable pageable);
    
    // For recommendation system
    Page<Product> findByCategoryOrderBySoldCountDesc(Category category, Pageable pageable);
    
    Page<Product> findTopByOrderBySoldCountDesc(Pageable pageable);
    
    Page<Product> findByCategoryAndPriceBetweenAndIdNotAndIsActiveTrue(
            Category category, 
            double minPrice, 
            double maxPrice, 
            Long excludeId, 
            Pageable pageable);
    
    // New method for improved personalized recommendations
    Page<Product> findByCategoryAndPriceBetweenAndIsActiveTrueAndRatingGreaterThanEqual(
            Category category,
            double minPrice,
            double maxPrice,
            double minRating,
            Pageable pageable);
}
