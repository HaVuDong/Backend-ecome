package havudong.baocao.service;

import havudong.baocao.dto.WishlistResponse;
import havudong.baocao.entity.Product;
import havudong.baocao.entity.User;
import havudong.baocao.entity.Wishlist;
import havudong.baocao.exception.BadRequestException;
import havudong.baocao.exception.ResourceNotFoundException;
import havudong.baocao.repository.ProductRepository;
import havudong.baocao.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service quản lý Wishlist
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WishlistService {
    
    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    
    /**
     * Thêm sản phẩm vào wishlist
     */
    @Transactional
    public WishlistResponse addToWishlist(User user, Long productId) {
        // Kiểm tra product tồn tại
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        
        // Kiểm tra đã có trong wishlist chưa
        if (wishlistRepository.existsByUserAndProduct(user, product)) {
            throw new BadRequestException("Sản phẩm đã có trong danh sách yêu thích");
        }
        
        // Tạo wishlist item
        Wishlist wishlist = Wishlist.builder()
                .user(user)
                .product(product)
                .build();
        
        wishlist = wishlistRepository.save(wishlist);
        log.info("User {} added product {} to wishlist", user.getId(), productId);
        
        return mapToResponse(wishlist);
    }
    
    /**
     * Xóa sản phẩm khỏi wishlist
     */
    @Transactional
    public void removeFromWishlist(User user, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        
        Wishlist wishlist = wishlistRepository.findByUserAndProduct(user, product)
                .orElseThrow(() -> new BadRequestException("Sản phẩm không có trong danh sách yêu thích"));
        
        wishlistRepository.delete(wishlist);
        log.info("User {} removed product {} from wishlist", user.getId(), productId);
    }
    
    /**
     * Toggle wishlist - thêm nếu chưa có, xóa nếu đã có
     */
    @Transactional
    public boolean toggleWishlist(User user, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        
        if (wishlistRepository.existsByUserAndProduct(user, product)) {
            wishlistRepository.deleteByUserAndProduct(user, product);
            log.info("User {} removed product {} from wishlist (toggle)", user.getId(), productId);
            return false; // Đã xóa
        } else {
            Wishlist wishlist = Wishlist.builder()
                    .user(user)
                    .product(product)
                    .build();
            wishlistRepository.save(wishlist);
            log.info("User {} added product {} to wishlist (toggle)", user.getId(), productId);
            return true; // Đã thêm
        }
    }
    
    /**
     * Lấy danh sách wishlist của user
     */
    @Transactional(readOnly = true)
    public Page<WishlistResponse> getWishlist(User user, Pageable pageable) {
        return wishlistRepository.findByUser(user, pageable)
                .map(this::mapToResponse);
    }
    
    /**
     * Kiểm tra product có trong wishlist không
     */
    public boolean isInWishlist(User user, Long productId) {
        return productRepository.findById(productId)
                .map(product -> wishlistRepository.existsByUserAndProduct(user, product))
                .orElse(false);
    }
    
    /**
     * Lấy danh sách productIds trong wishlist
     */
    public List<Long> getWishlistProductIds(User user) {
        return wishlistRepository.findProductIdsByUser(user);
    }
    
    /**
     * Đếm số sản phẩm trong wishlist
     */
    public long countWishlist(User user) {
        return wishlistRepository.countByUser(user);
    }
    
    /**
     * Convert entity to response DTO
     */
    private WishlistResponse mapToResponse(Wishlist wishlist) {
        Product product = wishlist.getProduct();
        return WishlistResponse.builder()
                .id(wishlist.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productImage(product.getMainImage())
                .productPrice(product.getPrice())
                .productOriginalPrice(product.getOriginalPrice())
                .productStock(product.getStock())
                .isAvailable(product.getIsActive() && product.getStock() > 0)
                .sellerName(product.getSeller().getFullName())
                .sellerId(product.getSeller().getId())
                .addedAt(wishlist.getCreatedAt())
                .build();
    }
}
