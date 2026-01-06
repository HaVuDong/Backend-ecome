package havudong.baocao.dto;

import havudong.baocao.entity.enums.UserRole;
import havudong.baocao.entity.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private UserRole role;
    private UserStatus status;
    private String address;
    private String avatarUrl;
    private LocalDateTime createdAt;
}
