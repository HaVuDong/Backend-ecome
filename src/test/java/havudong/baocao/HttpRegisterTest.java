package havudong.baocao;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class HttpRegisterTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void registerEndpoint_returnsErrorBody() {
        String json = "{\"email\":\"live+me6@local.test\",\"password\":\"123456\",\"fullName\":\"Live Test\",\"phone\":\"0367189928\"}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(json, headers);
        ResponseEntity<String> res = restTemplate.postForEntity("/api/auth/register", entity, String.class);
        System.out.println("status=" + res.getStatusCodeValue() + " body=" + res.getBody());
    }
}
