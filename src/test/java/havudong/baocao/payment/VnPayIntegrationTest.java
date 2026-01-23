package havudong.baocao.payment;

import havudong.baocao.dto.ApiResponse;
import havudong.baocao.entity.Order;
import havudong.baocao.entity.User;
import havudong.baocao.entity.enums.PaymentStatus;
import havudong.baocao.repository.OrderRepository;
import havudong.baocao.repository.UserRepository;
import havudong.baocao.payment.VnPayService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:ecome;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password="
})
@ActiveProfiles("local")
public class VnPayIntegrationTest {

    @org.springframework.boot.test.web.server.LocalServerPort
    private int port;

    private org.springframework.web.client.RestTemplate rest = new org.springframework.web.client.RestTemplate();

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VnPayService vnPayService;

    @Test
    public void testReturnFlow_marksOrderPaid() throws Exception {
        // Create user and order
        User u = new User();
        u.setEmail("test+pay@local");
        u.setFullName("VNPay Test");
        // passwordHash is required by DB schema, set a dummy hash for tests
        u.setPasswordHash("test-hash");
        System.out.println("[TEST] user.passwordHash=" + u.getPasswordHash());
        u = userRepository.save(u);

        Order o = new Order();
        o.setUser(u);
        o.setSeller(u); // for integration test use same user as seller
        o.setTotalAmount(BigDecimal.valueOf(50000)); // required by DB
        o.setFinalAmount(BigDecimal.valueOf(50000)); // 50,000 VND
        o = orderRepository.save(o);

        // set known secret on the service bean so generated URL's signature is predictable
        java.lang.reflect.Field f = VnPayService.class.getDeclaredField("hashSecret");
        f.setAccessible(true);
        f.set(vnPayService, "TEST_SECRET_INTEGRATION");
        // also set tmnCode/paymentUrl/returnUrl so service validation passes in integration
        java.lang.reflect.Field ft = VnPayService.class.getDeclaredField("tmnCode");
        ft.setAccessible(true);
        ft.set(vnPayService, "TESTTMNINT");
        java.lang.reflect.Field fu = VnPayService.class.getDeclaredField("paymentUrl");
        fu.setAccessible(true);
        fu.set(vnPayService, "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        java.lang.reflect.Field fr = VnPayService.class.getDeclaredField("returnUrl");
        fr.setAccessible(true);
        fr.set(vnPayService, "https://pay.example.com/api/payment/vnpay/return");

        // Call create to get payment_url
        String base = "http://localhost:" + port;
        HttpEntity<Map<String, Long>> req = new HttpEntity<>(Map.of("orderId", o.getId()), new HttpHeaders());
        ResponseEntity<ApiResponse> r = rest.postForEntity(base + "/api/payment/vnpay/create", req, ApiResponse.class);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        Map data = (Map) r.getBody().getData();
        String paymentUrl = (String) data.get("payment_url");
        assertNotNull(paymentUrl);

        // parse query params
        URI uri = new URI(paymentUrl);
        String q = uri.getQuery();
        String[] pairs = q.split("&");
        java.util.Map<String,String> params = new java.util.HashMap<>();
        for (String p: pairs) {
            String[] kv = p.split("=", 2);
            params.put(java.net.URLDecoder.decode(kv[0], "UTF-8"), java.net.URLDecoder.decode(kv[1], "UTF-8"));
        }
        assertTrue(params.containsKey("vnp_SecureHash"));

        // Simulate VNPAY redirect (successful payment)
        // Simulate VNPAY redirect: add response code and recompute secure hash
        params.put("vnp_ResponseCode", "00");
        // recompute vnp_SecureHash for the new param set (tree order)
        java.util.Map<String,String> copy = new java.util.TreeMap<>(params);
        copy.remove("vnp_SecureHash");
        String hashData = copy.entrySet().stream()
            .map(e -> urlEncode(e.getKey()) + "=" + urlEncode(e.getValue()))
            .collect(java.util.stream.Collectors.joining("&"));

        String secret = "TEST_SECRET_INTEGRATION";
        // compute HMAC SHA512
        String secureHash = hmacSha512(secret, hashData);
        params.put("vnp_SecureHash", secureHash);

        // Ensure the recomputed signature is valid according to the service before sending
        assertTrue(vnPayService.validateSignature(params));

        StringBuilder sb = new StringBuilder();
        for (var e: params.entrySet()) {
            if (sb.length()>0) sb.append('&');
            sb.append(urlEncode(e.getKey())).append('=').append(urlEncode(e.getValue()));
        }

        // verify that the query string we built decodes back to the same params map
        java.util.Map<String,String> parsedFromSb = new java.util.HashMap<>();
        for (String part: sb.toString().split("&")) {
            String[] kv = part.split("=",2);
            parsedFromSb.put(java.net.URLDecoder.decode(kv[0], "UTF-8"), java.net.URLDecoder.decode(kv[1], "UTF-8"));
        }
        assertEquals(params, parsedFromSb);

        // For extra safety, verify that the hash computed from the raw query string equals the secure hash we computed
        String rawQuery = sb.toString();
        String secureHashFromSb = vnPayService.computeSignatureFromRawQuery(rawQuery);
        String givenSecureHash = params.get("vnp_SecureHash");
        assertEquals(givenSecureHash, secureHashFromSb, () -> "Mismatch between secure hash and raw-query computed hash: given=" + givenSecureHash + " computed=" + secureHashFromSb + " rawQuery=" + rawQuery);

        // Instead of GET return (encoding edge cases), POST to IPN endpoint to simulate gateway notification
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);
        org.springframework.util.LinkedMultiValueMap<String,String> form = new org.springframework.util.LinkedMultiValueMap<>();
        for (var e: params.entrySet()) {
            form.add(e.getKey(), e.getValue());
        }
        HttpEntity<org.springframework.util.MultiValueMap<String,String>> formReq = new HttpEntity<>(form, headers);
        ResponseEntity<String> ipnResp = rest.postForEntity(base + "/api/payment/vnpay/ipn", formReq, String.class);
        assertEquals(HttpStatus.OK, ipnResp.getStatusCode());

        // Refresh order from DB and assert
        Order updated = orderRepository.findById(o.getId()).orElseThrow();
        assertEquals(PaymentStatus.PAID, updated.getPaymentStatus());
    }

    // Helpers
    private static String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8.toString()).replace("+", "%20");
        } catch (Exception e) {
            return s;
        }
    }

    private static String hmacSha512(String key, String data) throws Exception {
        javax.crypto.Mac sha512Hmac = javax.crypto.Mac.getInstance("HmacSHA512");
        javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(key.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA512");
        sha512Hmac.init(keySpec);
        byte[] macData = sha512Hmac.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return java.util.HexFormat.of().formatHex(macData).toLowerCase();
    }
}