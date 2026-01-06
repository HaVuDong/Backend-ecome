package havudong.baocao.service;

import havudong.baocao.constant.RecommendationConstants;
import havudong.baocao.dto.ProductResponse;
import havudong.baocao.entity.*;
import havudong.baocao.mapper.ProductMapper;
import havudong.baocao.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static havudong.baocao.constant.RecommendationConstants.*;

/**
 * Service xử lý gợi ý sản phẩm thông minh
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {
    
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final WishlistRepository wishlistRepository;
    private final UserBehaviorRepository userBehaviorRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final ProductMapper productMapper;
    
    /**
     * Gợi ý cá nhân hóa dựa trên hành vi người dùng
     * 
     * Thuật toán: Content-based Filtering + Weighted Scoring
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getPersonalizedRecommendations(User user, int limit) {
        log.info("Generating recommendations for user: {}", user.getId());
        
        // Bước 1: Tính điểm yêu thích category từ hành vi
        Map<Long, Double> categoryScores = calculateCategoryScores(user);
        
        // Bước 2: Lấy top categories (dùng constant)
        List<Long> topCategoryIds = categoryScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(TOP_CATEGORIES_FOR_RECOMMENDATION)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        // Edge case: user mới chưa có behavior
        if (topCategoryIds.isEmpty()) {
            log.debug("User {} has no behavior, returning trending products", user.getId());
            return getTrendingProducts(limit, TRENDING_DAYS);
        }
        
        log.debug("Top categories for user {}: {}", user.getId(), topCategoryIds);
        
        // Bước 3: Lấy sản phẩm từ top categories
        Set<Product> recommendations = new HashSet<>();
        
        UserPreference preference = userPreferenceRepository
                .findByUser(user)
                .orElse(null);
        
        Double avgPrice = (preference != null && preference.getAvgPriceRange() != null) 
                ? preference.getAvgPriceRange() 
                : null;
        
        for (Long categoryId : topCategoryIds) {
            Category category = new Category();
            category.setId(categoryId);
            
            List<Product> products;
            
            if (avgPrice != null) {
                // Lọc theo giá (dùng constant PRICE_RANGE_FACTOR)
                double minPrice = avgPrice * (1 - PRICE_RANGE_FACTOR);
                double maxPrice = avgPrice * (1 + PRICE_RANGE_FACTOR);
                
                products = productRepository
                        .findByCategoryAndPriceBetweenAndIsActiveTrueAndRatingGreaterThanEqual(
                            category, minPrice, maxPrice, MIN_RATING_FOR_RECOMMENDATION, 
                            PageRequest.of(0, 10)
                        ).getContent();
            } else {
                products = productRepository.findByCategoryOrderBySoldCountDesc(
                        category, PageRequest.of(0, 10)
                ).getContent();
            }
            
            recommendations.addAll(products);
            if (recommendations.size() >= limit) break;
        }
        
        // Bước 4: Loại bỏ sản phẩm đã mua gần đây
        LocalDateTime since = LocalDateTime.now().minusDays(BEHAVIOR_ANALYSIS_DAYS);
        Set<Long> recentPurchasedIds = getRecentPurchasedProductIds(user, since);
        
        List<ProductResponse> result = recommendations.stream()
                .filter(p -> !recentPurchasedIds.contains(p.getId()))
                .filter(Product::getIsActive)
                .limit(limit)
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
        
        // Bước 5: Bổ sung trending nếu chưa đủ
        if (result.size() < limit) {
            List<ProductResponse> trending = getTrendingProducts(limit - result.size(), TRENDING_DAYS);
            result.addAll(trending);
        }
        
        log.info("Generated {} recommendations for user {}", result.size(), user.getId());
        return result;
    }
    
    /**
     * Sản phẩm tương tự dựa trên category và price range
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getSimilarProducts(Long productId, int limit) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        // Tìm trong cùng category, price range ±30%
        double productPrice = product.getPrice().doubleValue();
        double minPrice = productPrice * 0.7;
        double maxPrice = productPrice * 1.3;
        
        List<Product> similar = productRepository.findByCategoryAndPriceBetweenAndIdNotAndIsActiveTrue(
                product.getCategory(), 
                minPrice, 
                maxPrice, 
                productId,
                PageRequest.of(0, limit)
        ).getContent();
        
        return similar.stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Sản phẩm thường được mua cùng (Collaborative filtering đơn giản)
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getFrequentlyBoughtTogether(Long productId, int limit) {
        // Tìm các đơn hàng có chứa productId
        List<Order> ordersWithProduct = orderRepository.findOrdersContainingProduct(productId);
        
        // Đếm frequency của các sản phẩm khác trong những đơn hàng đó
        Map<Long, Long> productFrequency = new HashMap<>();
        
        ordersWithProduct.forEach(order -> 
            order.getOrderItems().forEach(item -> {
                Long pid = item.getProduct().getId();
                if (!pid.equals(productId)) {
                    productFrequency.merge(pid, 1L, (existing, newVal) -> existing + newVal);
                }
            })
        );
        
        // Sắp xếp theo frequency và lấy top N
        List<Long> topProductIds = productFrequency.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        return productRepository.findAllById(topProductIds).stream()
                .filter(Product::getIsActive)
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Sản phẩm trending (nhiều tương tác gần đây)
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getTrendingProducts(int limit, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        
        List<UserBehavior> behaviors = userBehaviorRepository.findByTimestampAfter(since);
        
        // Tính trending score dùng ACTION_WEIGHTS chung
        Map<Long, Integer> trendingScores = new HashMap<>();
        
        for (UserBehavior behavior : behaviors) {
            if (behavior.getProduct() == null) continue;
            
            Long productId = behavior.getProduct().getId();
            int weight = getWeight(behavior.getAction());
            trendingScores.merge(productId, weight, Integer::sum);
        }
        
        // Lấy top N
        List<Long> topProductIds = trendingScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        if (topProductIds.isEmpty()) {
            // Fallback: lấy sản phẩm bán chạy nhất
            return productRepository.findTopByOrderBySoldCountDesc(PageRequest.of(0, limit))
                    .getContent().stream()
                    .map(productMapper::toResponse)
                    .collect(Collectors.toList());
        }
        
        return productRepository.findAllById(topProductIds).stream()
                .filter(Product::getIsActive)
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Sản phẩm phổ biến theo khu vực
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getPopularInArea(String province, int limit) {
        // Lấy các đơn hàng từ province này
        List<Order> ordersInArea = orderRepository.findByShippingAddressContaining(
                province, PageRequest.of(0, 100)
        ).getContent();
        
        // Đếm frequency
        Map<Long, Long> productFrequency = new HashMap<>();
        ordersInArea.forEach(order ->
            order.getOrderItems().forEach(item -> {
                Long pid = item.getProduct().getId();
                Long qty = Long.valueOf(item.getQuantity());
                productFrequency.merge(pid, qty, (existing, newVal) -> existing + newVal);
            })
        );
        
        List<Long> topProductIds = productFrequency.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        return productRepository.findAllById(topProductIds).stream()
                .filter(Product::getIsActive)
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Lưu hành vi người dùng
     */
    @Transactional
    public void trackUserBehavior(User user, String action, Long productId, Long categoryId, String searchQuery) {
        UserBehavior behavior = new UserBehavior();
        behavior.setUser(user);
        behavior.setAction(action);
        
        if (productId != null) {
            productRepository.findById(productId).ifPresent(behavior::setProduct);
        }
        
        behavior.setSearchQuery(searchQuery);
        behavior.setProvince(extractProvince(user.getAddress()));
        behavior.setTimestamp(LocalDateTime.now());
        
        userBehaviorRepository.save(behavior);
        log.debug("Tracked behavior: user={}, action={}, productId={}", user.getId(), action, productId);
    }
    
    /**
     * Tính điểm yêu thích category dựa trên hành vi
     */
    private Map<Long, Double> calculateCategoryScores(User user) {
        LocalDateTime since = LocalDateTime.now().minusDays(BEHAVIOR_ANALYSIS_DAYS);
        List<UserBehavior> behaviors = userBehaviorRepository
                .findByUserAndTimestampAfterOrderByTimestampDesc(user, since);
        
        Map<Long, Double> scores = new HashMap<>();
        
        for (UserBehavior behavior : behaviors) {
            if (behavior.getProduct() == null) continue;
            
            Long categoryId = behavior.getProduct().getCategory().getId();
            int weight = getWeight(behavior.getAction());
            scores.merge(categoryId, (double) weight, Double::sum);
        }
        
        return scores;
    }
    
    /**
     * Lấy IDs của sản phẩm đã mua gần đây
     */
    private Set<Long> getRecentPurchasedProductIds(User user, LocalDateTime since) {
        return orderRepository.findByUserAndCreatedAtAfter(user, since).stream()
                .flatMap(order -> order.getOrderItems().stream())
                .map(item -> item.getProduct().getId())
                .collect(Collectors.toSet());
    }
    
    private String extractProvince(String address) {
        if (address == null) return "Unknown";
        String[] parts = address.split(",");
        return parts.length > 0 ? parts[parts.length - 1].trim() : "Unknown";
    }
}
