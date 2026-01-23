package havudong.baocao;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class HttpRegisterTest {

    @org.springframework.boot.web.server.LocalServerPort
    private int port;

    private org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();

    @Test
    public void registerEndpoint_returnsErrorBody() {
        String json = "{\"email\":\"live+me6@local.test\",\"password\":\"123456\",\"fullName\":\"Live Test\",\"phone\":\"0367189928\"}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(json, headers);
        String base = "http://localhost:" + port;
        ResponseEntity<String> res = restTemplate.postForEntity(base + "/api/auth/register", entity, String.class);
        System.out.println("status=" + res.getStatusCodeValue() + " body=" + res.getBody());
    }
}
