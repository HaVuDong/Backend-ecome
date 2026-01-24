-- Flyway migration: seed demo data for local testing
-- Inserts users (seller, buyer), categories, products and one cart item if they do not already exist

START TRANSACTION;

-- Users: seller and buyer (password for seller/buyer is '123456')
INSERT INTO users (email, password_hash, full_name, role, status, created_at, updated_at)
SELECT 'seller@example.com', '$2a$10$3euPcmQFCiblsZeEu5s7p.3OxJqPLcPvBW/mZw.lXMQ1Gn4o2uqNi', 'Test Seller', 'SELLER', 'ACTIVE', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'seller@example.com');

INSERT INTO users (email, password_hash, full_name, role, status, created_at, updated_at)
SELECT 'buyer@example.com', '$2a$10$3euPcmQFCiblsZeEu5s7p.3OxJqPLcPvBW/mZw.lXMQ1Gn4o2uqNi', 'Test Buyer', 'CUSTOMER', 'ACTIVE', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'buyer@example.com');

-- Categories
INSERT INTO categories (name, icon, description, created_at, updated_at)
SELECT 'Điện thoại', NULL, 'Điện thoại mẫu', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Điện thoại');

INSERT INTO categories (name, icon, description, created_at, updated_at)
SELECT 'Phụ kiện', NULL, 'Phụ kiện điện tử', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Phụ kiện');

INSERT INTO categories (name, icon, description, created_at, updated_at)
SELECT 'Thời trang', NULL, 'Quần áo và phụ kiện thời trang', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Thời trang');

-- Products
INSERT INTO products (seller_id, category_id, name, description, price, original_price, stock, main_image, is_active, created_at, updated_at)
SELECT s.id, c.id, 'Điện thoại Mẫu A', 'Điện thoại mẫu A - cấu hình tốt, pin khỏe', 4990000, 5990000, 15, '/images/product-phone-a.jpg', 1, NOW(), NOW()
FROM users s, categories c
WHERE s.email = 'seller@example.com' AND c.name = 'Điện thoại'
AND NOT EXISTS (SELECT 1 FROM products p WHERE p.name = 'Điện thoại Mẫu A');

INSERT INTO products (seller_id, category_id, name, description, price, stock, main_image, is_active, created_at, updated_at)
SELECT s.id, c.id, 'Tai nghe Mẫu B', 'Tai nghe không dây, chống ồn', 790000, 50, '/images/product-headphone-b.jpg', 1, NOW(), NOW()
FROM users s, categories c
WHERE s.email = 'seller@example.com' AND c.name = 'Phụ kiện'
AND NOT EXISTS (SELECT 1 FROM products p WHERE p.name = 'Tai nghe Mẫu B');

INSERT INTO products (seller_id, category_id, name, description, price, stock, main_image, is_active, created_at, updated_at)
SELECT s.id, c.id, 'Áo Thun Mẫu C', 'Áo thun cotton, form chuẩn', 199000, 100, '/images/product-shirt-c.jpg', 1, NOW(), NOW()
FROM users s, categories c
WHERE s.email = 'seller@example.com' AND c.name = 'Thời trang'
AND NOT EXISTS (SELECT 1 FROM products p WHERE p.name = 'Áo Thun Mẫu C');

-- Cart item for buyer with the first product
INSERT INTO cart_items (user_id, product_id, quantity, price, selected, created_at, updated_at)
SELECT b.id, p.id, 1, p.price, 1, NOW(), NOW()
FROM users b, products p
WHERE b.email = 'buyer@example.com' AND p.name = 'Điện thoại Mẫu A'
AND NOT EXISTS (SELECT 1 FROM cart_items ci WHERE ci.user_id = b.id AND ci.product_id = p.id);

COMMIT;