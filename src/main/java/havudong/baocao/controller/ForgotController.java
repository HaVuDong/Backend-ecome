package havudong.baocao.controller;

import havudong.baocao.dto.ApiResponse;
import havudong.baocao.service.ForgotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/forgot")
@RequiredArgsConstructor
public class ForgotController {

    private final ForgotService forgotService;
    private final PasswordEncoder passwordEncoder;

    record RequestOtpReq(String phone) {}
    record VerifyOtpReq(String phone, String otp) {}
    record ResetReq(String resetToken, String password) {}

    @PostMapping("/request-otp")
    public ResponseEntity<ApiResponse<Object>> requestOtp(@RequestBody @Valid RequestOtpReq req) {
        forgotService.requestOtp(req.phone());
        return ResponseEntity.ok(ApiResponse.success("OTP sent"));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Map<String,String>>> verifyOtp(@RequestBody @Valid VerifyOtpReq req) {
        String token = forgotService.verifyOtpAndCreateToken(req.phone(), req.otp());
        return ResponseEntity.ok(ApiResponse.success(Map.of("resetToken", token)));
    }

    @PostMapping("/reset")
    public ResponseEntity<ApiResponse<Object>> reset(@RequestBody @Valid ResetReq req) {
        forgotService.resetPassword(req.resetToken(), req.password(), passwordEncoder);
        return ResponseEntity.ok(ApiResponse.success("Password reset"));
    }
}
