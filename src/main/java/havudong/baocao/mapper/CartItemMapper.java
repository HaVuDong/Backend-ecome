package havudong.baocao.mapper;

import havudong.baocao.dto.CartItemResponse;
import havudong.baocao.entity.CartItem;
import org.springframework.stereotype.Component;

@Component
public class CartItemMapper {
    
    public CartItemResponse toResponse(CartItem cartItem) {
        return CartItemResponse.builder()
                .id(cartItem.getId())
                .quantity(cartItem.getQuantity())
                .price(cartItem.getPrice())
                .selected(cartItem.getSelected())
                .product(CartItemResponse.ProductInfo.builder()
                        .id(cartItem.getProduct().getId())
                        .name(cartItem.getProduct().getName())
                        .price(cartItem.getProduct().getPrice())
                        .stock(cartItem.getProduct().getStock())
                        .mainImage(cartItem.getProduct().getMainImage())
                        .isActive(cartItem.getProduct().getIsActive())
                        .build())
                .build();
    }
}
