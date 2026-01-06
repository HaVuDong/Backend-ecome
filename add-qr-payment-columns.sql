-- Migration: Add QR Payment columns to orders table
-- Date: 2024
-- Description: Thêm các cột cần thiết cho thanh toán QR

-- ============================================
-- HƯỚNG DẪN KHI BẢO VỆ:
-- 
-- 1. File này dùng để mở rộng bảng orders hỗ trợ thanh toán QR
-- 2. Các cột mới:
--    - qr_code_url: URL hình ảnh QR từ VietQR
--    - qr_expired_at: Thời điểm QR hết hạn (5 phút)
--    - payment_transaction_id: Mã giao dịch unique
--    - paid_at: Thời điểm thanh toán thành công
-- 
-- 3. Nếu dùng spring.jpa.hibernate.ddl-auto=update
--    thì không cần chạy file này, Hibernate sẽ tự thêm cột
-- ============================================

-- Kiểm tra và thêm cột qr_code_url
ALTER TABLE orders ADD COLUMN IF NOT EXISTS qr_code_url TEXT;

-- Kiểm tra và thêm cột qr_expired_at  
ALTER TABLE orders ADD COLUMN IF NOT EXISTS qr_expired_at DATETIME;

-- Kiểm tra và thêm cột payment_transaction_id
ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_transaction_id VARCHAR(100);

-- Kiểm tra và thêm cột paid_at
ALTER TABLE orders ADD COLUMN IF NOT EXISTS paid_at DATETIME;

-- Cập nhật cột payment_method từ VARCHAR sang ENUM nếu cần
-- (Tùy chọn - có thể giữ nguyên VARCHAR)
-- ALTER TABLE orders MODIFY COLUMN payment_method ENUM('COD', 'QR_TRANSFER');

-- Index cho tìm kiếm nhanh
CREATE INDEX IF NOT EXISTS idx_orders_payment_transaction_id ON orders(payment_transaction_id);
CREATE INDEX IF NOT EXISTS idx_orders_payment_status ON orders(payment_status);
CREATE INDEX IF NOT EXISTS idx_orders_qr_expired_at ON orders(qr_expired_at);

-- ============================================
-- SAMPLE DATA (Optional - for testing)
-- ============================================

-- Test QR Payment với order ID = 1
-- UPDATE orders SET 
--     payment_method = 'QR_TRANSFER',
--     qr_code_url = 'https://img.vietqr.io/image/MB-037189928-compact2.png?amount=100000&addInfo=TEST_ORDER',
--     qr_expired_at = DATE_ADD(NOW(), INTERVAL 5 MINUTE),
--     payment_transaction_id = 'DH1_1234567890'
-- WHERE id = 1;
