package havudong.baocao.payment;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import havudong.baocao.entity.Order;
import java.math.BigDecimal;
import java.util.TreeMap;
import java.util.Map;

public class VnPayServiceTest {

    @Test
    public void testSignAndValidate() throws Exception {
        VnPayService service = new VnPayService();
        // set secret via reflection for test (simple way)
        java.lang.reflect.Field f = VnPayService.class.getDeclaredField("hashSecret");
        f.setAccessible(true);
        f.set(service, "TEST_SECRET_123");
        // set tmnCode and urls required by new validation
        java.lang.reflect.Field f2 = VnPayService.class.getDeclaredField("tmnCode");
        f2.setAccessible(true);
        f2.set(service, "TESTTMN");
        java.lang.reflect.Field f3 = VnPayService.class.getDeclaredField("paymentUrl");
        f3.setAccessible(true);
        f3.set(service, "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        java.lang.reflect.Field f4 = VnPayService.class.getDeclaredField("returnUrl");
        f4.setAccessible(true);
        f4.set(service, "https://pay.example.com/api/payment/vnpay/return");

        Order o = new Order();
        o.setId(100L);
        o.setFinalAmount(BigDecimal.valueOf(12345));

        String url = service.generatePaymentUrl(o, "127.0.0.1");
        assertNotNull(url);

        // Extract query params
        String query = url.split("\\?")[1];
        Map<String,String> params = new TreeMap<>();
        for (String part: query.split("&")) {
            String[] kv = part.split("=",2);
            params.put(java.net.URLDecoder.decode(kv[0], "UTF-8"), java.net.URLDecoder.decode(kv[1], "UTF-8"));
        }
        assertTrue(params.containsKey("vnp_SecureHash"));

        // validate signature
        boolean ok = service.validateSignature(params);
        assertTrue(ok);
    }
}