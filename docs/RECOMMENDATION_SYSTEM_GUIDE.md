# 📚 GIẢI THÍCH CHI TIẾT HỆ THỐNG RECOMMENDATION

> **Tài liệu dành cho:** Sinh viên chuẩn bị bảo vệ đồ án  
> **Công nghệ:** Spring Boot + MySQL  
> **Thuật toán:** Weighted Scoring (không Deep Learning)

---

## 1. LUỒNG HOẠT ĐỘNG TỔNG THỂ

### 📊 Sơ đồ luồng dữ liệu

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         LUỒNG HOẠT ĐỘNG HỆ THỐNG                            │
└─────────────────────────────────────────────────────────────────────────────┘

[NGƯỜI DÙNG] ──► [APP MOBILE] ──► [API /track] ──► [user_behaviors TABLE]
     │                                                       │
     │                                                       ▼
     │                                    ┌─────────────────────────────────┐
     │                                    │   SCHEDULED JOB (2:00 AM)       │
     │                                    │   Phân tích hành vi 30 ngày     │
     │                                    │   Tính toán preferences         │
     │                                    └─────────────────────────────────┘
     │                                                       │
     │                                                       ▼
     │                                    ┌─────────────────────────────────┐
     │                                    │   user_preferences TABLE        │
     │                                    │   - favoriteCategories          │
     │                                    │   - avgPriceRange              │
     │                                    │   - engagementScore            │
     │                                    └─────────────────────────────────┘
     │                                                       │
     ▼                                                       ▼
[YÊU CẦU /for-you] ──► [RecommendationService] ◄────────────┘
                                │
                                ▼
                    ┌───────────────────────┐
                    │   DANH SÁCH GỢI Ý     │
                    │   (Personalized)      │
                    └───────────────────────┘
```

### 📝 Chi tiết từng bước

| Bước | Hành động | Mô tả |
|------|-----------|-------|
| **1** | User thao tác trên app | Xem sản phẩm, tìm kiếm, thêm giỏ hàng... |
| **2** | App gọi API `/api/recommendations/track` | Gửi thông tin: action, productId, searchQuery |
| **3** | Server lưu vào `user_behaviors` | Mỗi hành vi = 1 record trong database |
| **4** | Job chạy lúc 2:00 AM | Phân tích hành vi 30 ngày gần nhất |
| **5** | Tính toán và lưu preferences | Cập nhật bảng `user_preferences` |
| **6** | User yêu cầu gợi ý | App gọi API `/api/recommendations/for-you` |
| **7** | Service tính toán realtime | Dùng cả behavior + preferences |
| **8** | Trả về danh sách gợi ý | Sản phẩm phù hợp nhất với user |

---

## 2. CÁC TRƯỜNG HỢP HOẠT ĐỘNG

### 🆕 2.1. User mới (Cold Start Problem)

**Tình huống:** Người dùng vừa đăng ký, chưa có bất kỳ hành vi nào.

```
User mới ──► Không có behavior ──► categoryScores = rỗng
                                          │
                                          ▼
                                   Fallback: Trending Products
```

**Kết quả:** Hệ thống trả về **sản phẩm trending** (đang hot trong 7 ngày gần nhất).

**Ví dụ thực tế:** 
- Bạn Minh vừa tải app, chưa xem sản phẩm nào
- Hệ thống gợi ý: iPhone 15, Laptop Dell XPS (vì nhiều người đang xem/mua)

---

### 📜 2.2. User đã có lịch sử

**Tình huống:** User đã dùng app 1 tuần, có nhiều hành vi.

```
User có lịch sử ──► Lấy behavior 30 ngày ──► Tính category scores
                                                    │
                    ┌───────────────────────────────┘
                    ▼
        ┌─────────────────────────────────┐
        │ Category "Điện thoại": 45 điểm  │  (Xem 10 lần + Mua 1 lần)
        │ Category "Laptop": 12 điểm      │  (Xem 5 lần + Thêm giỏ 2 lần)
        │ Category "Thời trang": 3 điểm   │  (Xem 3 lần)
        └─────────────────────────────────┘
                    │
                    ▼
        Top 3: Điện thoại, Laptop, Thời trang
                    │
                    ▼
        Gợi ý sản phẩm từ 3 category này
```

**Công thức tính điểm:**
```
Điểm = Σ (số_lần × trọng_số)

Ví dụ Category "Điện thoại":
= (10 × 1) + (1 × 5)     // 10 VIEW + 1 PURCHASE
= 10 + 5 = 15 điểm

