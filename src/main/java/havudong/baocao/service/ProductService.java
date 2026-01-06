package havudong.baocao.service;

import havudong.baocao.entity.Category;
import havudong.baocao.entity.Product;
import havudong.baocao.entity.User;
import havudong.baocao.exception.OutOfStockException;
import havudong.baocao.exception.ResourceNotFoundException;
import havudong.baocao.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {
    
    private final ProductRepository productRepository;
    
    @Transactional
    public Product createProduct(Product product) {
        log.info("Creating product: {} for seller: {}", product.getName(), product.getSeller().getId());
        return productRepository.save(product);
    }
    
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }
    
    /**
     * Lấy product với pessimistic lock để update stock an toàn
     */
    @Transactional
    public Product getProductByIdWithLock(Long id) {
        return productRepository.findByIdWithLock(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    }
    
    public Page<Product> getAllActiveProducts(Pageable pageable) {
        return productRepository.findByIsActiveTrue(pageable);
    }
    
    public Page<Product> getProductsByCategory(Category category, Pageable pageable) {
        return productRepository.findByCategoryAndIsActiveTrue(category, pageable);
    }
    
    public Page<Product> getProductsBySeller(User seller, Pageable pageable) {
        return productRepository.findBySeller(seller, pageable);
    }
    
    public Page<Product> searchProducts(String keyword, Pageable pageable) {
        return productRepository.searchByKeyword(keyword, pageable);
    }
    
    public Page<Product> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        return productRepository.findByPriceRange(minPrice, maxPrice, pageable);
    }
    
    public List<Product> getTopSellingProducts() {
        return productRepository.findTop10ByIsActiveTrueOrderBySoldCountDesc();
    }
    
    public List<Product> getNewestProducts() {
        return productRepository.findTop10ByIsActiveTrueOrderByCreatedAtDesc();
    }
    
    @Transactional
    public Product updateProduct(Long id, Product productDetails) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        
        log.info("Updating product: {} by seller: {}", id, product.getSeller().getId());
        
        product.setName(productDetails.getName());
        product.setDescription(productDetails.getDescription());
        product.setPrice(productDetails.getPrice());
        product.setOriginalPrice(productDetails.getOriginalPrice());
        product.setStock(productDetails.getStock());
        product.setMainImage(productDetails.getMainImage());
        product.setCategory(productDetails.getCategory());
        
        return productRepository.save(product);
    }
    
    /**
     * Soft delete - đánh dấu sản phẩm không active
     */
    @Transactional
    public void deactivateProduct(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        
        log.info("Deactivating product: {} by seller: {}", id, product.getSeller().getId());
        product.setIsActive(false);
        productRepository.save(product);
    }
    
    /**
     * Giảm stock khi đặt hàng - có lock để tránh race condition
     */
    @Transactional
    public void decreaseStock(Long productId, int quantity) {
        Product product = productRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        
        if (product.getStock() < quantity) {
            log.warn("Out of stock for product: {}, requested: {}, available: {}", 
                    productId, quantity, product.getStock());
            throw new OutOfStockException(product.getName(), quantity, product.getStock());
        }
        
        product.setStock(product.getStock() - quantity);
        product.setSoldCount(product.getSoldCount() + quantity);
        
        log.info("Decreased stock for product: {}, quantity: {}, remaining: {}", 
                productId, quantity, product.getStock());
        
        productRepository.save(product);
    }
    
    /**
     * Hoàn stock khi hủy đơn hàng
     */
    @Transactional
    public void restoreStock(Long productId, int quantity) {
        Product product = productRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        
        product.setStock(product.getStock() + quantity);
        product.setSoldCount(Math.max(0, product.getSoldCount() - quantity));
        
        log.info("Restored stock for product: {}, quantity: {}, new stock: {}", 
                productId, quantity, product.getStock());
        
        productRepository.save(product);
    }
    
    /**
     * Cập nhật rating của product dựa trên reviews
     */
    @Transactional
    public void updateProductRating(Long productId, BigDecimal newRating) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        
        product.setRating(newRating);
        productRepository.save(product);
        
        log.info("Updated rating for product: {} to {}", productId, newRating);
    }
    
    /**
     * Tìm kiếm với filters nâng cao
     */
    public Page<Product> searchProductsWithFilters(
            String keyword,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Double minRating,
            Pageable pageable) {
        
        log.info("Searching products with filters - keyword: {}, categoryId: {}, price: {}-{}, minRating: {}", 
                keyword, categoryId, minPrice, maxPrice, minRating);
        
        return productRepository.searchWithFilters(keyword, categoryId, minPrice, maxPrice, minRating, pageable);
    }
    
    /**
     * Lấy sản phẩm theo category với filters
     */
    public Page<Product> getProductsWithFilters(
            Category category,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Double minRating,
            Pageable pageable) {
        
        return productRepository.findWithFilters(category, minPrice, maxPrice, minRating, pageable);
    }
    
    // Giữ lại method cũ để backward compatible nhưng đánh dấu deprecated
    @Deprecated
    @Transactional
    public void deleteProduct(Long id) {
        deactivateProduct(id);
    }
}
