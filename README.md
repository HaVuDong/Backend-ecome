# Backend E-Commerce (Java Spring Boot) — Marketplace Backend

## 🎯 Tổng quan

Backend cho hệ thống E-commerce Marketplace hỗ trợ 2 app React Native:
- **Ecome** (Seller App) - Quản lý sản phẩm, đơn hàng, dashboard
- **EcomeCustomer** (Customer App) - Mua sắm, giỏ hàng, đặt hàng

## 🚀 Tính năng đã upgrade

### ✅ Security
- **SecurityUtil** - Lấy user từ JWT thay vì request params (bảo mật)
- **Ownership check** - Kiểm tra quyền sở hữu product/order
- **Authorization** - Phân quyền CUSTOMER/SELLER/ADMIN

### ✅ Stock Management
- **Pessimistic Lock** - Tránh race condition khi giảm stock
- **Auto restore** - Hoàn stock khi hủy đơn hàng
- **Validation** - Kiểm tra stock trước khi checkout

### ✅ Order Splitting (Marketplace)
- Tự động tách đơn theo seller khi checkout
- Tính phí platform (5% commission)
- `platformFee` và `sellerAmount` cho mỗi order

### ✅ Review System
- Chỉ review được sản phẩm đã mua
- Chỉ review được sau khi đã giao hàng
- Mỗi user chỉ review 1 lần/sản phẩm
- Auto update product rating

### ✅ Wishlist
- Thêm/xóa sản phẩm yêu thích
- Toggle wishlist
- Check nhiều product cùng lúc

### ✅ Dashboard Analytics
- Thống kê doanh thu seller
- Top sản phẩm bán chạy
- Doanh thu theo ngày
- Thống kê đơn hàng theo trạng thái

### ✅ Advanced Search
- Tìm kiếm với nhiều filters
- Lọc theo giá, category, rating
- Sort động

---

## 📚 API Documentation

### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Đăng ký |
| POST | `/api/auth/login` | Đăng nhập |
| GET | `/api/auth/me` | Profile hiện tại |

### Products
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/products` | No | Danh sách sản phẩm |
| GET | `/api/products/{id}` | No | Chi tiết sản phẩm |
| GET | `/api/products/search?keyword=...` | No | Tìm kiếm |
| GET | `/api/products/search/advanced` | No | Tìm kiếm nâng cao |
| GET | `/api/products/category/{id}` | No | Theo danh mục |
| GET | `/api/products/my-products` | Yes | Sản phẩm của seller |
| POST | `/api/products` | Yes | Tạo sản phẩm (Seller) |
| PUT | `/api/products/{id}` | Yes | Cập nhật (Seller) |
| DELETE | `/api/products/{id}` | Yes | Xóa (Seller) |

### Cart
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/cart` | Yes | Giỏ hàng |
| GET | `/api/cart/grouped` | Yes | Giỏ hàng theo seller |
| POST | `/api/cart` | Yes | Thêm vào giỏ |
| PUT | `/api/cart/{id}` | Yes | Cập nhật số lượng |
| DELETE | `/api/cart/{id}` | Yes | Xóa khỏi giỏ |
| POST | `/api/cart/checkout` | Yes | **Checkout** |

### Orders
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/orders/my-orders` | Yes | Đơn hàng của tôi |
| GET | `/api/orders/{id}` | Yes | Chi tiết đơn |
| GET | `/api/orders/seller/my-orders` | Yes | Đơn hàng của seller |
| GET | `/api/orders/seller/my-revenue` | Yes | Doanh thu seller |
| PUT | `/api/orders/{id}/shipping-status` | Yes | Cập nhật shipping |
| PUT | `/api/orders/{id}/cancel` | Yes | Hủy đơn |

### Reviews
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/reviews/product/{id}` | No | Reviews của sản phẩm |
| GET | `/api/reviews/product/{id}/stats` | No | Thống kê rating |
| POST | `/api/reviews` | Yes | Tạo review |
| GET | `/api/reviews/me` | Yes | Reviews của tôi |
| DELETE | `/api/reviews/{id}` | Yes | Xóa review |

