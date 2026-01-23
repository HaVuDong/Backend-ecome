package havudong.baocao.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
@ConditionalOnProperty(name = "sms.provider", havingValue = "dev", matchIfMissing = true)
@Slf4j
public class DevSmsSender implements SmsSender {

    private static final ConcurrentHashMap<String, String> LAST_MESSAGES = new ConcurrentHashMap<>();

    @Override
    public void sendSms(String phone, String message) {
        LAST_MESSAGES.put(phone, message);
        log.info("[DevSmsSender] sendSms to {}: {}", phone, message);
    }

    // Helper for tests
    public static String getLastMessageForPhone(String phone) {
        return LAST_MESSAGES.get(phone);
    }

    public static void clear() {
        LAST_MESSAGES.clear();
    }
}
