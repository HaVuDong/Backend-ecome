-- Seed minimal data: 1 seller, 2 categories, 5 products
-- Run in your local MySQL (use database ecome)

USE ecome;

-- 1) Insert seller if not exists
INSERT INTO users (email, password_hash, full_name, phone, address, avatar_url, role, status, created_at, updated_at)
SELECT 'local-seller@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.M0TIWr1n5pWdK4GiGi', 'Local Seller', '0912345678', 'Local Address', 'https://i.pravatar.cc/150?img=5', 'SELLER', 'ACTIVE', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'local-seller@example.com');

-- get seller id
SET @seller_id = (SELECT id FROM users WHERE email = 'local-seller@example.com' LIMIT 1);

-- 2) Insert categories if not exists
INSERT INTO categories (name, description, icon, created_at, updated_at)
SELECT 'Điện tử', 'Sản phẩm điện tử và phụ kiện', 'phone', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Điện tử');

INSERT INTO categories (name, description, icon, created_at, updated_at)
SELECT 'Phụ kiện', 'Phụ kiện điện thoại và laptop', 'accessory', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Phụ kiện');

SET @cat_dientu = (SELECT id FROM categories WHERE name = 'Điện tử' LIMIT 1);
SET @cat_phukien = (SELECT id FROM categories WHERE name = 'Phụ kiện' LIMIT 1);

-- 3) Insert 5 products (mix categories)
-- Product 1
INSERT INTO products (seller_id, category_id, name, description, price, original_price, stock, main_image, rating, sold_count, is_active, created_at, updated_at)
SELECT @seller_id, @cat_dientu, 'Tai nghe Bluetooth XYZ', 'Tai nghe Bluetooth XYZ, chống ồn, pin 30 giờ', 499000, 599000, 100, 'https://via.placeholder.com/300x300.png?text=Tai+nghe+XYZ', 4.6, 10, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Tai nghe Bluetooth XYZ' AND seller_id = @seller_id);

-- Product 2
INSERT INTO products (seller_id, category_id, name, description, price, original_price, stock, main_image, rating, sold_count, is_active, created_at, updated_at)
SELECT @seller_id, @cat_dientu, 'Sạc Dự Phòng 10000mAh', 'Sạc dự phòng 10000mAh, sạc nhanh PD', 299000, 399000, 50, 'https://via.placeholder.com/300x300.png?text=Pin+10000', 4.5, 5, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Sạc Dự Phòng 10000mAh' AND seller_id = @seller_id);

-- Product 3
INSERT INTO products (seller_id, category_id, name, description, price, original_price, stock, main_image, rating, sold_count, is_active, created_at, updated_at)
SELECT @seller_id, @cat_phukien, 'Ốp Lưng Silicone iPhone', 'Ốp lưng silicone mềm, chống sốc cho iPhone', 99000, 129000, 200, 'https://via.placeholder.com/300x300.png?text=Op+lung+iPhone', 4.4, 20, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Ốp Lưng Silicone iPhone' AND seller_id = @seller_id);

-- Product 4
INSERT INTO products (seller_id, category_id, name, description, price, original_price, stock, main_image, rating, sold_count, is_active, created_at, updated_at)
SELECT @seller_id, @cat_phukien, 'Cáp Sạc USB-C to Lightning', 'Cáp sạc nhanh USB-C to Lightning, 1m', 129000, 179000, 150, 'https://via.placeholder.com/300x300.png?text=Cap+USB-C', 4.7, 35, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Cáp Sạc USB-C to Lightning' AND seller_id = @seller_id);

-- Product 5
INSERT INTO products (seller_id, category_id, name, description, price, original_price, stock, main_image, rating, sold_count, is_active, created_at, updated_at)
SELECT @seller_id, @cat_dientu, 'Camera Hành Trình Mini', 'Camera hành trình mini FullHD, góc rộng 170 độ', 799000, 999000, 30, 'https://via.placeholder.com/300x300.png?text=Camera+Mini', 4.3, 7, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Camera Hành Trình Mini' AND seller_id = @seller_id);

-- Summary
SELECT 'Seed complete' AS status, @seller_id AS seller_id, @cat_dientu AS cat_dientu, @cat_phukien AS cat_phukien;
