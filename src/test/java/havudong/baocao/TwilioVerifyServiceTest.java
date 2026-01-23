package havudong.baocao;

import havudong.baocao.exception.BadRequestException;
import havudong.baocao.sms.TwilioVerifyService;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TwilioVerifyServiceTest {

    @Test
    public void sendVerification_successPending() {
        RestTemplate rt = mock(RestTemplate.class);
        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "pending");
        resp.put("to", "+84367189928");
        when(rt.postForObject(any(String.class), any(), eq(Map.class))).thenReturn(resp);

        TwilioVerifyService svc = new TwilioVerifyService("ACx", "token", "VAx", rt);
        assertDoesNotThrow(() -> svc.sendVerification("+84367189928"));
    }

    @Test
    public void sendVerification_400ThrowsBadRequest() {
        RestTemplate rt = mock(RestTemplate.class);
        String body = "{\"code\":60200,\"message\":\"Invalid parameter `To`: +84367189928\",\"status\":400}";
        when(rt.postForObject(any(String.class), any(), eq(Map.class))).thenThrow(
                HttpClientErrorException.create(org.springframework.http.HttpStatus.BAD_REQUEST, "Bad Request", null, body.getBytes(), null)
        );

        TwilioVerifyService svc = new TwilioVerifyService("ACx", "token", "VAx", rt);
        BadRequestException ex = assertThrows(BadRequestException.class, () -> svc.sendVerification("+84367189928"));
        assertTrue(ex.getMessage().contains("Invalid parameter"));
    }

    @Test
    public void checkVerification_approvedReturnsTrue() {
        RestTemplate rt = mock(RestTemplate.class);
        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "approved");
        when(rt.postForObject(any(String.class), any(), eq(Map.class))).thenReturn(resp);

        TwilioVerifyService svc = new TwilioVerifyService("ACx", "token", "VAx", rt);
        assertTrue(svc.checkVerification("+84367189928", "123456"));
    }

    @Test
    public void checkVerification_400ThrowsBadRequest() {
        RestTemplate rt = mock(RestTemplate.class);
        String body = "{\"code\":60200,\"message\":\"Invalid parameter `To`: +84367189928\",\"status\":400}";
        when(rt.postForObject(any(String.class), any(), eq(Map.class))).thenThrow(
                HttpClientErrorException.create(org.springframework.http.HttpStatus.BAD_REQUEST, "Bad Request", null, body.getBytes(), null)
        );

        TwilioVerifyService svc = new TwilioVerifyService("ACx", "token", "VAx", rt);
        BadRequestException ex = assertThrows(BadRequestException.class, () -> svc.checkVerification("+84367189928", "123456"));
        assertTrue(ex.getMessage().contains("Invalid parameter"));
    }
}
