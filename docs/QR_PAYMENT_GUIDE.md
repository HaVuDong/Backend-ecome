# Tài liệu Thanh toán QR - Hướng dẫn bảo vệ luận văn

## 1. Tổng quan hệ thống

### 1.1. Mô hình thanh toán QR

```
┌─────────────────┐     1. Đặt hàng + chọn QR     ┌──────────────────┐
│                 │ ─────────────────────────────►│                  │
│   Customer App  │                               │    Backend API   │
│   (React Native)│     2. Trả về QR URL         │   (Spring Boot)  │
│                 │ ◄─────────────────────────────│                  │
└────────┬────────┘                               └────────┬─────────┘
         │                                                 │
         │ 3. Hiển thị QR                                 │
         │    Countdown 5 phút                            │ 4. Scheduler
         │    Polling mỗi 5s                              │    Auto-confirm
         ▼                                                 │    sau 30-60s
┌─────────────────┐                               ┌────────▼─────────┐
│                 │     5. Quét QR & CK          │                  │
│   VietQR Image  │ ─────────────────────────────►│   MB Bank        │
│   (QR Code)     │                               │   STK: 037189928 │
└─────────────────┘                               └──────────────────┘
```

### 1.2. Thành phần hệ thống

| Component | Công nghệ | Chức năng |
|-----------|-----------|-----------|
| Frontend | React Native + Expo | Hiển thị QR, countdown, polling |
| Backend | Spring Boot | Sinh QR, check status, auto-confirm |
| QR Service | VietQR.io API | Sinh hình ảnh QR chuẩn NAPAS |
| Database | MySQL | Lưu trạng thái thanh toán |

## 2. Flow chi tiết

### 2.1. Đặt hàng với QR Payment

```
1. Customer chọn sản phẩm → Thêm vào giỏ hàng
2. Checkout → Chọn "Chuyển khoản QR (MB Bank)"
3. Nhấn "Đặt hàng" → API tạo Order với paymentMethod = QR_TRANSFER
4. Backend:
   - Tạo Order với trạng thái PENDING
   - Sinh mã giao dịch: DH{orderId}_{timestamp}
   - Tạo VietQR URL với số tiền + nội dung CK
   - Lưu qrCodeUrl, qrExpiredAt (NOW + 5 phút)
5. Frontend nhận qrCodeUrl → Hiển thị màn hình QrPaymentScreen
```

### 2.2. Màn hình thanh toán QR

```
┌────────────────────────────────────────┐
│  ←  Thanh toán QR                      │
├────────────────────────────────────────┤
│                                        │
│    ⏱️ Mã QR hết hạn sau: 04:32        │
│                                        │
│    ┌──────────────────────────────┐   │
│    │                              │   │
│    │          [QR CODE]           │   │
│    │                              │   │
│    │     Quét bằng app NH         │   │
│    └──────────────────────────────┘   │
│                                        │
│    📋 Thông tin chuyển khoản          │
│    ────────────────────────────────   │
│    Ngân hàng:  MB Bank (Quân đội)     │
│    Số TK:      037189928    [📋]      │
│    Chủ TK:     SHOP ECOMMERCE         │
│    Số tiền:    500,000 ₫              │
│    Nội dung:   DH123_1699...  [📋]    │
│                                        │
│    📝 Hướng dẫn                        │
│    1. Mở app ngân hàng                 │
│    2. Quét mã QR hoặc nhập thông tin  │
│    3. Xác nhận thanh toán             │
│    4. Đợi hệ thống xác nhận (30-60s)  │
│                                        │
│    🔄 Đang chờ thanh toán...          │
│                                        │
├────────────────────────────────────────┤
│  [  Hủy và chọn phương thức khác  ]   │
└────────────────────────────────────────┘
```

### 2.3. Auto-confirm (Mock cho demo)

```java
// QrPaymentScheduler.java - Chạy mỗi 10 giây
@Scheduled(fixedRate = 10000)
public void autoConfirmPayment() {
    // 1. Tìm đơn hàng QR_TRANSFER + PENDING
    // 2. Đã tạo QR từ 30-60 giây
    // 3. Chưa hết hạn (qrExpiredAt > NOW)
    // 4. Auto chuyển sang PAID
}
```

**Giải thích khi hỏi:**
- Trong thực tế: Ngân hàng gửi webhook khi nhận tiền
- Demo luận văn: Scheduler tự động confirm để mô phỏng
- Random delay 30-60s để tự nhiên hơn

## 3. API Endpoints

### 3.1. Tạo QR Payment

```
POST /api/payments/qr/{orderId}

Response:
{
  "orderId": 123,
  "qrCodeUrl": "https://img.vietqr.io/image/MB-037189928-compact2.png?amount=500000&addInfo=DH123_1699...",
  "transactionId": "DH123_1699876543210",
  "expiredAt": "2024-01-15T10:05:00",
  "expiryMinutes": 5,
  "amount": 500000,
  "bankId": "MB",
  "bankAccount": "037189928",
  "accountName": "SHOP ECOMMERCE"
}
```

### 3.2. Check Status (Polling)

```
GET /api/payments/qr/{orderId}/status

Response:
{
  "orderId": 123,
  "paymentStatus": "PENDING" | "PAID" | "FAILED",
  "isQrExpired": false,
  "qrExpiredAt": "2024-01-15T10:05:00",
  "paidAt": null,
  "amount": 500000
}
```

### 3.3. Hủy QR Payment

```
POST /api/payments/qr/{orderId}/cancel

Response:
{
  "success": true,
  "message": "Đã hủy thanh toán QR"
}
```

## 4. VietQR Integration

### 4.1. Về VietQR

