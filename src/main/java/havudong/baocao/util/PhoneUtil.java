package havudong.baocao.util;

public final class PhoneUtil {

    private PhoneUtil() {}

    // Convert any user provided phone to international numeric form without plus, e.g., 0xxxxxxxx -> 84xxxxxxxx
    public static String toInternational(String phone) {
        if (phone == null) return null;
        String d = phone.replaceAll("\\D", "");
        if (d.isEmpty()) return d;
        // already international with country code
        if (d.startsWith("84")) return d;
        // starts with leading zero
        if (d.startsWith("0")) return "84" + d.substring(1);
        // may start with '+' (handled by regex) or other; default prepend 84 if length looks like local
        if (d.length() == 9 || d.length() == 10) return "84" + d;
        return d;
    }

    // Convert international form to local (0...) e.g., 84xxxxxxxx -> 0xxxxxxxx
    public static String toLocal(String phone) {
        if (phone == null) return null;
        String d = phone.replaceAll("\\D", "");
        if (d.startsWith("84") && d.length() > 2) return "0" + d.substring(2);
        return d;
    }
}