package havudong.baocao.controller;

import havudong.baocao.dto.ApiResponse;
import havudong.baocao.dto.ReviewRequest;
import havudong.baocao.dto.ReviewResponse;
import havudong.baocao.entity.User;
import havudong.baocao.service.ReviewService;
import havudong.baocao.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {
    
    private final ReviewService reviewService;
    private final SecurityUtil securityUtil;
    
    /**
     * Tạo review mới cho sản phẩm
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @Valid @RequestBody ReviewRequest request
    ) {
        User user = securityUtil.getCurrentUser();
        ReviewResponse response = reviewService.createReview(user, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đánh giá thành công", response));
    }
    
    /**
     * Lấy reviews của sản phẩm
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ReviewResponse> reviews = reviewService.getProductReviews(productId, pageable);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }
    
    /**
     * Lấy thống kê rating của sản phẩm
     */
    @GetMapping("/product/{productId}/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProductRatingStats(
            @PathVariable Long productId
    ) {
        Map<String, Object> stats = reviewService.getProductRatingStats(productId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
    
    /**
     * Lấy reviews của user hiện tại
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getMyReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        User user = securityUtil.getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ReviewResponse> reviews = reviewService.getUserReviews(user, pageable);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }
    
    /**
     * Kiểm tra user có thể review sản phẩm không (đã mua và đã giao)
     */
    @GetMapping("/can-review")
    public ResponseEntity<ApiResponse<Boolean>> canReviewProduct(
            @RequestParam Long productId,
            @RequestParam Long orderId
    ) {
        User user = securityUtil.getCurrentUser();
        boolean canReview = reviewService.canUserReviewProduct(user, productId, orderId);
        return ResponseEntity.ok(ApiResponse.success(canReview));
    }
    
    /**
     * Xóa review
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable Long id) {
        User user = securityUtil.getCurrentUser();
        reviewService.deleteReview(id, user);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa đánh giá", null));
    }
}
