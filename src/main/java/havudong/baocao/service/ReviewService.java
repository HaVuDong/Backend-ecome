package havudong.baocao.service;

import havudong.baocao.dto.ReviewRequest;
import havudong.baocao.dto.ReviewResponse;
import havudong.baocao.entity.Order;
import havudong.baocao.entity.Product;
import havudong.baocao.entity.Review;
import havudong.baocao.entity.User;
import havudong.baocao.entity.enums.ShippingStatus;
import havudong.baocao.exception.BadRequestException;
import havudong.baocao.exception.DuplicateResourceException;
import havudong.baocao.exception.ResourceNotFoundException;
import havudong.baocao.repository.OrderRepository;
import havudong.baocao.repository.ProductRepository;
import havudong.baocao.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {
    
    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ProductService productService;
    
    /**
     * Tạo review mới - validate user đã mua hàng và chưa review
     */
    @Transactional
    public ReviewResponse createReview(User user, ReviewRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));
        
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", request.getOrderId()));
        
        // Validate: Order phải thuộc về user này
        if (!order.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Đơn hàng không thuộc về bạn");
        }
        
        // Validate: Order phải đã giao hàng thành công
        if (order.getShippingStatus() != ShippingStatus.DELIVERED) {
            throw new BadRequestException("Chỉ có thể đánh giá sản phẩm sau khi nhận hàng thành công");
        }
        
        // Validate: Product phải nằm trong order
        boolean productInOrder = order.getOrderItems().stream()
                .anyMatch(item -> item.getProduct().getId().equals(product.getId()));
        if (!productInOrder) {
            throw new BadRequestException("Sản phẩm không nằm trong đơn hàng này");
        }
        
        // Validate: User chưa review product này
        if (reviewRepository.existsByUserAndProduct(user, product)) {
            throw new DuplicateResourceException("Review", "productId", request.getProductId());
        }
        
        // Tạo review
        Review review = new Review();
        review.setUser(user);
        review.setProduct(product);
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setImageUrls(request.getImageUrls());
        
        Review saved = reviewRepository.save(review);
        log.info("User {} created review for product {}, rating: {}", 
                user.getId(), product.getId(), request.getRating());
        
        // Cập nhật rating trung bình của product
        updateProductRating(product);
        
        return toResponse(saved);
    }
    
    /**
     * Lấy reviews của product
     */
    public Page<ReviewResponse> getProductReviews(Long productId, Pageable pageable) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        
        return reviewRepository.findByProduct(product, pageable)
                .map(this::toResponse);
    }
    
    /**
     * Lấy thống kê rating của product
     */
    public Map<String, Object> getProductRatingStats(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        
        BigDecimal avgRating = reviewRepository.calculateAverageRating(product);
        long totalReviews = reviewRepository.countByProduct(product);
        List<Object[]> ratingCounts = reviewRepository.countByRating(product);
        
        Map<Integer, Long> ratingDistribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            ratingDistribution.put(i, 0L);
        }
        for (Object[] row : ratingCounts) {
            ratingDistribution.put((Integer) row[0], (Long) row[1]);
        }
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("averageRating", avgRating != null ? avgRating.setScale(1, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        stats.put("totalReviews", totalReviews);
        stats.put("ratingDistribution", ratingDistribution);
        
        return stats;
    }
    
    /**
     * Lấy reviews của user
     */
    public Page<ReviewResponse> getUserReviews(User user, Pageable pageable) {
        return reviewRepository.findByUser(user, pageable)
                .map(this::toResponse);
    }
    
    /**
     * Kiểm tra user có thể review sản phẩm không
     * - User phải là chủ order
     * - Order phải đã giao hàng
     * - Product phải trong order
     * - User chưa review product này
     */
    public boolean canUserReviewProduct(User user, Long productId, Long orderId) {
        try {
            Product product = productRepository.findById(productId).orElse(null);
            Order order = orderRepository.findById(orderId).orElse(null);
            
            if (product == null || order == null) {
                return false;
            }
            
            // Order phải thuộc về user
            if (!order.getUser().getId().equals(user.getId())) {
                return false;
            }
            
            // Order phải đã giao hàng
            if (order.getShippingStatus() != ShippingStatus.DELIVERED) {
                return false;
            }
            
            // Product phải trong order
            boolean productInOrder = order.getOrderItems().stream()
                    .anyMatch(item -> item.getProduct().getId().equals(product.getId()));
            if (!productInOrder) {
                return false;
            }
            
            // User chưa review product này
            return !reviewRepository.existsByUserAndProduct(user, product);
        } catch (Exception e) {
            log.error("Error checking can review: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Xóa review
     */
    @Transactional
    public void deleteReview(Long reviewId, User user) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));
        
        if (!review.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Bạn không có quyền xóa đánh giá này");
        }
        
        Product product = review.getProduct();
        reviewRepository.delete(review);
        
        log.info("User {} deleted review {}", user.getId(), reviewId);
        
        // Cập nhật lại rating của product
        updateProductRating(product);
    }
    
    /**
     * Cập nhật rating trung bình của product
     */
    private void updateProductRating(Product product) {
        BigDecimal avgRating = reviewRepository.calculateAverageRating(product);
        if (avgRating == null) {
            avgRating = BigDecimal.ZERO;
        }
        avgRating = avgRating.setScale(1, RoundingMode.HALF_UP);
        
        productService.updateProductRating(product.getId(), avgRating);
    }
    
    private ReviewResponse toResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProduct().getId())
                .productName(review.getProduct().getName())
                .user(ReviewResponse.UserInfo.builder()
                        .id(review.getUser().getId())
                        .fullName(review.getUser().getFullName())
                        .avatarUrl(review.getUser().getAvatarUrl())
                        .build())
                .rating(review.getRating())
                .comment(review.getComment())
                .imageUrls(review.getImageUrls())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
