-- =====================================================
-- SCRIPT KHỞI TẠO DỮ LIỆU MẪU CHO ECOME DATABASE
-- Chạy script này trong phpMyAdmin hoặc MySQL Workbench
-- =====================================================

USE ecome_db;

-- =====================================================
-- 1. THÊM USERS (Seller và Customer)
-- =====================================================
-- Password: 123456 (BCrypt encoded)
INSERT INTO users (email, password_hash, full_name, phone, address, avatar_url, role, status, created_at, updated_at) VALUES
('seller1@gmail.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.M0TIWr1n5pWdK4GiGi', 'Shop Điện Tử ABC', '0901234567', '123 Nguyễn Huệ, Q.1, TP.HCM', 'https://i.pravatar.cc/150?img=1', 'SELLER', 'ACTIVE', NOW(), NOW()),
('seller2@gmail.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.M0TIWr1n5pWdK4GiGi', 'Thời Trang XYZ', '0902345678', '456 Lê Lợi, Q.1, TP.HCM', 'https://i.pravatar.cc/150?img=2', 'SELLER', 'ACTIVE', NOW(), NOW()),
('seller3@gmail.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.M0TIWr1n5pWdK4GiGi', 'Shop Gia Dụng 123', '0903456789', '789 Trần Hưng Đạo, Q.5, TP.HCM', 'https://i.pravatar.cc/150?img=3', 'SELLER', 'ACTIVE', NOW(), NOW()),
('customer1@gmail.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.M0TIWr1n5pWdK4GiGi', 'Nguyễn Văn An', '0911111111', '100 Điện Biên Phủ, Q.Bình Thạnh, TP.HCM', 'https://i.pravatar.cc/150?img=10', 'CUSTOMER', 'ACTIVE', NOW(), NOW()),
('customer2@gmail.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.M0TIWr1n5pWdK4GiGi', 'Trần Thị Bình', '0922222222', '200 Cách Mạng Tháng 8, Q.3, TP.HCM', 'https://i.pravatar.cc/150?img=11', 'CUSTOMER', 'ACTIVE', NOW(), NOW()),
('admin@gmail.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.M0TIWr1n5pWdK4GiGi', 'Admin System', '0900000000', 'TP.HCM', 'https://i.pravatar.cc/150?img=20', 'ADMIN', 'ACTIVE', NOW(), NOW());

-- =====================================================
-- 2. THÊM CATEGORIES (Danh mục sản phẩm)
-- =====================================================
INSERT INTO categories (name, description, icon, created_at, updated_at) VALUES
('Điện thoại', 'Điện thoại di động và phụ kiện', 'phone', NOW(), NOW()),
('Laptop', 'Máy tính xách tay và phụ kiện', 'laptop', NOW(), NOW()),
('Thời trang Nam', 'Quần áo, giày dép nam', 'fashion', NOW(), NOW()),
('Thời trang Nữ', 'Quần áo, giày dép nữ', 'fashion', NOW(), NOW()),
('Đồ gia dụng', 'Thiết bị gia đình', 'home', NOW(), NOW()),
('Làm đẹp', 'Mỹ phẩm và chăm sóc da', 'beauty', NOW(), NOW()),
('Thể thao', 'Dụng cụ và trang phục thể thao', 'sports', NOW(), NOW()),
('Sách', 'Sách và văn phòng phẩm', 'books', NOW(), NOW()),
('Thực phẩm', 'Đồ ăn và thức uống', 'food', NOW(), NOW()),
('Đồ chơi', 'Đồ chơi trẻ em', 'toys', NOW(), NOW());