- **VietQR** là hệ thống QR liên ngân hàng của NAPAS
- Hỗ trợ **40+ ngân hàng** tại Việt Nam
- User có thể quét bằng app ngân hàng hoặc ví điện tử

### 4.2. VietQR URL Format

```
https://img.vietqr.io/image/{BANK_ID}-{ACCOUNT}-{TEMPLATE}.png
  ?amount={AMOUNT}
  &addInfo={CONTENT}
  &accountName={ACCOUNT_NAME}
```

**Ví dụ:**
```
https://img.vietqr.io/image/MB-037189928-compact2.png
  ?amount=500000
  &addInfo=DH123_1699876543210
  &accountName=SHOP%20ECOMMERCE
```

### 4.3. Bank ID của các ngân hàng phổ biến

| Bank | Bank ID |
|------|---------|
| MB Bank | MB |
| Vietcombank | VCB |
| Techcombank | TCB |
| BIDV | BIDV |
| Agribank | VBA |
| VPBank | VPB |

## 5. Database Schema

### 5.1. Bảng Orders (mở rộng)

```sql
CREATE TABLE orders (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  -- ... existing columns ...
  
  -- QR Payment columns
  payment_method ENUM('COD', 'QR_TRANSFER') DEFAULT 'COD',
  qr_code_url TEXT,
  qr_expired_at DATETIME,
  payment_transaction_id VARCHAR(100),
  paid_at DATETIME,
  
  -- Status
  payment_status ENUM('PENDING', 'PAID', 'FAILED', 'CANCELLED') DEFAULT 'PENDING',
  
  INDEX idx_payment_transaction_id (payment_transaction_id),
  INDEX idx_payment_status (payment_status)
);
```

### 5.2. PaymentMethod Enum

```java
public enum PaymentMethod {
    COD,            // Thanh toán khi nhận hàng
    QR_TRANSFER     // Chuyển khoản qua QR
}
```

## 6. Bảo mật

### 6.1. Các biện pháp bảo mật đã áp dụng

| Biện pháp | Mô tả |
|-----------|-------|
| QR Expiry | QR chỉ có hiệu lực 5 phút |
| Unique TransactionId | Mỗi đơn có mã giao dịch riêng để đối soát |
| Status Check | Kiểm tra trạng thái trước khi confirm |
| JWT Auth | Tất cả API yêu cầu token hợp lệ |

### 6.2. Production Recommendations

```
1. Webhook từ Payment Gateway:
   - Nhận callback từ ngân hàng khi có giao dịch
   - Verify signature để đảm bảo request hợp lệ
   
2. Đối soát tự động:
   - So sánh nội dung CK với transactionId
   - Kiểm tra số tiền khớp với đơn hàng
   
3. Rate Limiting:
   - Giới hạn số lần tạo QR / user / thời gian
   - Tránh spam tạo QR
```

## 7. Câu hỏi bảo vệ thường gặp

### Q1: Tại sao không tích hợp payment gateway thật?

**Trả lời:**
- Payment gateway thật (VNPAY, Momo) yêu cầu:
  - Đăng ký merchant account
  - Verification business license
  - Integration testing với sandbox
- Với luận văn, mock auto-confirm đủ demo flow
- Kiến trúc đã sẵn sàng để thay webhook thật

### Q2: Làm sao đảm bảo user đã chuyển tiền thật?

**Trả lời:**
- Production: Webhook từ ngân hàng + đối soát transactionId
- Demo: Scheduler mô phỏng việc ngân hàng xác nhận
- Nội dung CK có mã đơn hàng để đối soát

### Q3: Nếu QR hết hạn thì sao?

**Trả lời:**
- Frontend hiển thị thông báo "QR đã hết hạn"
- User có thể:
  1. Tạo QR mới (nhấn "Thử lại")
  2. Đổi sang COD (nhấn "Chọn phương thức khác")
- Backend đánh dấu paymentStatus = FAILED

### Q4: Polling có ảnh hưởng performance không?

**Trả lời:**
- Polling interval: 5 giây (không quá nhanh)
- Max polling time: 5 phút (QR expiry)
- Server side: Query đơn giản theo orderId (indexed)
- Alternative: WebSocket (phức tạp hơn cho demo)

### Q5: Tại sao dùng VietQR thay vì tự sinh QR?

**Trả lời:**
- VietQR là chuẩn quốc gia, được ngân hàng công nhận
- Tự sinh QR phải tuân thủ EMVCo standard
- VietQR tự động format đúng cho từng ngân hàng
- API miễn phí, không cần đăng ký

## 8. Files quan trọng

### Backend (Spring Boot)
- `PaymentMethod.java` - Enum phương thức thanh toán
- `Order.java` - Entity với các trường QR mới
- `PaymentService.java` - Business logic sinh QR, check status
- `PaymentController.java` - REST API endpoints
- `QrPaymentScheduler.java` - Auto-confirm scheduler

### Frontend (React Native)
- `paymentService.ts` - API client cho payment
- `QrPaymentScreen.tsx` - UI màn hình thanh toán QR
- `CheckoutView.tsx` - Cập nhật để tích hợp QR option

## 9. Demo Script

```
1. Mở app Customer → Thêm sản phẩm vào giỏ hàng
2. Checkout → Chọn "Chuyển khoản QR (MB Bank)"
3. Đặt hàng → Chuyển sang màn hình QR
4. Hiển thị:
   - Mã QR (có thể scan thật)
   - Countdown 5 phút
   - Thông tin bank
5. Đợi 30-60 giây → Auto confirm
6. Alert "Thanh toán thành công" → Navigate to orders
7. Kiểm tra order → Status = PAID
```
