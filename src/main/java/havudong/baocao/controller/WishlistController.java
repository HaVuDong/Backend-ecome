package havudong.baocao.controller;

import havudong.baocao.dto.ApiResponse;
import havudong.baocao.dto.WishlistResponse;
import havudong.baocao.entity.User;
import havudong.baocao.service.WishlistService;
import havudong.baocao.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller cho Wishlist API
 */
@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
@Slf4j
public class WishlistController {
    
    private final WishlistService wishlistService;
    private final SecurityUtil securityUtil;
    
    /**
     * Lấy danh sách wishlist của user hiện tại
     * GET /api/wishlist
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<WishlistResponse>>> getWishlist(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        User user = securityUtil.getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<WishlistResponse> wishlist = wishlistService.getWishlist(user, pageable);
        return ResponseEntity.ok(ApiResponse.success(wishlist));
    }
    
    /**
     * Thêm sản phẩm vào wishlist
     * POST /api/wishlist/{productId}
     */
    @PostMapping("/{productId}")
    public ResponseEntity<ApiResponse<WishlistResponse>> addToWishlist(@PathVariable Long productId) {
        User user = securityUtil.getCurrentUser();
        WishlistResponse response = wishlistService.addToWishlist(user, productId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đã thêm vào danh sách yêu thích", response));
    }
    
    /**
     * Xóa sản phẩm khỏi wishlist
     * DELETE /api/wishlist/{productId}
     */
    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> removeFromWishlist(@PathVariable Long productId) {
        User user = securityUtil.getCurrentUser();
        wishlistService.removeFromWishlist(user, productId);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa khỏi danh sách yêu thích", null));
    }
    
    /**
     * Toggle wishlist - thêm nếu chưa có, xóa nếu đã có
     * POST /api/wishlist/{productId}/toggle
     */
    @PostMapping("/{productId}/toggle")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleWishlist(@PathVariable Long productId) {
        User user = securityUtil.getCurrentUser();
        boolean isAdded = wishlistService.toggleWishlist(user, productId);
        
        String message = isAdded ? "Đã thêm vào danh sách yêu thích" : "Đã xóa khỏi danh sách yêu thích";
        return ResponseEntity.ok(ApiResponse.success(message, Map.of(
                "isInWishlist", isAdded,
                "productId", productId
        )));
    }
    
    /**
     * Kiểm tra sản phẩm có trong wishlist không
     * GET /api/wishlist/check/{productId}
     */
    @GetMapping("/check/{productId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkInWishlist(@PathVariable Long productId) {
        User user = securityUtil.getCurrentUser();
        boolean isInWishlist = wishlistService.isInWishlist(user, productId);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "isInWishlist", isInWishlist,
                "productId", productId
        )));
    }
    
    /**
     * Lấy danh sách productIds trong wishlist (để check nhiều sản phẩm cùng lúc)
     * GET /api/wishlist/product-ids
     */
    @GetMapping("/product-ids")
    public ResponseEntity<ApiResponse<List<Long>>> getWishlistProductIds() {
        User user = securityUtil.getCurrentUser();
        List<Long> productIds = wishlistService.getWishlistProductIds(user);
        return ResponseEntity.ok(ApiResponse.success(productIds));
    }
    
    /**
     * Đếm số sản phẩm trong wishlist
     * GET /api/wishlist/count
     */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> countWishlist() {
        User user = securityUtil.getCurrentUser();
        long count = wishlistService.countWishlist(user);
        return ResponseEntity.ok(ApiResponse.success(Map.of("count", count)));
    }
}
