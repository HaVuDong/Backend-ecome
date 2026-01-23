package havudong.baocao.controller;

import havudong.baocao.dto.ApiResponse;
import havudong.baocao.entity.Order;
import havudong.baocao.entity.enums.PaymentStatus;
import havudong.baocao.payment.VnPayService;
import havudong.baocao.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMethod;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment/vnpay")
@RequiredArgsConstructor
@Slf4j
public class VnPayController {

    private final VnPayService vnPayService;
    private final OrderService orderService;

    record CreateReq(Long orderId) {}

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Map<String, String>>> create(@RequestBody CreateReq req, HttpServletRequest httpRequest) throws Exception {
        Order order = orderService.getOrderById(req.orderId()).orElseThrow(() -> new RuntimeException("Order not found"));
        String ip = httpRequest.getRemoteAddr();
        String url = vnPayService.generatePaymentUrl(order, ip);
        // mark order payment method
        order.setPaymentMethod(havudong.baocao.entity.enums.PaymentMethod.VNPAY);
        orderService.updatePaymentStatus(order.getId(), PaymentStatus.PENDING);
        // save transaction id as txn ref (order id already)
        // return payment url
        Map<String, String> data = new HashMap<>();
        data.put("payment_url", url);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/return")
    public ResponseEntity<String> vnpayReturn(@RequestParam Map<String, String> params, HttpServletRequest httpRequest) throws Exception {
        try {
            log.info("VNPAY return params: {}", params);
            log.debug("VNPAY return params raw: {}", params);
            // Prefer verifying signature using the raw query string to avoid any decoding/encoding mismatches
            String rawQuery = httpRequest.getQueryString();
            String received = params.get("vnp_SecureHash");
            String calc = vnPayService.computeSignatureFromRawQuery(rawQuery);
            String txnRef = params.get("vnp_TxnRef");
            String responseCode = params.get("vnp_ResponseCode");
            String amount = params.get("vnp_Amount");

            if (received == null || !calc.equalsIgnoreCase(received)) {
                // try fallback: compute using decoded params map
                try {
                    String altCalc = vnPayService.computeSignature(params);
                    if (altCalc.equalsIgnoreCase(received)) {
                        log.info("Signature matched using decoded-params fallback");
                    } else {
                        // debugging info
                        String secretPrefix = "n/a";
                        try {
                            java.lang.reflect.Field hf = VnPayService.class.getDeclaredField("hashSecret");
                            hf.setAccessible(true);
                            String s = (String) hf.get(vnPayService);
                            if (s != null) secretPrefix = s.length() > 6 ? s.substring(0, 6) : s;
                        } catch (Exception ignore) {}
                        log.warn("Invalid signature: received={} calculatedRaw={} calculatedDecoded={} secretPrefix={}", received, calc, altCalc, secretPrefix);
                        return ResponseEntity.badRequest().body("Invalid signature: received=" + received + " calculatedRaw=" + calc + " calculatedDecoded=" + altCalc + " secretPrefix=" + secretPrefix + " params=" + params.toString());
                    }
                } catch (Exception e) {
                    log.warn("Invalid signature and failed to compute comparison hash", e);
                    return ResponseEntity.badRequest().body("Invalid signature");
                }
            }
            Long orderId = Long.parseLong(txnRef);
            Order order = orderService.getOrderById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
            // amount is in cents
            long sentAmount = Long.parseLong(amount);
            long orderAmount = order.getFinalAmount().multiply(java.math.BigDecimal.valueOf(100)).longValue();
            if (sentAmount != orderAmount) {
                log.warn("Amount mismatch: sent={} expected={}", sentAmount, orderAmount);
                orderService.updatePaymentStatus(orderId, PaymentStatus.FAILED);
                return ResponseEntity.ok("Amount mismatch");
            }
            if ("00".equals(responseCode)) {
                orderService.updatePaymentStatus(orderId, PaymentStatus.PAID);
                return ResponseEntity.ok("Payment success");
            } else {
                orderService.updatePaymentStatus(orderId, PaymentStatus.FAILED);
                return ResponseEntity.ok("Payment failed");
            }
        } catch (Exception e) {
            log.error("Error handling VNPAY return", e);
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).body("EX:" + e.getMessage());
        }
    }

    @RequestMapping(value = "/ipn", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<String> ipn(@RequestParam Map<String, String> params, HttpServletRequest httpRequest) throws Exception {
        log.info("VNPAY IPN params: {}", params);
        boolean ok = vnPayService.validateSignature(params);
        if (!ok) {
            // fallback: try raw-query based verification (handles encoding differences)
            String raw = httpRequest.getQueryString();
            String calcRaw = vnPayService.computeSignatureFromRawQuery(raw);
            log.warn("IPN invalid signature, fallback computedRaw={}", calcRaw);
            if (!calcRaw.equalsIgnoreCase(params.get("vnp_SecureHash"))) {
                return ResponseEntity.badRequest().body("Invalid signature");
            } else {
                ok = true;
            }
        }
        Long orderId = Long.parseLong(params.get("vnp_TxnRef"));
        String responseCode = params.get("vnp_ResponseCode");
        if ("00".equals(responseCode)) {
            orderService.updatePaymentStatus(orderId, PaymentStatus.PAID);
            return ResponseEntity.ok("OK");
        } else {
            orderService.updatePaymentStatus(orderId, PaymentStatus.FAILED);
            return ResponseEntity.ok("OK");
        }
    }
}
