package havudong.baocao.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {
    
    private final PasswordEncoder passwordEncoder;
    
    @GetMapping("/hash-password")
    public Map<String, String> hashPassword(@RequestParam String password) {
        String hash = passwordEncoder.encode(password);
        Map<String, String> result = new HashMap<>();
        result.put("password", password);
        result.put("hash", hash);
        return result;
    }
    
    @PostMapping("/verify-password")
    public Map<String, Object> verifyPassword(@RequestParam String password, @RequestParam String hash) {
        boolean matches = passwordEncoder.matches(password, hash);
        Map<String, Object> result = new HashMap<>();
        result.put("password", password);
        result.put("hash", hash);
        result.put("matches", matches);
        return result;
    }
}
