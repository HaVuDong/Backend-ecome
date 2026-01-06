package havudong.baocao.controller;

import havudong.baocao.dto.ApiResponse;
import havudong.baocao.dto.ProductResponse;
import havudong.baocao.entity.User;
import havudong.baocao.service.RecommendationService;
import havudong.baocao.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API Gợi ý sản phẩm dựa trên hành vi người dùng
 */
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Slf4j
public class RecommendationController {
    
    private final RecommendationService recommendationService;
    private final SecurityUtil securityUtil;
    
    /**
     * Lấy sản phẩm gợi ý dựa trên lịch sử mua hàng và wishlist
     */
    @GetMapping("/for-you")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getPersonalizedRecommendations(
            @RequestParam(defaultValue = "20") int limit
    ) {
        User currentUser = securityUtil.getCurrentUser();
        List<ProductResponse> recommendations = recommendationService.getPersonalizedRecommendations(currentUser, limit);
        return ResponseEntity.ok(ApiResponse.success("Gợi ý sản phẩm cho bạn", recommendations));
    }
    
    /**
     * Sản phẩm tương tự - dựa trên category và giá
     */
    @GetMapping("/similar/{productId}")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getSimilarProducts(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<ProductResponse> similar = recommendationService.getSimilarProducts(productId, limit);
        return ResponseEntity.ok(ApiResponse.success(similar));
    }
    
    /**
     * Khách hàng cũng mua (Collaborative filtering)
     */
    @GetMapping("/bought-together/{productId}")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getFrequentlyBoughtTogether(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<ProductResponse> products = recommendationService.getFrequentlyBoughtTogether(productId, limit);
        return ResponseEntity.ok(ApiResponse.success("Thường được mua cùng", products));
    }
    
    /**
     * Trending - sản phẩm đang hot
     */
    @GetMapping("/trending")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getTrendingProducts(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "7") int days // Trong vòng N ngày
    ) {
        List<ProductResponse> trending = recommendationService.getTrendingProducts(limit, days);
        return ResponseEntity.ok(ApiResponse.success("Sản phẩm đang thịnh hành", trending));
    }
    
    /**
     * Dựa trên location - sản phẩm phổ biến ở khu vực
     */
    @GetMapping("/popular-in-area")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getPopularInArea(
            @RequestParam(required = false) String province,
            @RequestParam(defaultValue = "20") int limit
    ) {
        User currentUser = securityUtil.getCurrentUser();
        String userProvince = province != null ? province : extractProvinceFromAddress(currentUser.getAddress());
        List<ProductResponse> products = recommendationService.getPopularInArea(userProvince, limit);
        return ResponseEntity.ok(ApiResponse.success("Phổ biến tại " + userProvince, products));
    }
    
    /**
     * Lưu hành vi người dùng (view product, add to cart, search)
     */
    @PostMapping("/track")
    public ResponseEntity<ApiResponse<Void>> trackUserBehavior(
            @RequestParam String action, // VIEW, ADD_TO_CART, SEARCH, PURCHASE
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String searchQuery
    ) {
        User currentUser = securityUtil.getCurrentUser();
        recommendationService.trackUserBehavior(currentUser, action, productId, categoryId, searchQuery);
        return ResponseEntity.ok(ApiResponse.success("Đã lưu hành vi", null));
    }
    
    private String extractProvinceFromAddress(String address) {
        if (address == null) return "Hà Nội"; // Default
        // Extract province from address string (simple implementation)
        String[] parts = address.split(",");
        return parts.length > 0 ? parts[parts.length - 1].trim() : "Hà Nội";
    }
}
