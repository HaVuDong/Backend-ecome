package havudong.baocao.payment;

import havudong.baocao.entity.Order;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

public class VnPayServiceEnhancedTest {

    @Test
    public void testGenerateUrl_and_validateSignature() throws Exception {
        VnPayService service = new VnPayService();
        java.lang.reflect.Field f = VnPayService.class.getDeclaredField("hashSecret");
        f.setAccessible(true);
        f.set(service, "TEST_SECRET_456");

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
        o.setId(321L);
        o.setFinalAmount(BigDecimal.valueOf(10000));

        String url = service.generatePaymentUrl(o, "127.0.0.1");
        assertNotNull(url);
        URI uri = new URI(url);
        assertNotNull(uri.getQuery());

        // parse params
        Map<String,String> params = new TreeMap<>();
        for (String part: uri.getQuery().split("&")) {
            String[] kv = part.split("=",2);
            params.put(java.net.URLDecoder.decode(kv[0], "UTF-8"), java.net.URLDecoder.decode(kv[1], "UTF-8"));
        }
        assertTrue(params.containsKey("vnp_SecureHash"));

        // validate signature using service
        assertTrue(service.validateSignature(params));
    }

    @Test
    public void testMissingConfigThrows() {
        VnPayService service = new VnPayService();
        // no tmnCode/hashSecret -> should throw
        Order o = new Order();
        o.setId(1L);
        o.setFinalAmount(BigDecimal.valueOf(100));
        Exception ex = assertThrows(IllegalStateException.class, () -> service.generatePaymentUrl(o, "127.0.0.1"));
        assertTrue(ex.getMessage().contains("VNPAY_TMN_CODE") || ex.getMessage().contains("VNPAY_HASH_SECRET"));
    }
}