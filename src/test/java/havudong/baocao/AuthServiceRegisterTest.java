package havudong.baocao;

import havudong.baocao.dto.RegisterRequest;
import havudong.baocao.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class AuthServiceRegisterTest {

    @Autowired
    private AuthService authService;

    @Test
    public void registerShouldShowStack() {
        RegisterRequest r = new RegisterRequest("live+test@local.test", "123456", "Test", "0367189928", null, null);
        try {
            authService.register(r);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
}