-- =====================================================
-- 3. THÊM PRODUCTS (Sản phẩm)
-- =====================================================
INSERT INTO products (name, description, price, original_price, stock, main_image, rating, sold_count, is_active, seller_id, category_id, created_at, updated_at) VALUES
-- Điện thoại (Category 1, Seller 1)
('iPhone 15 Pro Max 256GB', 'Điện thoại Apple iPhone 15 Pro Max chính hãng VN/A', 34990000, 36990000, 50, 'https://cdn.tgdd.vn/Products/Images/42/305658/iphone-15-pro-max-blue-thumbnew-600x600.jpg', 4.9, 1250, 1, 1, 1, NOW(), NOW()),
('Samsung Galaxy S24 Ultra', 'Samsung Galaxy S24 Ultra 256GB chính hãng', 31990000, 33990000, 45, 'https://cdn.tgdd.vn/Products/Images/42/307174/samsung-galaxy-s24-ultra-grey-thumbnew-600x600.jpg', 4.8, 980, 1, 1, 1, NOW(), NOW()),
('Xiaomi 14 Ultra', 'Xiaomi 14 Ultra 512GB - Camera Leica', 24990000, 26990000, 30, 'https://cdn.tgdd.vn/Products/Images/42/320721/xiaomi-14-ultra-black-thumb-600x600.jpg', 4.7, 560, 1, 1, 1, NOW(), NOW()),
('OPPO Find X7 Ultra', 'OPPO Find X7 Ultra 16GB/512GB', 22990000, 24990000, 25, 'https://cdn.tgdd.vn/Products/Images/42/329938/oppo-find-x7-ultra-xanh-thumb-600x600.jpg', 4.6, 320, 1, 1, 1, NOW(), NOW()),
('Vivo X100 Pro', 'Vivo X100 Pro 5G 256GB', 19990000, 21990000, 35, 'https://cdn.tgdd.vn/Products/Images/42/318228/vivo-x100-pro-xanh-thumb-1-600x600.jpg', 4.5, 450, 1, 1, 1, NOW(), NOW()),

-- Laptop (Category 2, Seller 1)
('MacBook Pro 14 M3 Pro', 'MacBook Pro 14 inch M3 Pro 18GB/512GB', 49990000, 52990000, 20, 'https://cdn.tgdd.vn/Products/Images/44/318235/apple-macbook-pro-14-inch-m3-pro-2023-den-1-600x600.jpg', 4.9, 380, 1, 1, 2, NOW(), NOW()),
('Dell XPS 15', 'Dell XPS 15 9530 i7-13700H/16GB/512GB', 42990000, 45990000, 15, 'https://cdn.tgdd.vn/Products/Images/44/309623/dell-xps-15-9530-i7-9530slv-thumb-600x600.jpg', 4.8, 220, 1, 1, 2, NOW(), NOW()),
('ASUS ROG Zephyrus G14', 'ASUS ROG Zephyrus G14 Ryzen 9/RTX 4060', 38990000, 41990000, 18, 'https://cdn.tgdd.vn/Products/Images/44/321880/asus-rog-zephyrus-g14-r9-ga403uv-thumb-600x600.jpg', 4.7, 180, 1, 1, 2, NOW(), NOW()),
('Lenovo ThinkPad X1 Carbon', 'ThinkPad X1 Carbon Gen 11 i7/16GB/512GB', 35990000, 38990000, 12, 'https://cdn.tgdd.vn/Products/Images/44/320688/lenovo-thinkpad-x1-carbon-gen-11-i7-21hm004vvn-thumb-600x600.jpg', 4.8, 150, 1, 1, 2, NOW(), NOW()),

-- Thời trang Nam (Category 3, Seller 2)
('Áo Polo Nam Owen', 'Áo Polo nam Owen cotton cao cấp', 450000, 550000, 200, 'https://owen.cdn.vccloud.vn/media/catalog/product/cache/01755127bd64f5dde3182fd2f139143a/a/p/apv231287.1.jpg', 4.6, 2500, 1, 2, 3, NOW(), NOW()),
('Quần Jean Nam Routine', 'Quần Jean nam Routine dáng slim fit', 650000, 750000, 150, 'https://routine.vn/media/amasty/webp/catalog/product/cache/5b5632a96960191c63a51c70c4ae2c96/q/j/qjd011_-_xanh_dam_1__1_jpg.webp', 4.5, 1800, 1, 2, 3, NOW(), NOW()),
('Giày Sneaker Bitis Hunter', 'Giày thể thao Bitis Hunter Street', 850000, 950000, 100, 'https://product.hstatic.net/1000230642/product/hsm004500xdg__1__6b03ad19e9e54f38bb9ca2dff1bf6ddf_1024x1024.jpg', 4.7, 3200, 1, 2, 3, NOW(), NOW()),

