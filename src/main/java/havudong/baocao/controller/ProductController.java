package havudong.baocao.controller;

import havudong.baocao.dto.ApiResponse;
import havudong.baocao.dto.ProductRequest;
import havudong.baocao.dto.ProductResponse;
import havudong.baocao.entity.Category;
import havudong.baocao.entity.Product;
import havudong.baocao.entity.User;
import havudong.baocao.exception.ResourceNotFoundException;
import havudong.baocao.exception.UnauthorizedException;
import havudong.baocao.mapper.ProductMapper;
import havudong.baocao.service.CategoryService;
import havudong.baocao.service.ProductService;
import havudong.baocao.service.UserService;
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

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    
    private final ProductService productService;
    private final UserService userService;
    private final CategoryService categoryService;
    private final ProductMapper productMapper;
    private final SecurityUtil securityUtil;
    
    /**
     * Tạo sản phẩm mới - seller lấy từ JWT token
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductRequest request) {
        // Lấy seller từ JWT token
        User seller = securityUtil.getCurrentUser();
        
        Category category = categoryService.getCategoryById(request.getCategoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
        
        // Convert DTO to Entity - dùng seller từ JWT
        Product product = productMapper.toEntity(request, seller, category);
        
        // Save và convert response
        Product created = productService.createProduct(product);
        ProductResponse response = productMapper.toResponse(created);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo sản phẩm thành công", response));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        return productService.getProductById(id)
            .map(productMapper::toResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction
    ) {
        Sort sort = direction.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Product> products = productService.getAllActiveProducts(pageable);
        return ResponseEntity.ok(products.map(productMapper::toResponse));
    }
    
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<ProductResponse>> getProductsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return categoryService.getCategoryById(categoryId)
            .map(category -> {
                Pageable pageable = PageRequest.of(page, size);
                Page<Product> products = productService.getProductsByCategory(category, pageable);
                return ResponseEntity.ok(products.map(productMapper::toResponse));
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Lấy sản phẩm của seller hiện tại (từ JWT)
     */
    @GetMapping("/my-products")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getMyProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        User seller = securityUtil.getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Product> products = productService.getProductsBySeller(seller, pageable);
        return ResponseEntity.ok(ApiResponse.success(products.map(productMapper::toResponse)));
    }
    
    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<Page<ProductResponse>> getProductsBySeller(
            @PathVariable Long sellerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return userService.getUserById(sellerId)
            .map(seller -> {
                Pageable pageable = PageRequest.of(page, size);
                Page<Product> products = productService.getProductsBySeller(seller, pageable);
                return ResponseEntity.ok(products.map(productMapper::toResponse));
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/search")
    public ResponseEntity<Page<ProductResponse>> searchProducts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products = productService.searchProducts(keyword, pageable);
        return ResponseEntity.ok(products.map(productMapper::toResponse));
    }
    
    /**
     * Tìm kiếm với filters nâng cao
     * GET /api/products/search/advanced?keyword=...&categoryId=...&minPrice=...&maxPrice=...&minRating=...
     */
    @GetMapping("/search/advanced")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> searchProductsAdvanced(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Double minRating,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction
    ) {
        Sort sort = direction.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Product> products = productService.searchProductsWithFilters(
                keyword, categoryId, minPrice, maxPrice, minRating, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(products.map(productMapper::toResponse)));
    }
    
    @GetMapping("/price-range")
    public ResponseEntity<Page<ProductResponse>> getProductsByPriceRange(
            @RequestParam BigDecimal minPrice,
            @RequestParam BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products = productService.getProductsByPriceRange(minPrice, maxPrice, pageable);
        return ResponseEntity.ok(products.map(productMapper::toResponse));
    }
    
    @GetMapping("/top-selling")
    public ResponseEntity<List<ProductResponse>> getTopSellingProducts() {
        List<Product> products = productService.getTopSellingProducts();
        List<ProductResponse> responses = products.stream()
            .map(productMapper::toResponse)
            .toList();
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/newest")
    public ResponseEntity<List<ProductResponse>> getNewestProducts() {
        List<Product> products = productService.getNewestProducts();
        List<ProductResponse> responses = products.stream()
            .map(productMapper::toResponse)
            .toList();
        return ResponseEntity.ok(responses);
    }
    
    /**
     * Cập nhật sản phẩm - kiểm tra quyền từ JWT
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id, 
            @Valid @RequestBody ProductRequest request
    ) {
        // Lấy seller từ JWT
        User currentUser = securityUtil.getCurrentUser();
        
        // Load category từ DB
        Category category = categoryService.getCategoryById(request.getCategoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
        
        // Load existing product
        Product existingProduct = productService.getProductById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        
        // Kiểm tra quyền sở hữu: Seller chỉ được sửa sản phẩm của mình
        if (!existingProduct.getSeller().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("Bạn không có quyền sửa sản phẩm này. Sản phẩm không thuộc về bạn.");
        }
        
        // Update fields
        existingProduct.setName(request.getName());
        existingProduct.setDescription(request.getDescription());
        existingProduct.setPrice(request.getPrice());
        existingProduct.setOriginalPrice(request.getOriginalPrice());
        existingProduct.setStock(request.getStock());
        existingProduct.setMainImage(request.getMainImage());
        existingProduct.setCategory(category);
        if (request.getIsActive() != null) {
            existingProduct.setIsActive(request.getIsActive());
        }
        
        // Save và convert response
        Product updated = productService.updateProduct(id, existingProduct);
        ProductResponse response = productMapper.toResponse(updated);
        
        return ResponseEntity.ok(ApiResponse.success("Cập nhật sản phẩm thành công", response));
    }
    
    /**
     * Xóa (deactivate) sản phẩm - kiểm tra quyền từ JWT
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        // Lấy seller từ JWT
        User currentUser = securityUtil.getCurrentUser();
        
        // Load existing product
        Product existingProduct = productService.getProductById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        
        // Kiểm tra quyền sở hữu
        if (!existingProduct.getSeller().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("Bạn không có quyền xóa sản phẩm này. Sản phẩm không thuộc về bạn.");
        }
        
        productService.deactivateProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa sản phẩm", null));
    }
    
    // ========== Legacy endpoints for backward compatibility ==========
    
    @PostMapping("/legacy")
    @Deprecated
    public ResponseEntity<ProductResponse> createProductLegacy(@Valid @RequestBody ProductRequest request) {
        User seller = userService.getUserById(request.getSellerId())
            .orElseThrow(() -> new ResourceNotFoundException("Seller", "id", request.getSellerId()));
        Category category = categoryService.getCategoryById(request.getCategoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
        
        Product product = productMapper.toEntity(request, seller, category);
        Product created = productService.createProduct(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(productMapper.toResponse(created));
    }
}
