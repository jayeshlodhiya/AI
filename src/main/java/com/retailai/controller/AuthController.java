package com.retailai.controller;




import com.retailai.model.AppUser;
import com.retailai.repo.AppUserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final AppUserRepository repo;
    private final BCryptPasswordEncoder encoder;

    public AuthController(AppUserRepository repo, BCryptPasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    // Sign Up form submit
    @PostMapping("/signup")
    @ResponseBody
    public ResponseEntity<?> signup(@RequestParam String username,
                                    @RequestParam String password,
                                    @RequestParam(required = false) String fullName) {
        if (repo.existsByUsername(username)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username already exists"));
        }
        AppUser u = new AppUser();
        u.setUsername(username);
        u.setPasswordHash(encoder.encode(password));
        u.setFullName(fullName);
        repo.save(u);
        // Return JSON; the index.html JS can redirect to /?registered#login
        return ResponseEntity.created(URI.create("/?registered")).body(Map.of("ok", true));
    }
}
