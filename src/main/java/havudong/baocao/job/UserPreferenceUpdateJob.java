package havudong.baocao.job;

import havudong.baocao.entity.User;
import havudong.baocao.entity.UserBehavior;
import havudong.baocao.entity.UserPreference;
import havudong.baocao.repository.UserBehaviorRepository;
import havudong.baocao.repository.UserPreferenceRepository;
import havudong.baocao.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static havudong.baocao.constant.RecommendationConstants.*;

/**
 * Scheduled Job: Cập nhật user preferences hàng ngày
 * 
 * Chạy lúc 2:00 AM mỗi ngày, phân tích hành vi 30 ngày
 * và cập nhật bảng user_preferences.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserPreferenceUpdateJob {
    
    private final UserRepository userRepository;
    private final UserBehaviorRepository userBehaviorRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    
    /**
     * Chạy mỗi ngày lúc 2:00 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void updateAllUserPreferences() {
        long startTime = System.currentTimeMillis();
        log.info("Starting user preference update job...");
        
        List<User> users = userRepository.findAll();
        int updated = 0;
        int skipped = 0;
        
        for (User user : users) {
            try {
                if (updateUserPreference(user)) {
                    updated++;
                } else {
                    skipped++;
                }
            } catch (Exception e) {
                log.error("Error updating preference for user {}: {}", user.getId(), e.getMessage());
            }
        }
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("Preference update completed. Updated: {}, Skipped: {}, Duration: {}ms", 
                 updated, skipped, duration);
    }
    
    /**
     * Cho phép gọi thủ công (test/debug)
     */
    public void runManually() {
        updateAllUserPreferences();
    }
    
    /**
     * Cập nhật preference cho 1 user
     */
    private boolean updateUserPreference(User user) {
        LocalDateTime since = LocalDateTime.now().minusDays(BEHAVIOR_ANALYSIS_DAYS);
        List<UserBehavior> behaviors = userBehaviorRepository
                .findByUserAndTimestampAfterOrderByTimestampDesc(user, since);
        
        if (behaviors.isEmpty()) {
            return false;
        }
        
        // Tính toán
        Map<Long, Integer> categoryScores = calculateCategoryScores(behaviors);
        List<Long> topCategories = getTopCategories(categoryScores, MAX_FAVORITE_CATEGORIES);
        Double avgPrice = calculateAveragePrice(behaviors);
        int engagementScore = calculateEngagementScore(behaviors);
        
        // Lưu
        UserPreference preference = userPreferenceRepository
                .findByUser(user)
                .orElse(new UserPreference());
        
        preference.setUser(user);
        preference.setFavoriteCategories(convertToJson(topCategories));
        preference.setAvgPriceRange(avgPrice);
        preference.setEngagementScore(engagementScore);
        preference.setLastUpdated(LocalDateTime.now());
        
        userPreferenceRepository.save(preference);
        return true;
    }
    
    /**
     * Tính điểm preference cho từng category (Weighted Scoring)
     */
    private Map<Long, Integer> calculateCategoryScores(List<UserBehavior> behaviors) {
        Map<Long, Integer> scores = new HashMap<>();
        
        for (UserBehavior behavior : behaviors) {
            if (behavior.getProduct() == null) continue;
            
            Long categoryId = behavior.getProduct().getCategory().getId();
            int weight = getWeight(behavior.getAction());
            scores.merge(categoryId, weight, Integer::sum);
        }
        
        return scores;
    }
    
    /**
     * Lấy top N categories
     */
    private List<Long> getTopCategories(Map<Long, Integer> scores, int limit) {
        return scores.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
    
    /**
     * Tính giá trung bình user thường xem
     */
    private Double calculateAveragePrice(List<UserBehavior> behaviors) {
        return behaviors.stream()
                .filter(b -> b.getProduct() != null)
                .mapToDouble(b -> b.getProduct().getPrice().doubleValue())
                .average()
                .orElse(0.0);
    }
    
    /**
     * Tính engagement score (0-100)
     * Công thức: min(100, tổng_điểm_trọng_số / 2)
     */
    private int calculateEngagementScore(List<UserBehavior> behaviors) {
        int totalScore = behaviors.stream()
                .mapToInt(b -> getWeight(b.getAction()))
                .sum();
        
        return Math.min(100, totalScore / 2);
    }
    
    /**
     * Convert list thành JSON string đơn giản
     */
    private String convertToJson(List<Long> list) {
        if (list == null || list.isEmpty()) return "[]";
        return "[" + list.stream().map(String::valueOf).collect(Collectors.joining(",")) + "]";
    }
}