-- Thời trang Nữ (Category 4, Seller 2)
('Váy Đầm Nữ Elise', 'Váy đầm công sở Elise thanh lịch', 890000, 1090000, 80, 'https://elise.vn/media/catalog/product/cache/c80e2dea5a88d3fe4ae0ac3dbd75db73/e/s/ess23d092-dbe_4_.jpg', 4.8, 1500, 1, 2, 4, NOW(), NOW()),
('Áo Kiểu Nữ IVY Moda', 'Áo kiểu nữ IVY Moda phong cách Hàn Quốc', 550000, 650000, 120, 'https://ivymoda.com/wp-content/uploads/2023/03/MS-94D3012-1-600x900.jpg', 4.6, 2100, 1, 2, 4, NOW(), NOW()),
('Túi Xách Nữ Charles Keith', 'Túi xách nữ Charles & Keith cao cấp', 1290000, 1490000, 50, 'https://charleskeith.vn/cdn/shop/products/CK2-50782021-2-01.jpg', 4.7, 890, 1, 2, 4, NOW(), NOW()),

-- Đồ gia dụng (Category 5, Seller 3)
('Nồi chiên không dầu Philips', 'Nồi chiên không dầu Philips HD9650 XXL', 4990000, 5490000, 40, 'https://cdn.tgdd.vn/Products/Images/1990/229306/philips-hd9650-600x600.jpg', 4.8, 1200, 1, 3, 5, NOW(), NOW()),
('Robot hút bụi Ecovacs', 'Robot hút bụi lau nhà Ecovacs Deebot T20 Omni', 12990000, 14990000, 25, 'https://cdn.tgdd.vn/Products/Images/1992/304597/ecovacs-deebot-t20-omni-thumb-600x600.jpg', 4.7, 650, 1, 3, 5, NOW(), NOW()),
('Máy lọc không khí Xiaomi', 'Máy lọc không khí Xiaomi 4 Lite', 2990000, 3290000, 60, 'https://cdn.tgdd.vn/Products/Images/1993/271556/xiaomi-smart-air-purifier-4-lite-thumb-600x600.jpg', 4.6, 980, 1, 3, 5, NOW(), NOW()),
('Máy xay sinh tố Panasonic', 'Máy xay sinh tố Panasonic MX-V310KRA', 1590000, 1790000, 80, 'https://cdn.tgdd.vn/Products/Images/1985/228131/panasonic-mx-v310kra-1-org.jpg', 4.5, 2200, 1, 3, 5, NOW(), NOW()),

-- Làm đẹp (Category 6, Seller 2)
('Serum Vitamin C La Roche-Posay', 'La Roche-Posay Pure Vitamin C10 30ml', 890000, 990000, 100, 'https://hasaki.vn/images/products/sku/1599726138_1.jpg', 4.8, 3500, 1, 2, 6, NOW(), NOW()),
('Kem chống nắng Anessa', 'Anessa Perfect UV Sunscreen Skincare Milk', 650000, 750000, 150, 'https://hasaki.vn/images/products/sku/1650252548_1.jpg', 4.9, 5200, 1, 2, 6, NOW(), NOW()),
('Son môi MAC', 'Son thỏi MAC Matte Lipstick', 590000, 650000, 200, 'https://product.hstatic.net/200000259653/product/son-thoi-mac-matte-lipstick-chili-622-do-gach_f3e5c6aba8eb4f2b8e4d5a2caa4e9b58_master.jpg', 4.7, 4100, 1, 2, 6, NOW(), NOW()),

-- Thể thao (Category 7, Seller 3)
('Bóng đá Động Lực UCV 3.05', 'Quả bóng đá Động Lực UCV 3.05 số 5', 350000, 420000, 100, 'https://product.hstatic.net/1000061481/product/bong-da-dong-luc-ucv-3-05_77cd5a9d03b945a3ab9f2dab9c7c9ee5_master.jpg', 4.5, 1800, 1, 3, 7, NOW(), NOW()),
('Vợt cầu lông Yonex', 'Vợt cầu lông Yonex Astrox 88D Pro', 3890000, 4290000, 30, 'https://shopvnb.com/uploads/gallery/vot-cau-long-yonex-astrox-88d-pro-2024-1.webp', 4.8, 650, 1, 3, 7, NOW(), NOW()),
('Bộ tạ tay 10kg', 'Bộ tạ tay cao su 10kg/cặp', 450000, 550000, 80, 'https://bizweb.dktcdn.net/100/438/408/products/ta-tay-cao-su-6kg.jpg', 4.4, 1200, 1, 3, 7, NOW(), NOW()),

