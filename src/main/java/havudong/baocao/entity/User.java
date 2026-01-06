package havudong.baocao.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import havudong.baocao.entity.enums.UserRole;
import havudong.baocao.entity.enums.UserStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class User extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 255)
    private String email;
    
    @Column(name = "password_hash", nullable = false)
    @JsonIgnore
    private String passwordHash;
    
    @Column(name = "full_name", length = 255)
    private String fullName;
    
    @Column(length = 20)
    @Pattern(regexp = "^[0-9]{10,11}$", message = "Số điện thoại phải có 10-11 chữ số")
    private String phone;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.CUSTOMER;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;
    
    @Column(columnDefinition = "TEXT")
    private String address;
    
    @Column(name = "avatar_url")
    private String avatarUrl;
}