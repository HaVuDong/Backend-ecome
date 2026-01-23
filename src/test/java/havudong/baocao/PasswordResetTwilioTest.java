package havudong.baocao;

import havudong.baocao.BaocaoApplication;
import havudong.baocao.sms.TwilioVerifyService;
import havudong.baocao.dto.RegisterRequest;
import havudong.baocao.dto.ApiResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = BaocaoApplication.class)
@TestPropertySource(properties = "sms.provider=twilio_verify")
@org.springframework.context.annotation.Import(PasswordResetTwilioTest.Config.class)
public class PasswordResetTwilioTest {

    @LocalServerPort
    private int port;

    private RestTemplate rest = new RestTemplate();

    @TestConfiguration
    static class Config {
        static class StubTwilioVerifyService extends TwilioVerifyService {
            private String lastPhone;
            public StubTwilioVerifyService() { super("", "", ""); }
            @Override
            public void sendVerification(String phone) { this.lastPhone = phone; }
            @Override
            public boolean checkVerification(String phone, String code) { return true; }
            public String getLastPhone() { return lastPhone; }
        }

        @Bean
        @org.springframework.context.annotation.Primary
        public TwilioVerifyService twilioVerifyService() {
            return new StubTwilioVerifyService();
        }
    }

    @Autowired
    private TwilioVerifyService twilioVerifyService;

    @Test
    public void twilioRequestAndVerifyFlow() throws Exception {
        String base = "http://localhost:" + port;

        String phone = "0999" + java.util.UUID.randomUUID().toString().replaceAll("[^0-9]", "").substring(0,7);
        // register
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail("u_twilio_" + java.util.UUID.randomUUID() + "@example.com");
        reg.setFullName("User Twilio");
        reg.setPassword("InitialP@ss1");
        reg.setPhone(phone);

        ResponseEntity r1 = rest.postForEntity(base + "/api/auth/register", reg, Object.class);
        Assertions.assertEquals(HttpStatus.CREATED, r1.getStatusCode());

        // request OTP -> Twilio stub will record call
        ResponseEntity<ApiResponse> r2 = rest.postForEntity(base + "/api/auth/forgot/request-otp", java.util.Map.of("phone", phone), ApiResponse.class);
        Assertions.assertEquals(HttpStatus.OK, r2.getStatusCode());

        // assert stub recorded the normalized phone
        Config.StubTwilioVerifyService stub = (Config.StubTwilioVerifyService) twilioVerifyService;
        String expected = havudong.baocao.util.PhoneUtils.normalizeToE164(phone);
        Assertions.assertEquals(expected, stub.getLastPhone());

        // verify otp (the stub returns true)
        ResponseEntity<ApiResponse> r3 = rest.postForEntity(base + "/api/auth/forgot/verify-otp", java.util.Map.of("phone", phone, "otp", "000000"), ApiResponse.class);
        Assertions.assertEquals(HttpStatus.OK, r3.getStatusCode());
        java.util.Map body = (java.util.Map) r3.getBody().getData();
        String resetToken = (String) body.get("resetToken");
        Assertions.assertNotNull(resetToken);
    }
}