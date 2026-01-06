package havudong.baocao.service;

import havudong.baocao.dto.*;
import havudong.baocao.entity.User;
import havudong.baocao.entity.enums.UserStatus;
import havudong.baocao.exception.DuplicateResourceException;
import havudong.baocao.exception.ResourceNotFoundException;
import havudong.baocao.exception.UnauthorizedException;
import havudong.baocao.repository.UserRepository;
import havudong.baocao.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check email đã tồn tại chưa
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }
        
        // Tạo user mới
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setRole(request.getRole());
        user.setStatus(UserStatus.ACTIVE);
        user.setAddress(request.getAddress());
        
        User savedUser = userRepository.save(user);
        
        // Generate token
        String token = jwtUtil.generateToken(savedUser);
        
        // Return response
        return new AuthResponse(token, mapToUserResponse(savedUser));
    }
    
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // Tìm user theo email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Email hoặc mật khẩu không đúng"));
        
        // Check password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Email hoặc mật khẩu không đúng");
        }
        
        // Check status
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản đã bị khóa hoặc vô hiệu hóa");
        }
        
        // Generate token
        String token = jwtUtil.generateToken(user);
        
        // Return response
        return new AuthResponse(token, mapToUserResponse(user));
    }
    
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return mapToUserResponse(user);
    }
    
    private UserResponse mapToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setPhone(user.getPhone());
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());
        response.setAddress(user.getAddress());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}