-- Sách (Category 8, Seller 3)
('Đắc Nhân Tâm', 'Đắc Nhân Tâm - Dale Carnegie (Bìa cứng)', 120000, 150000, 300, 'https://salt.tikicdn.com/cache/750x750/ts/product/08/5a/a1/93da5c44aab9be4e7e3d5c78722d7cc1.jpg', 4.9, 12500, 1, 3, 8, NOW(), NOW()),
('Nhà Giả Kim', 'Nhà Giả Kim - Paulo Coelho', 79000, 99000, 250, 'https://salt.tikicdn.com/cache/750x750/ts/product/45/3b/fc/aa81d0a534b45706ae1eee1e344e80d9.jpg', 4.8, 9800, 1, 3, 8, NOW(), NOW()),
('Atomic Habits', 'Atomic Habits - Thói quen nguyên tử', 189000, 229000, 200, 'https://salt.tikicdn.com/cache/750x750/ts/product/22/cb/a9/524a27dcd45e8a13ae6eecf0eaa8e299.jpg', 4.9, 7500, 1, 3, 8, NOW(), NOW()),

-- Thực phẩm (Category 9, Seller 3)
('Cà phê G7 3in1', 'Cà phê hòa tan G7 3in1 hộp 21 gói', 65000, 75000, 500, 'https://cdn.tgdd.vn/Products/Images/2563/195686/bhx/ca-phe-sua-hoa-tan-g7-3-trong-1-hop-21-goi-x-16g-202208300844195785.jpg', 4.5, 8900, 1, 3, 9, NOW(), NOW()),
('Bánh Oreo', 'Bánh quy Oreo socola kem vani 266.2g', 45000, 55000, 400, 'https://cdn.tgdd.vn/Products/Images/3302/195715/bhx/banh-quy-nhan-kem-socola-va-kem-vani-oreo-hop-2662g-202103221650447117.jpg', 4.6, 6500, 1, 3, 9, NOW(), NOW()),
('Mì Hảo Hảo tôm chua cay', 'Thùng 30 gói mì Hảo Hảo tôm chua cay 75g', 115000, 130000, 300, 'https://cdn.tgdd.vn/Products/Images/2565/230308/bhx/thung-30-goi-mi-hao-hao-huong-vi-tom-chua-cay-75g-202201110955049550.jpg', 4.7, 15200, 1, 3, 9, NOW(), NOW()),

-- Đồ chơi (Category 10, Seller 3)
('LEGO City Police', 'LEGO City Police Station 60316', 1890000, 2190000, 30, 'https://salt.tikicdn.com/cache/750x750/ts/product/c3/51/cd/9fab5e02b0c5c17bf1b0a0d3bf4d46c6.jpg', 4.8, 450, 1, 3, 10, NOW(), NOW()),
('Rubik 3x3 GAN', 'Rubik 3x3 GAN 356 M Lite', 350000, 420000, 60, 'https://rubik.com.vn/storage/photos/1/GAN/GAN%20356%20M%20Lite/gan-356-m-lite-1.jpg', 4.7, 890, 1, 3, 10, NOW(), NOW()),
('Búp bê Barbie', 'Búp bê Barbie thời trang Fashionistas', 450000, 550000, 50, 'https://salt.tikicdn.com/cache/750x750/ts/product/a4/98/ba/7b30b4f90d47b2f04e5c00a6abf3c7df.jpg', 4.6, 1200, 1, 3, 10, NOW(), NOW());

-- =====================================================
-- 4. THÊM REVIEWS (Đánh giá sản phẩm)
-- =====================================================
INSERT INTO reviews (product_id, user_id, rating, comment, created_at, updated_at) VALUES
(1, 4, 5, 'Sản phẩm chính hãng, giao hàng nhanh, đóng gói cẩn thận. Rất hài lòng!', NOW(), NOW()),
(1, 5, 5, 'iPhone quá đẹp, camera chụp đỉnh. Pin dùng cả ngày thoải mái.', NOW(), NOW()),
(2, 4, 5, 'Samsung S24 Ultra xứng đáng đồng tiền. AI rất hay!', NOW(), NOW()),
(6, 5, 5, 'MacBook M3 quá mượt, render video cực nhanh!', NOW(), NOW()),
(10, 4, 4, 'Áo đẹp, chất vải tốt. Ship nhanh.', NOW(), NOW()),
(16, 5, 5, 'Nồi chiên không dầu rất tiện, nấu ăn ngon mà healthy.', NOW(), NOW()),
(20, 4, 5, 'Serum vitamin C dùng da sáng lên thấy rõ sau 2 tuần.', NOW(), NOW()),
(26, 5, 5, 'Sách hay, nội dung bổ ích. Đóng gói đẹp.', NOW(), NOW());