(Thực tế còn SEARCH, ADD_TO_CART, WISHLIST nữa)
```

---

### 👁️ 2.3. User xem chi tiết sản phẩm

**Trigger:** User click vào sản phẩm A.

```
Click vào iPhone 15
        │
        ├──► Track: action="VIEW", productId=123
        │
        └──► Hiển thị section "Sản phẩm tương tự"
                    │
                    ▼
            API: /similar/123
                    │
                    ▼
            ┌─────────────────────────┐
            │ Logic tìm Similar:      │
            │ - Cùng category         │
            │ - Giá ±30%              │
            │ - Đang còn hàng         │
            └─────────────────────────┘
                    │
                    ▼
            Samsung Galaxy S24, Xiaomi 14, OPPO Find X7
            (cùng là Điện thoại, giá tương đương)
```

---

### 🔍 2.4. User tìm kiếm sản phẩm

**Trigger:** User gõ "laptop gaming".

```
Tìm "laptop gaming"
        │
        ├──► Track: action="SEARCH", searchQuery="laptop gaming"
        │           (trọng số = 2, cao hơn VIEW)
        │
        └──► Lần sau gợi ý category "Laptop" nhiều hơn
```

**Tại sao SEARCH có trọng số cao hơn VIEW?**

Vì khi user **chủ động tìm kiếm**, họ thể hiện **ý định rõ ràng** hơn so với chỉ lướt xem.

---

### 💰 2.5. User mua hàng

**Trigger:** User hoàn tất đơn hàng.

```
Mua iPhone 15
        │
        ├──► Track: action="PURCHASE", productId=123
        │           (trọng số = 5, CAO NHẤT)
        │
        ├──► Cập nhật avgPriceRange (giá TB user hay mua)
        │
        ├──► Loại iPhone 15 khỏi gợi ý 30 ngày tới
        │    (vì đã mua rồi, không cần gợi ý lại)
        │
        └──► Tăng mạnh ưu tiên category "Điện thoại"
```

**Lưu ý quan trọng:**
- Sản phẩm đã mua sẽ **không xuất hiện** trong gợi ý "For You" trong 30 ngày
- Tránh gợi ý thứ user đã có

---

## 3. TRƯỜNG HỢP HOẠT ĐỘNG TỐT NHẤT (BEST CASE)

### ✅ Điều kiện lý tưởng

```
┌────────────────────────────────────────────────────────────────┐
│                    ĐIỀU KIỆN TỐI ƯU                            │
├────────────────────────────────────────────────────────────────┤
│ ✓ User đăng nhập liên tục                                      │
│ ✓ Có ít nhất 50+ behaviors trong 30 ngày                       │
│ ✓ Đã mua hàng ít nhất 2-3 lần                                  │
│ ✓ Có pattern rõ ràng (thích 1-2 category chính)                │
│ ✓ Database có nhiều sản phẩm đa dạng                           │
└────────────────────────────────────────────────────────────────┘
```

### 🎯 Ví dụ Best Case

**Hồ sơ user:**
- Tên: Lan
- 30 ngày qua: Xem 80 sản phẩm, mua 5 lần, wishlist 10 sản phẩm
- Pattern: 70% là Thời trang nữ, 20% là Mỹ phẩm

**Hệ thống hiểu:**
```
Category Scores:
├── Thời trang nữ: 156 điểm (rất cao)
├── Mỹ phẩm: 42 điểm (cao)
└── Đồ gia dụng: 8 điểm (thấp)

avgPriceRange: 500,000 VNĐ (hay mua đồ ~500k)
```

**Gợi ý trả về:**
1. Váy công sở mới (Thời trang, ~450k, rating 4.5★)
2. Son MAC (Mỹ phẩm, ~550k, rating 4.8★)
3. Áo sơ mi nữ (Thời trang, ~350k, rating 4.3★)

→ **Độ chính xác cao** vì dữ liệu đủ để "hiểu" user.

---

## 4. TRƯỜNG HỢP HOẠT ĐỘNG XẤU NHẤT (WORST CASE)

### ❌ Các trường hợp xấu

| Trường hợp | Nguyên nhân | Fallback |
|------------|-------------|----------|
| User chưa đăng nhập | Không track được behavior | Trending products |
| User mới hoàn toàn | Không có dữ liệu | Trending products |
| User random (không có pattern) | Xem đủ loại category | Best-seller chung |
| Database ít sản phẩm | Không có gì để gợi ý | Tất cả sản phẩm active |

### 🔄 Cơ chế Fallback

```java
// Trong RecommendationService.java

