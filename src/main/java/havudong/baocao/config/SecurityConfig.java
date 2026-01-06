package havudong.baocao.config;

import havudong.baocao.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // ============ Public endpoints (không cần đăng nhập) ============
                .requestMatchers("/api/auth/**").permitAll()
                
                // Payments: cho phép tất cả để test QR
                .requestMatchers("/api/payments/**").permitAll()
                
                // Products: GET là public
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                
                // Categories: GET là public
                .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                
                // Reviews: GET là public (xem review sản phẩm)
                .requestMatchers(HttpMethod.GET, "/api/reviews/**").permitAll()
                
                // Static resources (uploads)
                .requestMatchers("/uploads/**").permitAll()
                
                // ============ Authenticated endpoints (cần đăng nhập) ============
                
                // Cart - Customer actions
                .requestMatchers("/api/cart/**").authenticated()
                
                // Wishlist - Customer actions
                .requestMatchers("/api/wishlist/**").authenticated()
                
                // Dashboard - Seller/Admin
                .requestMatchers("/api/dashboard/seller").authenticated()
                .requestMatchers("/api/dashboard/admin").hasRole("ADMIN")
                
                // Orders - Customer actions
                .requestMatchers("/api/orders/my-orders").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/orders/*/cancel").authenticated()
                
                // Orders - Seller actions
                .requestMatchers("/api/orders/seller/my-orders").authenticated()
                .requestMatchers("/api/orders/seller/my-revenue").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/orders/*/payment-status").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/orders/*/shipping-status").authenticated()
                
                // Products - Seller actions (create, update, delete)
                .requestMatchers("/api/products/my-products").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/products").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/products/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/products/**").authenticated()
                
                // Reviews - Customer actions (create, delete own review)
                .requestMatchers(HttpMethod.POST, "/api/reviews").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/reviews/**").authenticated()
                .requestMatchers("/api/reviews/me").authenticated()
                
                // User profile
                .requestMatchers("/api/users/me").authenticated()
                .requestMatchers("/api/users/profile").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/users/profile").authenticated()
                
                // Chat
                .requestMatchers("/api/chat/**").authenticated()
                
                // ============ Admin only endpoints ============
                .requestMatchers(HttpMethod.POST, "/api/categories/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/categories/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasRole("ADMIN")
                .requestMatchers("/api/orders/revenue").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/orders").hasRole("ADMIN")
                .requestMatchers("/api/users").hasRole("ADMIN")
                
                // Any other request requires authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
