package havudong.until;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "123456";
        String hash = encoder.encode(password);
        
        System.out.println("============================================");
        System.out.println("Password: " + password);
        System.out.println("Hash: " + hash);
        System.out.println("============================================");
        
        // Test verify
        boolean matches = encoder.matches(password, hash);
        System.out.println("Verification test: " + matches);
        
        // Test với hash hiện tại trong SQL
        String currentHash = "$2a$10$3euPcmQFCiblsZeEu5s7p.3OxJqPLcPvBW/mZw.lXMQ1Gn4o2uqNi";
        boolean currentMatches = encoder.matches(password, currentHash);
        System.out.println("Current hash matches '123456': " + currentMatches);
    }
}