// Edge case: user mới chưa có behavior
if (topCategoryIds.isEmpty()) {
    log.debug("User {} has no behavior, returning trending products", user.getId());
    return getTrendingProducts(limit, TRENDING_DAYS);
}

// Nếu gợi ý chưa đủ số lượng
if (result.size() < limit) {
    List<ProductResponse> trending = getTrendingProducts(limit - result.size(), TRENDING_DAYS);
    result.addAll(trending);
}
```

**Giải thích:** 
- Khi không có đủ dữ liệu cá nhân → dùng **dữ liệu cộng đồng** (trending)
- Đảm bảo user luôn thấy gợi ý, không bao giờ trả về rỗng

---

## 5. CÁC ĐOẠN CODE QUAN TRỌNG CẦN HIỂU

### 🔴 5.1. Tracking hành vi

**File:** `RecommendationService.java` (line 256-271)

```java
@Transactional
public void trackUserBehavior(User user, String action, Long productId, 
                               Long categoryId, String searchQuery) {
    UserBehavior behavior = new UserBehavior();
    behavior.setUser(user);                    // Ai thực hiện?
    behavior.setAction(action);                // Làm gì? (VIEW/SEARCH/...)
    
    if (productId != null) {
        productRepository.findById(productId)
            .ifPresent(behavior::setProduct);  // Sản phẩm nào?
    }
    
    behavior.setSearchQuery(searchQuery);      // Tìm từ khóa gì?
    behavior.setProvince(extractProvince(user.getAddress()));  // Ở đâu?
    behavior.setTimestamp(LocalDateTime.now());               // Lúc nào?
    
    userBehaviorRepository.save(behavior);     // Lưu vào DB
}
```

**Mục đích:** Thu thập **"dấu chân số"** của người dùng. Mỗi lần thao tác = 1 record.

---

### 🟠 5.2. Preference Learning (Học sở thích)

**File:** `UserPreferenceUpdateJob.java` (line 99-112)

```java
/**
 * Tính điểm preference cho từng category (Weighted Scoring)
 */
private Map<Long, Integer> calculateCategoryScores(List<UserBehavior> behaviors) {
    Map<Long, Integer> scores = new HashMap<>();
    
    for (UserBehavior behavior : behaviors) {
        if (behavior.getProduct() == null) continue;
        
        Long categoryId = behavior.getProduct().getCategory().getId();
        int weight = getWeight(behavior.getAction());  // Lấy trọng số từ Constants
        scores.merge(categoryId, weight, Integer::sum);  // Cộng dồn
    }
    
    return scores;
}
```

**Bảng trọng số (ACTION_WEIGHTS):**

| Hành vi | Trọng số | Lý do |
|---------|----------|-------|
| VIEW | 1 | Có thể vô tình click |
| SEARCH | 2 | Chủ động tìm kiếm |
| ADD_TO_CART | 3 | Có ý định mua |
| WISHLIST | 4 | Yêu thích, muốn mua sau |
| PURCHASE | 5 | Đã bỏ tiền mua = quan trọng nhất |

**Ví dụ tính toán:**

```
User A trong 30 ngày:
- VIEW "Điện thoại" 20 lần      → 20 × 1 = 20
- SEARCH "Điện thoại" 5 lần     → 5 × 2 = 10
- ADD_TO_CART "Điện thoại" 2 lần → 2 × 3 = 6
- PURCHASE "Điện thoại" 1 lần   → 1 × 5 = 5
────────────────────────────────────────────
TỔNG điểm category "Điện thoại" = 41 điểm
```

---

### 🟢 5.3. Personalized Recommendation

**File:** `RecommendationService.java` (line 39-106)

```java
public List<ProductResponse> getPersonalizedRecommendations(User user, int limit) {
    
    // BƯỚC 1: Tính điểm yêu thích từng category
    Map<Long, Double> categoryScores = calculateCategoryScores(user);
    
    // BƯỚC 2: Lấy TOP 3 categories (constant = 3)
    List<Long> topCategoryIds = categoryScores.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .limit(TOP_CATEGORIES_FOR_RECOMMENDATION)  // = 3
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    
    // BƯỚC 3: Nếu user mới → fallback trending
    if (topCategoryIds.isEmpty()) {
        return getTrendingProducts(limit, TRENDING_DAYS);
    }
    
    // BƯỚC 4: Lọc sản phẩm theo giá phù hợp
    Double avgPrice = preference.getAvgPriceRange();
    if (avgPrice != null) {
        double minPrice = avgPrice * (1 - PRICE_RANGE_FACTOR);  // -50%
        double maxPrice = avgPrice * (1 + PRICE_RANGE_FACTOR);  // +50%
        // Query products trong khoảng giá này
    }
    
    // BƯỚC 5: Loại bỏ sản phẩm đã mua gần đây
    Set<Long> recentPurchasedIds = getRecentPurchasedProductIds(user, since);
    result = recommendations.stream()
            .filter(p -> !recentPurchasedIds.contains(p.getId()))
            ...
    
    // BƯỚC 6: Bổ sung trending nếu chưa đủ
    if (result.size() < limit) {
        result.addAll(getTrendingProducts(limit - result.size(), TRENDING_DAYS));
    }
    
    return result;
}
```

**Tóm tắt logic:**

```
                    ┌──────────────────┐
                    │  Input: User ID  │
                    └────────┬─────────┘
                             ▼
              ┌──────────────────────────────┐
              │ Tính categoryScores từ       │
              │ behaviors 30 ngày            │
              └──────────────┬───────────────┘
                             ▼
              ┌──────────────────────────────┐
              │ Lấy TOP 3 categories         │
              │ (điểm cao nhất)              │
              └──────────────┬───────────────┘
                             ▼
              ┌──────────────────────────────┐
              │ Lọc sản phẩm:                │
              │ - Thuộc top categories       │
              │ - Giá ±50% avgPrice          │
              │ - Rating ≥ 4.0               │
              │ - Chưa mua gần đây           │
              └──────────────┬───────────────┘
                             ▼
              ┌──────────────────────────────┐
              │ Output: List<Product>        │
              │ được xếp hạng phù hợp        │
              └──────────────────────────────┘
