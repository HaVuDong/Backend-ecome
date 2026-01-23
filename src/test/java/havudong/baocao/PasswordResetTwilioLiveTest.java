package havudong.baocao;

import havudong.baocao.dto.ApiResponse;
import havudong.baocao.dto.RegisterRequest;
import havudong.baocao.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = BaocaoApplication.class)
@TestPropertySource(properties = "sms.provider=twilio_verify")
public class PasswordResetTwilioLiveTest {

    private final RestTemplate restTemplate = new RestTemplate();

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    private final String phone = "+84367189928"; // your provided number

    @BeforeEach
    public void cleanup() {
        userRepository.findByPhone(phone).ifPresent(u -> userRepository.delete(u));
    }

    @Test
    public void liveTwilioSend_requestOtp_shouldReturnNon500() {
        // register a user with that phone
        RegisterRequest r = new RegisterRequest("test+live@local.test", "123456", "Live Test", phone, null, null);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<RegisterRequest> regReq = new HttpEntity<>(r, headers);
        ResponseEntity<Map> regRes = restTemplate.postForEntity("http://localhost:" + port + "/api/auth/register", regReq, Map.class);
        assertTrue(regRes.getStatusCode().is2xxSuccessful() || regRes.getStatusCode().value() == 409);

        // request OTP
        Map<String, String> body = Map.of("phone", phone);
        HttpEntity<Map<String, String>> otpReq = new HttpEntity<>(body, headers);
        ResponseEntity<String> otpRes = restTemplate.postForEntity("http://localhost:" + port + "/api/auth/forgot/request-otp", otpReq, String.class);

        // Ensure we didn't get 500 (internal error). If twilio rejects number, expect 400 with Twilio message.
        assertNotEquals(500, otpRes.getStatusCode().value());
        System.out.println("Request OTP response: " + otpRes.getStatusCode() + " body=" + otpRes.getBody());
    }
}
