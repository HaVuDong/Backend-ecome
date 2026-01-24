package havudong.baocao.controller;

import havudong.baocao.dto.AiProductResponse;
import havudong.baocao.dto.ApiResponse;
import havudong.baocao.entity.Product;
import havudong.baocao.entity.User;
import havudong.baocao.service.ProductService;
import havudong.baocao.service.RecommendationService;
import havudong.baocao.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AiSearchController {

    private final ProductService productService;
    private final RecommendationService recommendationService;
    private final SecurityUtil securityUtil;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<AiProductResponse>>> searchProductsForAi(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "6") int limit,
            @RequestParam(required = false) Long categoryId
    ) {
        Pageable pageable = PageRequest.of(0, Math.max(1, limit));
        Page<Product> page = productService.searchProductsWithFilters(keyword, categoryId, null, null, null, pageable);
        List<AiProductResponse> list = page.getContent().stream().map(p -> AiProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .price(p.getPrice().longValue())
                .mainImage(p.getMainImage())
                .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .build()).collect(Collectors.toList());

        // Track behavior if authenticated
        try {
            User u = securityUtil.getCurrentUser();
            if (u != null) {
                recommendationService.trackUserBehavior(u, "SEARCH", null, categoryId, keyword);
            }
        } catch (Exception ex) {
            // ignore - optional
            log.debug("No authenticated user for AI search or tracking failed: {}", ex.getMessage());
        }

        return ResponseEntity.ok(ApiResponse.success(list));
    }
}
