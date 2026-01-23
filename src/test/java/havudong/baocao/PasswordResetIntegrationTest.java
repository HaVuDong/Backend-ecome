package havudong.baocao;

import havudong.baocao.BaocaoApplication;
import havudong.baocao.sms.DevSmsSender;
import havudong.baocao.dto.RegisterRequest;
import havudong.baocao.dto.AuthResponse;
import havudong.baocao.dto.LoginRequest;
import havudong.baocao.dto.ApiResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = BaocaoApplication.class)
@TestPropertySource(properties = "sms.provider=dev")
public class PasswordResetIntegrationTest {

    @LocalServerPort
    private int port;

    private RestTemplate rest = new RestTemplate();

    @Test
    public void requestVerifyResetFlow() throws Exception {
        String base = "http://localhost:" + port;

        String phone = "0999" + java.util.UUID.randomUUID().toString().replaceAll("[^0-9]", "").substring(0,7);
        // register
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail("u_for_reset_" + java.util.UUID.randomUUID() + "@example.com");
        reg.setFullName("User Reset");
        reg.setPassword("InitialP@ss1");
        reg.setPhone(phone);

        ResponseEntity<AuthResponse> r1 = rest.postForEntity(base + "/api/auth/register", reg, AuthResponse.class);
        Assertions.assertEquals(HttpStatus.CREATED, r1.getStatusCode());

        // request OTP
        ResponseEntity<ApiResponse> r2 = rest.postForEntity(base + "/api/auth/forgot/request-otp", java.util.Map.of("phone", phone), ApiResponse.class);
        Assertions.assertEquals(HttpStatus.OK, r2.getStatusCode());

        // grab OTP from DevSmsSender (phone stored normalized)
        String normalizedPhone = havudong.baocao.util.PhoneUtils.normalizeToE164(phone);
        String msg = DevSmsSender.getLastMessageForPhone(normalizedPhone);
        Assertions.assertNotNull(msg, "No SMS captured for phone: " + normalizedPhone);
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{6})").matcher(msg);
        Assertions.assertTrue(m.find(), "No 6-digit OTP found in message: " + msg);
        String otp = m.group(1);

        // verify otp
        ResponseEntity<ApiResponse> r3 = rest.postForEntity(base + "/api/auth/forgot/verify-otp", java.util.Map.of("phone", phone, "otp", otp), ApiResponse.class);
        Assertions.assertEquals(HttpStatus.OK, r3.getStatusCode());
        java.util.Map body = (java.util.Map) r3.getBody().getData();
        String resetToken = (String) body.get("resetToken");
        Assertions.assertNotNull(resetToken);

        // reset password
        ResponseEntity<ApiResponse> r4 = rest.postForEntity(base + "/api/auth/forgot/reset", java.util.Map.of("resetToken", resetToken, "password", "NewPass123"), ApiResponse.class);
        Assertions.assertEquals(HttpStatus.OK, r4.getStatusCode());

        // login with new password
        LoginRequest lr = new LoginRequest(); lr.setEmail("u_for_reset@example.com"); lr.setPassword("NewPass123");
        ResponseEntity<AuthResponse> r5 = rest.postForEntity(base + "/api/auth/login", lr, AuthResponse.class);
        Assertions.assertEquals(HttpStatus.OK, r5.getStatusCode());
    }
}