### Wishlist
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/wishlist` | Yes | Danh sách yêu thích |
| POST | `/api/wishlist/{productId}` | Yes | Thêm vào wishlist |
| DELETE | `/api/wishlist/{productId}` | Yes | Xóa khỏi wishlist |
| POST | `/api/wishlist/{productId}/toggle` | Yes | Toggle wishlist |
| GET | `/api/wishlist/product-ids` | Yes | Danh sách product IDs |

### Dashboard
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/dashboard/seller` | Yes | Dashboard seller |
| GET | `/api/dashboard/admin` | Admin | Dashboard admin |

---

## 1) Entity & thuộc tính (dự kiến)

- **User**
  - `id`, `email`, `passwordHash`, `fullName`, `phone`, `role`(CUSTOMER/SELLER/ADMIN), `status`, `createdAt`, `updatedAt`
- **Category**
  - `id`, `name`, `slug`, `icon`, `createdAt`, `updatedAt`
- **Product**
  - `id`, `sellerId(FK User)`, `categoryId(FK Category)`, `name`, `description`, `price`, `originalPrice`, `stock`, `thumbnailUrl`, `ratingAvg`, `soldCount`, `status`, `createdAt`, `updatedAt`
- **ProductImage**
  - `id`, `productId(FK)`, `url`, `isPrimary`, `sortOrder`
- **CartItem**
  - `id`, `userId(FK)`, `productId(FK)`, `quantity`, `selected`, `createdAt`, `updatedAt`
- **Order**
  - `id`, `userId(FK)`, `sellerId(FK User)`, `code`, `status`, `subtotal`, `discountAmount`, `shippingFee`, `totalAmount`, `paymentMethod`, `shippingName`, `shippingPhone`, `shippingAddress`, `note`, `createdAt`, `updatedAt`
- **OrderItem**
  - `id`, `orderId(FK)`, `productId(FK)`, `productName`, `unitPrice`, `quantity`, `lineTotal`
- **Voucher**
  - `id`, `code`, `title`, `discountType`(PERCENT/FIXED), `discountValue`, `minOrderAmount`, `maxDiscount`, `startAt`, `endAt`, `totalQuantity`, `usedQuantity`, `status`
- **UserVoucher**
  - `id`, `userId(FK)`, `voucherId(FK)`, `claimedAt`, `usedAt`, `orderId(FK Order, null)`
- **Review**
  - `id`, `productId(FK)`, `userId(FK)`, `rating`, `comment`, `createdAt`
- **ChatConversation** (tuỳ chọn)
  - `id`, `customerId(FK User)`, `sellerId(FK User)`, `lastMessageAt`, `createdAt`
- **ChatMessage** (tuỳ chọn)
  - `id`, `conversationId(FK)`, `senderId(FK User)`, `content`, `type`(TEXT/IMAGE), `createdAt`, `isRead`
- **LiveStream** (tuỳ chọn)
  - `id`, `sellerId(FK User)`, `title`, `thumbnailUrl`, `status`, `startedAt`, `endedAt`
- **DailyCheckin** (tuỳ chọn)
  - `id`, `userId(FK)`, `checkinDate`, `points`

> Ghi chú: Đây là các entity tối thiểu để khớp UI hiện tại của 2 app (Customer + Seller). Có thể cắt bớt các phần “tuỳ chọn” nếu bài tập yêu cầu đơn giản.

---

## 2) Chức năng chính theo từng đối tượng

- **User/Auth**
  - Đăng ký, đăng nhập, phân quyền CUSTOMER/SELLER
  - Xem/cập nhật hồ sơ
- **Category**
  - Xem danh mục (customer)
  - Quản lý danh mục (admin/seller nếu cần)
- **Product**
  - Customer: xem danh sách/chi tiết, lọc theo danh mục, tìm kiếm
  - Seller: thêm/sửa/xoá, cập nhật tồn kho
- **CartItem**
  - Customer: thêm vào giỏ, tăng/giảm số lượng, chọn item, xoá item
- **Order/OrderItem**
  - Customer: tạo đơn từ giỏ, xem lịch sử, xem chi tiết, huỷ đơn (tuỳ)
  - Seller: xem danh sách đơn, cập nhật trạng thái
