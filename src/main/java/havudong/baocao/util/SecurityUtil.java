package havudong.baocao.util;

import havudong.baocao.entity.User;
import havudong.baocao.exception.UnauthorizedException;
import havudong.baocao.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Utility class để lấy thông tin user hiện tại từ JWT token
 * Thay thế việc truyền userId qua request param
 */
@Component
@RequiredArgsConstructor
public class SecurityUtil {
    
    private final UserRepository userRepository;
    
    /**
     * Lấy email của user đang đăng nhập từ SecurityContext
     */
    public String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Bạn chưa đăng nhập");
        }
        return authentication.getName();
    }
    
    /**
     * Lấy User entity của user đang đăng nhập
     */
    public User getCurrentUser() {
        String email = getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy thông tin người dùng"));
    }
    
    /**
     * Lấy ID của user đang đăng nhập
     */
    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }
    
    /**
     * Kiểm tra user hiện tại có phải là owner của resource không
     */
    public boolean isCurrentUser(Long userId) {
        return getCurrentUserId().equals(userId);
    }
    
    /**
     * Kiểm tra user hiện tại có role SELLER không
     */
    public boolean isSeller() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_SELLER"));
    }
    
    /**
     * Kiểm tra user hiện tại có role ADMIN không
     */
    public boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
    }
}
