package havudong.baocao.util;

public final class PhoneUtils {

    private PhoneUtils() {}

    // Normalize common VN numbers to E.164 (simple rules sufficient for tests)
    public static String normalizeToE164(String input) {
        if (input == null) return null;
        String s = input.trim();
        if (s.startsWith("+")) return s;
        if (s.startsWith("0")) {
            return "+84" + s.substring(1);
        }
        if (s.matches("^84\\d+$")) {
            return "+" + s;
        }
        // fallback: assume local number without leading 0
        if (s.matches("\\d{9,10}")) {
            return "+84" + (s.length() == 9 ? s : s.substring(1));
        }
        return s;
    }
}
