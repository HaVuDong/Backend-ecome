package havudong.baocao.constant;

import java.util.Map;

/**
 * Constants cho Recommendation System
 * 
 * Tập trung tất cả trọng số và config tại 1 nơi duy nhất
 * để tránh trùng lặp và dễ maintain.
 */
public final class RecommendationConstants {
    
    private RecommendationConstants() {
        // Prevent instantiation
    }
    
    // ========================================================================
    // TRỌNG SỐ HÀNH VI - Weighted Scoring
    // ========================================================================
    
    /**
     * Bảng trọng số cho mỗi loại hành vi người dùng
     * 
     * Giá trị càng cao = hành vi càng thể hiện sự quan tâm
     * 
     * Cơ sở:
     * - VIEW (1): Chỉ xem qua, có thể vô tình
     * - SEARCH (2): Chủ động tìm kiếm, có ý định
     * - ADD_TO_CART (3): Có ý định mua rõ ràng
     * - WISHLIST (4): Yêu thích, muốn mua sau
     * - PURCHASE (5): Đã mua = quan trọng nhất
     */
    public static final Map<String, Integer> ACTION_WEIGHTS = Map.of(
        "VIEW", 1,
        "SEARCH", 2,
        "ADD_TO_CART", 3,
        "WISHLIST", 4,
        "PURCHASE", 5
    );
    
    // ========================================================================
    // CẤU HÌNH THỜI GIAN
    // ========================================================================
    
    /** Số ngày lấy dữ liệu hành vi để phân tích */
    public static final int BEHAVIOR_ANALYSIS_DAYS = 30;
    
    /** Số ngày để tính trending */
    public static final int TRENDING_DAYS = 7;
    
    // ========================================================================
    // CẤU HÌNH GIỚI HẠN
    // ========================================================================
    
    /** Số category yêu thích tối đa lưu trữ */
    public static final int MAX_FAVORITE_CATEGORIES = 5;
    
    /** Số category dùng để recommend */
    public static final int TOP_CATEGORIES_FOR_RECOMMENDATION = 3;
    
    /** Khoảng giá mở rộng khi filter (±50%) */
    public static final double PRICE_RANGE_FACTOR = 0.5;
    
    /** Rating tối thiểu để recommend */
    public static final double MIN_RATING_FOR_RECOMMENDATION = 4.0;
    
    // ========================================================================
    // HELPER METHODS
    // ========================================================================
    
    /**
     * Lấy trọng số của một action
     * @param action Tên action (VIEW, SEARCH, etc.)
     * @return Trọng số, mặc định 1 nếu không tìm thấy
     */
    public static int getWeight(String action) {
        return ACTION_WEIGHTS.getOrDefault(action, 1);
    }
}