-- =====================================================
-- 5. THÊM USER_BEHAVIORS (Hành vi người dùng cho AI Recommendation)
-- =====================================================
INSERT INTO user_behaviors (user_id, product_id, category_id, action, timestamp, device_type, province) VALUES
-- Customer 1 behaviors
(4, 1, 1, 'VIEW', DATE_SUB(NOW(), INTERVAL 7 DAY), 'MOBILE', 'Ho Chi Minh'),
(4, 1, 1, 'PURCHASE', DATE_SUB(NOW(), INTERVAL 5 DAY), 'MOBILE', 'Ho Chi Minh'),
(4, 2, 1, 'VIEW', DATE_SUB(NOW(), INTERVAL 6 DAY), 'MOBILE', 'Ho Chi Minh'),
(4, 3, 1, 'VIEW', DATE_SUB(NOW(), INTERVAL 4 DAY), 'MOBILE', 'Ho Chi Minh'),
(4, 6, 2, 'VIEW', DATE_SUB(NOW(), INTERVAL 3 DAY), 'MOBILE', 'Ho Chi Minh'),
(4, 10, 3, 'VIEW', DATE_SUB(NOW(), INTERVAL 7 DAY), 'MOBILE', 'Ho Chi Minh'),
(4, 10, 3, 'ADD_TO_CART', DATE_SUB(NOW(), INTERVAL 5 DAY), 'MOBILE', 'Ho Chi Minh'),
(4, 10, 3, 'PURCHASE', DATE_SUB(NOW(), INTERVAL 5 DAY), 'MOBILE', 'Ho Chi Minh'),
(4, 20, 6, 'VIEW', DATE_SUB(NOW(), INTERVAL 2 DAY), 'MOBILE', 'Ho Chi Minh'),
(4, 20, 6, 'ADD_TO_CART', DATE_SUB(NOW(), INTERVAL 2 DAY), 'MOBILE', 'Ho Chi Minh'),
-- Customer 2 behaviors
(5, 13, 4, 'VIEW', DATE_SUB(NOW(), INTERVAL 4 DAY), 'MOBILE', 'Ha Noi'),
(5, 14, 4, 'VIEW', DATE_SUB(NOW(), INTERVAL 4 DAY), 'MOBILE', 'Ha Noi'),
(5, 15, 4, 'VIEW', DATE_SUB(NOW(), INTERVAL 3 DAY), 'MOBILE', 'Ha Noi'),
(5, 20, 6, 'VIEW', DATE_SUB(NOW(), INTERVAL 2 DAY), 'MOBILE', 'Ha Noi'),
(5, 20, 6, 'ADD_TO_CART', DATE_SUB(NOW(), INTERVAL 2 DAY), 'MOBILE', 'Ha Noi'),
(5, 21, 6, 'VIEW', DATE_SUB(NOW(), INTERVAL 2 DAY), 'MOBILE', 'Ha Noi'),
(5, 21, 6, 'PURCHASE', DATE_SUB(NOW(), INTERVAL 2 DAY), 'MOBILE', 'Ha Noi'),
(5, 26, 8, 'VIEW', DATE_SUB(NOW(), INTERVAL 1 DAY), 'MOBILE', 'Ha Noi'),
(5, 26, 8, 'ADD_TO_CART', NOW(), 'MOBILE', 'Ha Noi');

-- =====================================================
-- HOÀN TẤT! 
-- =====================================================
SELECT 'Du lieu mau da duoc them thanh cong!' AS Status;
SELECT COUNT(*) AS 'So Users' FROM users;
SELECT COUNT(*) AS 'So Categories' FROM categories;
SELECT COUNT(*) AS 'So Products' FROM products;
SELECT COUNT(*) AS 'So Reviews' FROM reviews;
