package havudong.baocao.service;

import havudong.baocao.dto.CartGroupedResponse;
import havudong.baocao.entity.CartItem;
import havudong.baocao.entity.Product;
import havudong.baocao.entity.User;
import havudong.baocao.exception.ResourceNotFoundException;
import havudong.baocao.repository.CartItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {
    
    private final CartItemRepository cartItemRepository;
    
    @Transactional
    public CartItem addToCart(User user, Product product, Integer quantity) {
        log.info("Adding product {} to cart for user {}, quantity: {}", 
                product.getId(), user.getId(), quantity);
        
        Optional<CartItem> existingItem = cartItemRepository.findByUserAndProduct(user, product);
        
        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
            item.setPrice(product.getPrice());
            return cartItemRepository.save(item);
        }
        
        CartItem newItem = new CartItem();
        newItem.setUser(user);
        newItem.setProduct(product);
        newItem.setQuantity(quantity);
        newItem.setPrice(product.getPrice());
        newItem.setSelected(true);
        
        return cartItemRepository.save(newItem);
    }
    
    public List<CartItem> getCartItems(User user) {
        return cartItemRepository.findByUser(user);
    }
    
    public List<CartItem> getSelectedCartItems(User user) {
        return cartItemRepository.findByUserAndSelectedTrue(user);
    }
    
    public BigDecimal calculateTotal(User user) {
        return cartItemRepository.calculateTotalAmount(user);
    }
    
    /**
     * Lấy giỏ hàng đã group theo seller - phục vụ hiển thị UI marketplace
     */
    public CartGroupedResponse getCartGroupedBySeller(User user) {
        List<CartItem> items = cartItemRepository.findByUser(user);
        
        if (items.isEmpty()) {
            return CartGroupedResponse.builder()
                    .sellerGroups(new ArrayList<>())
                    .totalAmount(BigDecimal.ZERO)
                    .totalItems(0)
                    .build();
        }
        
        // Group items theo seller
        Map<Long, List<CartItem>> itemsBySeller = items.stream()
                .collect(Collectors.groupingBy(item -> item.getProduct().getSeller().getId()));
        
        List<CartGroupedResponse.SellerCartGroup> sellerGroups = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalItems = 0;
        
        for (Map.Entry<Long, List<CartItem>> entry : itemsBySeller.entrySet()) {
            List<CartItem> sellerItems = entry.getValue();
            User seller = sellerItems.get(0).getProduct().getSeller();
            
            BigDecimal subtotal = BigDecimal.ZERO;
            List<CartGroupedResponse.CartItemDetail> itemDetails = new ArrayList<>();
            
            for (CartItem item : sellerItems) {
                Product product = item.getProduct();
                BigDecimal itemSubtotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                
                itemDetails.add(CartGroupedResponse.CartItemDetail.builder()
                        .cartItemId(item.getId())
                        .productId(product.getId())
                        .productName(product.getName())
                        .productImage(product.getMainImage())
                        .price(product.getPrice())
                        .originalPrice(product.getOriginalPrice())
                        .quantity(item.getQuantity())
                        .stock(product.getStock())
                        .selected(item.getSelected())
                        .subtotal(itemSubtotal)
                        .build());
                
                if (item.getSelected()) {
                    subtotal = subtotal.add(itemSubtotal);
                }
                totalItems++;
            }
            
            sellerGroups.add(CartGroupedResponse.SellerCartGroup.builder()
                    .sellerId(seller.getId())
                    .sellerName(seller.getFullName())
                    .sellerAvatar(seller.getAvatarUrl())
                    .items(itemDetails)
                    .subtotal(subtotal)
                    .itemCount(sellerItems.size())
                    .build());
            
            totalAmount = totalAmount.add(subtotal);
        }
        
        return CartGroupedResponse.builder()
                .sellerGroups(sellerGroups)
                .totalAmount(totalAmount)
                .totalItems(totalItems)
                .build();
    }
    
    @Transactional
    public CartItem updateCartItem(Long id, Integer quantity) {
        CartItem item = cartItemRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cart item", "id", id));
        
        log.info("Updating cart item {} quantity from {} to {}", id, item.getQuantity(), quantity);
        item.setQuantity(quantity);
        return cartItemRepository.save(item);
    }
    
    @Transactional
    public CartItem toggleSelected(Long id) {
        CartItem item = cartItemRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cart item", "id", id));
        
        item.setSelected(!item.getSelected());
        return cartItemRepository.save(item);
    }
    
    @Transactional
    public void selectAllBySeller(User user, Long sellerId, Boolean selected) {
        List<CartItem> items = cartItemRepository.findByUser(user);
        items.stream()
                .filter(item -> item.getProduct().getSeller().getId().equals(sellerId))
                .forEach(item -> item.setSelected(selected));
        cartItemRepository.saveAll(items);
    }
    
    @Transactional
    public void removeFromCart(Long id) {
        log.info("Removing cart item: {}", id);
        cartItemRepository.deleteById(id);
    }
    
    @Transactional
    public void clearCart(User user) {
        log.info("Clearing cart for user: {}", user.getId());
        cartItemRepository.deleteByUser(user);
    }
}
