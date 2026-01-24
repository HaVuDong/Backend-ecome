package havudong.baocao.repository;

import havudong.baocao.entity.PasswordReset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetRepository extends JpaRepository<PasswordReset, Long> {
    Optional<PasswordReset> findByResetTokenAndUsedFalse(String resetToken);
    Optional<PasswordReset> findFirstByPhoneAndUsedFalseOrderByCreatedAtDesc(String phone);
}