- **Voucher/UserVoucher**
  - Customer: xem voucher, claim voucher, áp voucher khi đặt hàng
- **Review**
  - Customer: đánh giá sản phẩm (sau khi mua)
- **ChatConversation/ChatMessage** (tuỳ)
  - Customer ↔ Seller: nhắn tin
- **LiveStream** (tuỳ)
  - Customer: xem live
  - Seller: tạo/đóng live
- **DailyCheckin** (tuỳ)
  - Customer: điểm danh nhận điểm

---

## 3) Các trang giao diện (UI) đang có

### EcomeCustomer (Customer)
- Home (Banner, FlashSale, Voucher, Live, DailyCheckin)
- Categories
- Product Detail (Modal)
- Search (Modal)
- Cart
- Profile

### Ecome (Seller)
- Login
- Register
- Dashboard
- Product List
- Add Product
- Edit Product
- Orders List
- Order Detail
- Chat
- Profile

---

## 4) Công nghệ sử dụng (backend-ecome)

Theo `pom.xml` hiện tại:
- Java **21**
- Spring Boot **4.0.0**
- Spring Web MVC
- Spring Data JPA
- Spring Security
- Validation
- MySQL Connector
- Lombok
- Maven Wrapper (`mvnw`)

---

## 5) Sơ đồ quan hệ + SQL (MySQL) — copy chạy được

> Script dưới đây tạo DB + bảng + khoá ngoại cho mô hình monolith (phù hợp bài tập và khớp 2 app). Nếu bạn muốn microservices thì sẽ tách DB theo service sau.

