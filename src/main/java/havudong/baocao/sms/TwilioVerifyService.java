package havudong.baocao.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Small Twilio Verify wrapper using REST API so we don't need the Twilio SDK.
 * Enabled when sms.provider=twilio_verify and required env vars are set.
 */
@Service
@ConditionalOnProperty(name = "sms.provider", havingValue = "twilio_verify")
@Slf4j
public class TwilioVerifyService {

    private String accountSid;
    private String authToken;
    private String verifyServiceSid;
    private final RestTemplate restTemplate;

    // Default constructor to keep bean instantiation robust in dev/local runs
    public TwilioVerifyService() {
        this.accountSid = "";
        this.authToken = "";
        this.verifyServiceSid = "";
        this.restTemplate = new RestTemplate();
    }

    public TwilioVerifyService(
            @Value("${sms.twilio.account-sid:}") String accountSid,
            @Value("${sms.twilio.auth-token:}") String authToken,
            @Value("${sms.twilio.verify-service-sid:}") String verifyServiceSid
    ) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.verifyServiceSid = verifyServiceSid;
        this.restTemplate = new RestTemplate();
    }

    // Constructor for tests (allows injecting a mock RestTemplate)
    public TwilioVerifyService(String accountSid, String authToken, String verifyServiceSid, RestTemplate restTemplate) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.verifyServiceSid = verifyServiceSid;
        this.restTemplate = restTemplate == null ? new RestTemplate() : restTemplate;
    }

    // Load config from environment variables at call time if not already present. This allows env vars set after app start (e.g., from shell session) to be picked up.
    private synchronized void loadConfigFromEnvIfNeeded() {
        if (this.accountSid == null || this.accountSid.isBlank()) {
            String env = System.getenv("SMS_TWILIO_ACCOUNT_SID");
            if (env != null && !env.isBlank()) this.accountSid = env.trim();
        }
        if (this.authToken == null || this.authToken.isBlank()) {
            String env = System.getenv("SMS_TWILIO_AUTH_TOKEN");
            if (env != null && !env.isBlank()) this.authToken = env.trim();
        }
        if (this.verifyServiceSid == null || this.verifyServiceSid.isBlank()) {
            String env = System.getenv("SMS_TWILIO_VERIFY_SERVICE_SID");
            if (env != null && !env.isBlank()) this.verifyServiceSid = env.trim();
        }
    }

    public void sendVerification(String phone) {
        // attempt to load env vars at runtime if constructor injection was empty
        loadConfigFromEnvIfNeeded();
        if (verifyServiceSid == null || verifyServiceSid.isBlank() || accountSid == null || accountSid.isBlank() || authToken == null || authToken.isBlank()) {
            throw new IllegalStateException("Twilio Verify not configured. Set SMS_TWILIO_ACCOUNT_SID, SMS_TWILIO_AUTH_TOKEN and SMS_TWILIO_VERIFY_SERVICE_SID.");
        }
        try {
            String url = String.format("https://verify.twilio.com/v2/Services/%s/Verifications", verifyServiceSid);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBasicAuth(accountSid, authToken);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("To", phone);
            body.add("Channel", "sms");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            Map<String,Object> response = restTemplate.postForObject(url, request, Map.class);

            log.info("Twilio verify send response: {}", response);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            String body = e.getResponseBodyAsString();
            String msg = extractMessageFromJson(body, e.getStatusText());
            log.warn("Twilio client error sending verification: status={} body={}", e.getStatusCode(), body);
            throw new havudong.baocao.exception.BadRequestException(msg);
        } catch (Exception e) {
            log.error("Twilio send error", e);
            throw new RuntimeException("Failed to send verification");
        }
    }

    public boolean checkVerification(String phone, String code) {
        // attempt to load env vars at runtime if constructor injection was empty
        loadConfigFromEnvIfNeeded();
        if (verifyServiceSid == null || verifyServiceSid.isBlank() || accountSid == null || accountSid.isBlank() || authToken == null || authToken.isBlank()) {
            throw new IllegalStateException("Twilio Verify not configured. Set SMS_TWILIO_ACCOUNT_SID, SMS_TWILIO_AUTH_TOKEN and SMS_TWILIO_VERIFY_SERVICE_SID.");
        }
        try {
            String url = String.format("https://verify.twilio.com/v2/Services/%s/VerificationChecks", verifyServiceSid);
            log.info("Calling Twilio Verify check URL: {} for phone {}", url, phone);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBasicAuth(accountSid, authToken);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("To", phone);
            body.add("Code", code);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            Map<String,Object> response = restTemplate.postForObject(url, request, Map.class);

            log.info("Twilio verify check response: {}", response);
            if (response != null && "approved".equalsIgnoreCase(String.valueOf(response.get("status")))) {
                return true;
            }
            return false;
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            String body = e.getResponseBodyAsString();
            String msg = extractMessageFromJson(body, e.getStatusText());
            log.warn("Twilio client error checking verification: status={} body={}", e.getStatusCode(), body);
            throw new havudong.baocao.exception.BadRequestException(msg);
        } catch (Exception e) {
            log.error("Twilio check error", e);
            return false;
        }
    }

    private String extractMessageFromJson(String body, String fallback) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> m = om.readValue(body, java.util.Map.class);
            Object msg = m.get("message");
            if (msg != null) return msg.toString();
            return fallback;
        } catch (Exception e) {
            return fallback;
        }
    }
}