```

---

### 🔵 5.4. Similar Products (Content-based)

**File:** `RecommendationService.java` (line 112-131)

```java
public List<ProductResponse> getSimilarProducts(Long productId, int limit) {
    Product product = productRepository.findById(productId).orElseThrow(...);
    
    // Tìm sản phẩm tương tự:
    // - Cùng category
    // - Giá chênh lệch ±30%
    double productPrice = product.getPrice().doubleValue();
    double minPrice = productPrice * 0.7;   // -30%
    double maxPrice = productPrice * 1.3;   // +30%
    
    List<Product> similar = productRepository
        .findByCategoryAndPriceBetweenAndIdNotAndIsActiveTrue(
            product.getCategory(),   // Cùng category
            minPrice,                // Giá min
            maxPrice,                // Giá max
            productId,               // Loại chính nó
            PageRequest.of(0, limit)
        ).getContent();
    
    return similar.stream().map(productMapper::toResponse).collect(...);
}
```

**Mục đích:** Khi user xem iPhone 15 (25 triệu) → Gợi ý Samsung S24 (22 triệu), Xiaomi 14 (20 triệu) vì cùng category + giá tương đương.

---

## 6. TỔNG KẾT NGẮN GỌN

### ✅ Ưu điểm của hệ thống

| Ưu điểm | Giải thích |
|---------|-----------|
| **Đơn giản, dễ hiểu** | Không dùng ML phức tạp, logic rõ ràng |
| **Realtime + Batch** | Có cả phân tích realtime và job hàng ngày |
| **Fallback thông minh** | Luôn có gợi ý dù user mới hay cũ |
| **Dễ maintain** | Constants tập trung, code clean |
| **Tối ưu cho đồ án** | Đủ phức tạp để impress, đủ đơn giản để giải thích |

### ❌ Hạn chế hiện tại

| Hạn chế | Lý do chấp nhận được |
|---------|----------------------|
| **Không dùng ML/AI thực sự** | Phức tạp, cần data lớn, overkill cho đồ án |
| **Tính toán đơn giản** | Weighted scoring dễ hiểu hơn collaborative filtering |
| **Load all behaviors vào memory** | Với quy mô đồ án (~1000 users) không thành vấn đề |
| **Không có A/B testing** | Ngoài scope đồ án |

### 🎓 Lý do thiết kế phù hợp đồ án đại học

```
┌─────────────────────────────────────────────────────────────┐
│                  PHÂN TÍCH SỰ PHÙ HỢP                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ✓ Thuật toán WEIGHTED SCORING:                             │
│    - Có cơ sở logic (hành vi → trọng số → điểm)            │
│    - Dễ giải thích trong 5 phút                             │
│    - Có thể demo trực tiếp                                  │
│                                                             │
│  ✓ Không dùng Deep Learning vì:                             │
│    - Cần dataset cực lớn (>100k records)                    │
│    - Training time dài, khó demo live                       │
│    - Khó giải thích cho hội đồng                            │
│    - Overkill cho app e-commerce quy mô nhỏ                 │
│                                                             │
│  ✓ Đủ điểm để đạt mục tiêu đồ án:                           │
│    - Có tracking behavior ✓                                 │
│    - Có personalization ✓                                   │
│    - Có multiple recommendation types ✓                     │
│    - Có scheduled job ✓                                     │
│    - Code clean, có constants ✓                             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 📋 GỢI Ý KHI BẢO VỆ ĐỒ ÁN

