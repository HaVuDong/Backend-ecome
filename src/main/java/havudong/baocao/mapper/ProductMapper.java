package havudong.baocao.mapper;

import havudong.baocao.dto.ProductRequest;
import havudong.baocao.dto.ProductResponse;
import havudong.baocao.entity.Category;
import havudong.baocao.entity.Product;
import havudong.baocao.entity.User;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {
    
    public Product toEntity(ProductRequest request, User seller, Category category) {
        Product product = new Product();
        product.setSeller(seller);
        product.setCategory(category);
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setOriginalPrice(request.getOriginalPrice());
        product.setStock(request.getStock());
        product.setMainImage(request.getMainImage());
        product.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        return product;
    }
    
    public void updateEntity(Product product, ProductRequest request, Category category) {
        product.setCategory(category);
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setOriginalPrice(request.getOriginalPrice());
        product.setStock(request.getStock());
        product.setMainImage(request.getMainImage());
        if (request.getIsActive() != null) {
            product.setIsActive(request.getIsActive());
        }
    }
    
    public ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .originalPrice(product.getOriginalPrice())
                .stock(product.getStock())
                .mainImage(product.getMainImage())
                .rating(product.getRating())
                .soldCount(product.getSoldCount())
                .isActive(product.getIsActive())
                .createdAt(product.getCreatedAt())
                .seller(ProductResponse.SellerInfo.builder()
                        .id(product.getSeller().getId())
                        .fullName(product.getSeller().getFullName())
                        .email(product.getSeller().getEmail())
                        .avatarUrl(product.getSeller().getAvatarUrl())
                        .build())
                .category(ProductResponse.CategoryInfo.builder()
                        .id(product.getCategory().getId())
                        .name(product.getCategory().getName())
                        .icon(product.getCategory().getIcon())
                        .build())
                .build();
    }
}
