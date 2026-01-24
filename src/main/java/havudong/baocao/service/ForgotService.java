package havudong.baocao.service;

import havudong.baocao.entity.PasswordReset;
import havudong.baocao.entity.User;
import havudong.baocao.repository.PasswordResetRepository;
import havudong.baocao.repository.UserRepository;
import havudong.baocao.sms.TwilioVerifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ForgotService {

    private final PasswordResetRepository passwordResetRepository;
    private final UserRepository userRepository;
    private final TwilioVerifyService twilioVerifyService;

    @Value("${sms.provider:}")
    private String smsProvider;

    // Normalize phone numbers to local VN format (e.g., 0XXXXXXXXX)
    private String normalizePhone(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("\\D", "");
        if (digits.startsWith("84") && digits.length() > 2) {
            return "0" + digits.substring(2);
        }
        if (digits.startsWith("0")) return digits;
        if (digits.length() == 9 || digits.length() == 10) return digits.startsWith("0") ? digits : "0" + digits;
        return digits;
    }

    private String toLocalFormat(String phone) {
        if (phone == null) return null;
        String d = phone.replaceAll("\\D", "");
        if (d.startsWith("84")) return "0" + d.substring(2);
        return d;
    }

    private String toInternationalFormat(String phone) {
        if (phone == null) return null;
        String d = phone.replaceAll("\\D", "");
        if (d.startsWith("0")) return "84" + d.substring(1);
        return d;
    }

    public void requestOtp(String phone) {
        String normalized = normalizePhone(phone);
        // Ensure phone is registered
        if (userRepository.findByPhone(normalized).isEmpty()) {
            throw new havudong.baocao.exception.BadRequestException("Phone number not registered");
        }
        // If Twilio configured, delegate sending
        if ("twilio_verify".equalsIgnoreCase(smsProvider)) {
            twilioVerifyService.sendVerification(normalized);
            // Store a marker record
            PasswordReset pr = new PasswordReset();
            pr.setPhone(normalized);
            pr.setCreatedAt(Instant.now());
            pr.setExpiresAt(Instant.now().plus(15, ChronoUnit.MINUTES));
            pr.setUsed(false);
            passwordResetRepository.save(pr);
            return;
        }
        // Fallback: not implemented, throw for now
        throw new IllegalStateException("No SMS provider configured");
    }

    @Transactional
    public String verifyOtpAndCreateToken(String phone, String code) {
        String normalized = normalizePhone(phone);
        boolean ok = false;
        if ("twilio_verify".equalsIgnoreCase(smsProvider)) {
            ok = twilioVerifyService.checkVerification(normalized, code);
        }
        if (!ok) throw new havudong.baocao.exception.BadRequestException("Invalid OTP");

        // generate reset token and persist
        String token = UUID.randomUUID().toString();
        PasswordReset pr = new PasswordReset();
        pr.setPhone(normalized);
        pr.setResetToken(token);
        pr.setCreatedAt(Instant.now());
        pr.setExpiresAt(Instant.now().plus(15, ChronoUnit.MINUTES));
        pr.setUsed(false);
        passwordResetRepository.save(pr);
        return token;
    }

    @Transactional
    public void resetPassword(String resetToken, String newPassword, org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        Optional<PasswordReset> opt = passwordResetRepository.findByResetTokenAndUsedFalse(resetToken);
        if (opt.isEmpty()) throw new havudong.baocao.exception.BadRequestException("Invalid or used reset token");
        PasswordReset pr = opt.get();
        if (pr.getExpiresAt().isBefore(Instant.now())) throw new havudong.baocao.exception.BadRequestException("Reset token expired");

        String phone = pr.getPhone();
        // try flexible lookup using normalized variants
        Optional<User> userOpt = userRepository.findByPhone(phone);
        if (userOpt.isEmpty()) {
            // try alternative variations
            String alt1 = toLocalFormat(phone); // 84xxxx -> 0xxxx
            String alt2 = toInternationalFormat(phone); // 0xxxx -> 84xxxx
            if (!alt1.equals(phone)) userOpt = userRepository.findByPhone(alt1);
            if (userOpt.isEmpty() && !alt2.equals(phone)) userOpt = userRepository.findByPhone(alt2);
        }
        if (userOpt.isEmpty()) {
            // last attempt: try stripping non-digits and re-lookup
            String stripped = phone.replaceAll("\\D", "");
            userOpt = userRepository.findByPhone(stripped);
        }
        User user = userOpt.orElseThrow(() -> new IllegalArgumentException("User not found for phone: " + phone));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        pr.setUsed(true);
        passwordResetRepository.save(pr);
    }
}