### Câu hỏi thường gặp và cách trả lời:

**Q: "Tại sao không dùng Machine Learning?"**

> A: "Em đã cân nhắc và thấy rằng với quy mô đồ án (~1000 users giả lập), việc dùng ML phức tạp như Collaborative Filtering hay Neural Networks là overkill. Weighted Scoring Algorithm cho kết quả đủ tốt, dễ maintain, và quan trọng nhất là em có thể giải thích được từng bước logic cho hội đồng. Trong thực tế, nhiều startup e-commerce cũng bắt đầu với heuristic-based recommendation trước khi scale lên ML."

**Q: "Làm sao hệ thống biết user thích gì?"**

> A: "Hệ thống track 5 loại hành vi: VIEW, SEARCH, ADD_TO_CART, WISHLIST, PURCHASE. Mỗi loại có trọng số khác nhau (PURCHASE=5, VIEW=1). Sau đó tính tổng điểm cho từng category, category nào điểm cao nhất = user thích nhất. Ví dụ user xem 10 điện thoại, mua 1 cái → category Điện thoại có 15 điểm."

**Q: "Cold start problem giải quyết như thế nào?"**

> A: "Khi user mới chưa có dữ liệu, em fallback sang Trending Products - những sản phẩm đang được nhiều người quan tâm trong 7 ngày gần nhất. Đây là chiến lược phổ biến trong industry gọi là popularity-based recommendation."

---

## 📁 CẤU TRÚC FILE LIÊN QUAN

```
src/main/java/havudong/baocao/
├── constant/
│   └── RecommendationConstants.java    # Trọng số và config
├── controller/
│   └── RecommendationController.java   # REST API endpoints
├── service/
│   └── RecommendationService.java      # Business logic
├── job/
│   └── UserPreferenceUpdateJob.java    # Scheduled job 2:00 AM
├── entity/
│   ├── UserBehavior.java               # Entity tracking hành vi
│   └── UserPreference.java             # Entity lưu sở thích
└── repository/
    ├── UserBehaviorRepository.java
    └── UserPreferenceRepository.java
```

---

## 📊 DATABASE SCHEMA

### Bảng `user_behaviors`
```sql
CREATE TABLE user_behaviors (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,        -- VIEW, SEARCH, ADD_TO_CART, WISHLIST, PURCHASE
    product_id BIGINT,
    category_id BIGINT,
    search_query VARCHAR(255),
    device_type VARCHAR(100),
    province VARCHAR(100),
    timestamp DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);
```

### Bảng `user_preferences`
```sql
CREATE TABLE user_preferences (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNIQUE NOT NULL,
    favorite_categories TEXT,           -- JSON: [1, 5, 12]
    avg_price_range DOUBLE,
    max_price_paid DOUBLE,
    favorite_sellers TEXT,              -- JSON
    preferred_shopping_time VARCHAR(50),
    engagement_score INT,
    last_updated DATETIME,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

---

## 🔗 API ENDPOINTS

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/api/recommendations/for-you` | Gợi ý cá nhân hóa |
| GET | `/api/recommendations/similar/{productId}` | Sản phẩm tương tự |
| GET | `/api/recommendations/bought-together/{productId}` | Mua cùng nhau |
| GET | `/api/recommendations/trending` | Sản phẩm đang hot |
| GET | `/api/recommendations/popular-in-area` | Phổ biến theo vùng |
| POST | `/api/recommendations/track` | Lưu hành vi user |

---

**Chúc bạn bảo vệ đồ án thành công! 🎓**
