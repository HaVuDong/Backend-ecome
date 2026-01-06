package havudong.baocao.controller;

import havudong.baocao.dto.ApiResponse;
import havudong.baocao.dto.CartGroupedResponse;
import havudong.baocao.dto.CheckoutRequest;
import havudong.baocao.dto.OrderResponse;
import havudong.baocao.entity.CartItem;
import havudong.baocao.entity.User;
import havudong.baocao.service.CartService;
import havudong.baocao.service.CheckoutService;
import havudong.baocao.service.ProductService;
import havudong.baocao.service.UserService;
import havudong.baocao.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {
    
    private final CartService cartService;
    private final UserService userService;
    private final ProductService productService;
    private final CheckoutService checkoutService;
    private final SecurityUtil securityUtil;
    
    /**
     * Thêm sản phẩm vào giỏ hàng - lấy user từ JWT
     */
    @PostMapping("/add")
    public ResponseEntity<ApiResponse<CartItem>> addToCart(
            @RequestParam Long productId,
            @RequestParam(defaultValue = "1") Integer quantity
    ) {
        User user = securityUtil.getCurrentUser();
        return productService.getProductById(productId)
                .map(product -> {
                    CartItem item = cartService.addToCart(user, product, quantity);
                    return ResponseEntity.status(HttpStatus.CREATED)
                            .body(ApiResponse.success("Đã thêm vào giỏ hàng", item));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Không tìm thấy sản phẩm")));
    }
    
    /**
     * Lấy giỏ hàng của user hiện tại
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CartItem>>> getCartItems() {
        User user = securityUtil.getCurrentUser();
        List<CartItem> items = cartService.getCartItems(user);
        return ResponseEntity.ok(ApiResponse.success(items));
    }
    
    /**
     * Lấy giỏ hàng đã group theo seller - cho UI marketplace
     */
    @GetMapping("/grouped")
    public ResponseEntity<ApiResponse<CartGroupedResponse>> getCartGroupedBySeller() {
        User user = securityUtil.getCurrentUser();
        CartGroupedResponse response = cartService.getCartGroupedBySeller(user);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * Lấy các items đã chọn
     */
    @GetMapping("/selected")
    public ResponseEntity<ApiResponse<List<CartItem>>> getSelectedCartItems() {
        User user = securityUtil.getCurrentUser();
        List<CartItem> items = cartService.getSelectedCartItems(user);
        return ResponseEntity.ok(ApiResponse.success(items));
    }
    
    /**
     * Tính tổng tiền các items đã chọn
     */
    @GetMapping("/total")
    public ResponseEntity<ApiResponse<Map<String, BigDecimal>>> getCartTotal() {
        User user = securityUtil.getCurrentUser();
        BigDecimal total = cartService.calculateTotal(user);
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("total", total != null ? total : BigDecimal.ZERO)));
    }
    
    /**
     * Cập nhật số lượng
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CartItem>> updateCartItem(
            @PathVariable Long id,
            @RequestParam Integer quantity
    ) {
        CartItem updated = cartService.updateCartItem(id, quantity);
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật số lượng", updated));
    }
    
    /**
     * Toggle chọn/bỏ chọn item
     */
    @PutMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<CartItem>> toggleSelected(@PathVariable Long id) {
        CartItem updated = cartService.toggleSelected(id);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }
    
    /**
     * Chọn/bỏ chọn tất cả items của 1 seller
     */
    @PutMapping("/seller/{sellerId}/select")
    public ResponseEntity<ApiResponse<Void>> selectAllBySeller(
            @PathVariable Long sellerId,
            @RequestParam Boolean selected
    ) {
        User user = securityUtil.getCurrentUser();
        cartService.selectAllBySeller(user, sellerId, selected);
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật", null));
    }
    
    /**
     * Xóa item khỏi giỏ hàng
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> removeFromCart(@PathVariable Long id) {
        cartService.removeFromCart(id);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa khỏi giỏ hàng", null));
    }
    
    /**
     * Xóa toàn bộ giỏ hàng
     */
    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<Void>> clearCart() {
        User user = securityUtil.getCurrentUser();
        cartService.clearCart(user);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa giỏ hàng", null));
    }
    
    /**
     * Checkout - tạo đơn hàng từ giỏ hàng
     * Tự động tách order theo seller
     */
    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> checkout(
            @Valid @RequestBody CheckoutRequest request
    ) {
        User user = securityUtil.getCurrentUser();
        List<OrderResponse> orders = checkoutService.checkout(user, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đặt hàng thành công", orders));
    }
    
    // ========== Legacy endpoints for backward compatibility ==========
    
    @PostMapping("/add/legacy")
    @Deprecated
    public ResponseEntity<CartItem> addToCartLegacy(
            @RequestParam Long userId,
            @RequestParam Long productId,
            @RequestParam Integer quantity
    ) {
        return userService.getUserById(userId)
            .flatMap(user -> productService.getProductById(productId)
                .map(product -> {
                    CartItem item = cartService.addToCart(user, product, quantity);
                    return ResponseEntity.status(HttpStatus.CREATED).body(item);
                }))
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/user/{userId}")
    @Deprecated
    public ResponseEntity<List<CartItem>> getCartItemsLegacy(@PathVariable Long userId) {
        return userService.getUserById(userId)
            .map(user -> ResponseEntity.ok(cartService.getCartItems(user)))
            .orElse(ResponseEntity.notFound().build());
    }
}