```sql
-- MySQL 8+
CREATE DATABASE IF NOT EXISTS ecome_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ecome_db;

-- 1) users
CREATE TABLE IF NOT EXISTS users (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  email VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  full_name VARCHAR(255) NULL,
  phone VARCHAR(30) NULL,
  role ENUM('CUSTOMER','SELLER','ADMIN') NOT NULL DEFAULT 'CUSTOMER',
  status ENUM('ACTIVE','INACTIVE','BANNED') NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 2) categories
CREATE TABLE IF NOT EXISTS categories (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(120) NOT NULL,
  slug VARCHAR(140) NOT NULL UNIQUE,
  icon VARCHAR(255) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 3) products
CREATE TABLE IF NOT EXISTS products (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  seller_id BIGINT UNSIGNED NOT NULL,
  category_id BIGINT UNSIGNED NULL,
  name VARCHAR(255) NOT NULL,
  description TEXT NULL,
  price DECIMAL(12,2) NOT NULL,
  original_price DECIMAL(12,2) NULL,
  stock INT NOT NULL DEFAULT 0,
  thumbnail_url VARCHAR(500) NULL,
  rating_avg DECIMAL(3,2) NOT NULL DEFAULT 0.00,
  sold_count INT NOT NULL DEFAULT 0,
  status ENUM('DRAFT','ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_products_seller FOREIGN KEY (seller_id) REFERENCES users(id),
  CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id)
) ENGINE=InnoDB;

CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_seller ON products(seller_id);

-- 4) product_images
CREATE TABLE IF NOT EXISTS product_images (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT UNSIGNED NOT NULL,
  url VARCHAR(500) NOT NULL,
  is_primary BOOLEAN NOT NULL DEFAULT FALSE,
  sort_order INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_product_images_product ON product_images(product_id);

-- 5) cart_items
CREATE TABLE IF NOT EXISTS cart_items (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  product_id BIGINT UNSIGNED NOT NULL,
  quantity INT NOT NULL,
  selected BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_cart_product FOREIGN KEY (product_id) REFERENCES products(id),
  CONSTRAINT uq_cart_user_product UNIQUE (user_id, product_id)
) ENGINE=InnoDB;

-- 6) orders
CREATE TABLE IF NOT EXISTS orders (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(30) NOT NULL UNIQUE,
  user_id BIGINT UNSIGNED NOT NULL,
  seller_id BIGINT UNSIGNED NOT NULL,
  status ENUM('PENDING','PROCESSING','SHIPPED','DELIVERED','CANCELLED') NOT NULL DEFAULT 'PENDING',
  subtotal DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  shipping_fee DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  total_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  payment_method ENUM('COD','BANK','MOMO','VNPAY') NOT NULL DEFAULT 'COD',
  shipping_name VARCHAR(255) NULL,
  shipping_phone VARCHAR(30) NULL,
  shipping_address TEXT NULL,
  note TEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_orders_seller FOREIGN KEY (seller_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE INDEX idx_orders_user ON orders(user_id);
CREATE INDEX idx_orders_seller ON orders(seller_id);
CREATE INDEX idx_orders_status ON orders(status);

-- 7) order_items
CREATE TABLE IF NOT EXISTS order_items (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT UNSIGNED NOT NULL,
  product_id BIGINT UNSIGNED NOT NULL,
  product_name VARCHAR(255) NOT NULL,
  unit_price DECIMAL(12,2) NOT NULL,
  quantity INT NOT NULL,
  line_total DECIMAL(12,2) NOT NULL,
  CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
  CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB;

CREATE INDEX idx_order_items_order ON order_items(order_id);

-- 8) vouchers
CREATE TABLE IF NOT EXISTS vouchers (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(50) NOT NULL UNIQUE,
  title VARCHAR(255) NOT NULL,
  discount_type ENUM('PERCENT','FIXED') NOT NULL,
  discount_value DECIMAL(12,2) NOT NULL,
  min_order_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  max_discount DECIMAL(12,2) NULL,
  start_at DATETIME NULL,
  end_at DATETIME NULL,
  total_quantity INT NOT NULL DEFAULT 0,
  used_quantity INT NOT NULL DEFAULT 0,
  status ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE'
) ENGINE=InnoDB;

-- 9) user_vouchers
CREATE TABLE IF NOT EXISTS user_vouchers (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  voucher_id BIGINT UNSIGNED NOT NULL,
  claimed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  used_at TIMESTAMP NULL,
  order_id BIGINT UNSIGNED NULL,
  CONSTRAINT fk_user_vouchers_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_user_vouchers_voucher FOREIGN KEY (voucher_id) REFERENCES vouchers(id) ON DELETE CASCADE,
  CONSTRAINT fk_user_vouchers_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE SET NULL,
  CONSTRAINT uq_user_voucher UNIQUE (user_id, voucher_id)
) ENGINE=InnoDB;

-- 10) reviews
CREATE TABLE IF NOT EXISTS reviews (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  rating TINYINT UNSIGNED NOT NULL,
  comment TEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_reviews_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
  CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_reviews_product ON reviews(product_id);

-- (Tuỳ chọn) 11) chat_conversations
CREATE TABLE IF NOT EXISTS chat_conversations (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  customer_id BIGINT UNSIGNED NOT NULL,
  seller_id BIGINT UNSIGNED NOT NULL,
  last_message_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_chat_customer FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_chat_seller FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT uq_chat_pair UNIQUE (customer_id, seller_id)
) ENGINE=InnoDB;

-- (Tuỳ chọn) 12) chat_messages
CREATE TABLE IF NOT EXISTS chat_messages (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  conversation_id BIGINT UNSIGNED NOT NULL,
  sender_id BIGINT UNSIGNED NOT NULL,
  type ENUM('TEXT','IMAGE') NOT NULL DEFAULT 'TEXT',
  content TEXT NOT NULL,
  is_read BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_messages_conversation FOREIGN KEY (conversation_id) REFERENCES chat_conversations(id) ON DELETE CASCADE,
  CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- (Tuỳ chọn) 13) live_streams
CREATE TABLE IF NOT EXISTS live_streams (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  seller_id BIGINT UNSIGNED NOT NULL,
  title VARCHAR(255) NOT NULL,
  thumbnail_url VARCHAR(500) NULL,
  status ENUM('SCHEDULED','LIVE','ENDED') NOT NULL DEFAULT 'SCHEDULED',
  started_at DATETIME NULL,
  ended_at DATETIME NULL,
  CONSTRAINT fk_live_seller FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- (Tuỳ chọn) 14) daily_checkins
CREATE TABLE IF NOT EXISTS daily_checkins (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  checkin_date DATE NOT NULL,
  points INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_checkin_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT uq_user_checkin UNIQUE (user_id, checkin_date)
) ENGINE=InnoDB;
```
