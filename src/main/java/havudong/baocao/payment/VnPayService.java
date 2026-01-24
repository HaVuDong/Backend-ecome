package havudong.baocao.payment;

import havudong.baocao.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VnPayService {

    @Value("${VNPAY_TMN_CODE:}")
    private String tmnCode;

    @Value("${VNPAY_HASH_SECRET:}")
    private String hashSecret;

    @Value("${VNPAY_SANDBOX_PAYMENT_URL:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String paymentUrl;

    @Value("${VNPAY_RETURN_URL:}")
    private String returnUrl;

    public String generatePaymentUrl(Order order, String ipAddr) throws Exception {
        // Basic configuration checks
        if (tmnCode == null || tmnCode.isBlank()) throw new IllegalStateException("VNPAY_TMN_CODE is not configured");
        if (hashSecret == null || hashSecret.isBlank()) throw new IllegalStateException("VNPAY_HASH_SECRET is not configured");
        if (paymentUrl == null || paymentUrl.isBlank()) throw new IllegalStateException("VNPAY_PAYMENT_URL is not configured");

        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", tmnCode);
        params.put("vnp_Amount", String.valueOf(order.getFinalAmount().multiply(java.math.BigDecimal.valueOf(100)).longValue()));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", String.valueOf(order.getId()));
        params.put("vnp_OrderInfo", "Payment for order " + order.getId());
        params.put("vnp_OrderType", "other");
        params.put("vnp_ReturnUrl", returnUrl);
        params.put("vnp_Locale", "vn");
        params.put("vnp_CreateDate", java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        if (ipAddr != null) params.put("vnp_IpAddr", ipAddr);

        // Filter out empty/null parameters (VNPay expects only present params)
        // Build parameters and compute hash using the same algorithm/encoding as the sample implementation
        java.util.List<String> fieldNames = new java.util.ArrayList<>(params.keySet());
        java.util.Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            String fieldValue = params.get(fieldName);
            if (fieldValue != null && fieldValue.length() > 0) {
                // build hashData using URLEncoder (do NOT replace '+' with '%20')
                hashData.append(fieldName).append('=')
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                // build query
                query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8.toString()))
                        .append('=')
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));

                if (i < fieldNames.size() - 1) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        String queryUrl = query.toString();
        String secureHash = hmacSHA512(hashSecret, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + secureHash;

        String finalUrl = paymentUrl + "?" + queryUrl;

        // Avoid logging secrets — mask the secure hash when logging
        String maskedUrl = finalUrl.replaceAll("(?i)vnp_SecureHash=[^&]*", "vnp_SecureHash=***");
        log.info("Generated VNPAY URL: {}", maskedUrl);
        // For debugging you can set a vnpay.debug flag to log hashData/secureHash if needed
        return finalUrl;
    }

    public boolean validateSignature(Map<String, String> params) throws Exception {
        String receivedHash = params.get("vnp_SecureHash");
        if (receivedHash == null) return false;
        String calculated = computeSignature(params);
        return calculated.equalsIgnoreCase(receivedHash);
    }

    // Compute the signature string for given params (used by validateSignature and for debugging)
    public String computeSignature(Map<String, String> params) throws Exception {
        Map<String, String> copy = params.entrySet().stream()
                .filter(e -> e.getKey() != null && !"vnp_SecureHash".equals(e.getKey()) && !"vnp_SecureHashType".equals(e.getKey()))
                .filter(e -> e.getValue() != null && !e.getValue().isBlank())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a,b)->a, TreeMap::new));
        String hashData = copy.entrySet().stream()
                .map(e -> urlEncode(e.getKey()) + "=" + urlEncode(e.getValue()))
                .collect(Collectors.joining("&"));
        return hmacSHA512(hashSecret, hashData);
    }

    // Compute signature using the raw query string (useful when handling gateway redirect that preserves encoding)
    public String computeSignatureFromRawQuery(String rawQuery) throws Exception {
        if (rawQuery == null || rawQuery.isBlank()) return hmacSHA512(hashSecret, "");
        Map<String, String> copy = java.util.Arrays.stream(rawQuery.split("&"))
                .map(s -> s.split("=", 2))
                .filter(kv -> kv.length == 2 && kv[0] != null && !"vnp_SecureHash".equals(kv[0]) && !"vnp_SecureHashType".equals(kv[0]) && kv[1] != null && !kv[1].isBlank())
                .collect(Collectors.toMap(kv -> java.net.URLDecoder.decode(kv[0], java.nio.charset.StandardCharsets.UTF_8), kv -> java.net.URLDecoder.decode(kv[1], java.nio.charset.StandardCharsets.UTF_8), (a,b)->a, TreeMap::new));

        String hashData = copy.entrySet().stream()
                .map(e -> urlEncode(e.getKey()) + "=" + urlEncode(e.getValue()))
                .collect(Collectors.joining("&"));
        return hmacSHA512(hashSecret, hashData);
    }

    private static String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, StandardCharsets.UTF_8.toString()).replace("+", "%20");
        } catch (Exception e) {
            return s;
        }
    }

    private static String hmacSHA512(String key, String data) throws Exception {
        if (key == null || key.isBlank()) throw new IllegalStateException("HMAC secret is not set");
        if (data == null) data = "";
        Mac sha512Hmac = Mac.getInstance("HmacSHA512");
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        sha512Hmac.init(keySpec);
        byte[] macData = sha512Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return java.util.HexFormat.of().formatHex(macData).toLowerCase();
    }
}
