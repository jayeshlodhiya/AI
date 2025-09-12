package com.retailai.service;



import com.retailai.api.dto.UserProfileResponse;
import com.retailai.model.AppUser;

import com.retailai.repo.AppUserRepository;
import com.retailai.service.UserAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class UserAccountServiceImpl implements UserAccountService {

    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final SessionRegistry sessionRegistry; // may be null

    @Autowired
    public UserAccountServiceImpl(AppUserRepository users,
                                  PasswordEncoder encoder,
                                  @Autowired(required = false) SessionRegistry sessionRegistry) {
        this.users = users;
        this.encoder = encoder;
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    @Transactional
    public void updatePassword(String username, String currentPassword, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new ResponseStatusException(BAD_REQUEST, "New password and confirmation do not match.");
        }
        AppUser u = users.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "User not found."));

        if (!u.isEnabled()) throw new ResponseStatusException(UNAUTHORIZED, "User is disabled.");

        if (u.getPasswordHash() == null || !encoder.matches(currentPassword, u.getPasswordHash())) {
            throw new ResponseStatusException(UNAUTHORIZED, "Current password is incorrect.");
        }
        u.setPasswordHash(encoder.encode(newPassword));
        users.save(u);
    }

    @Override
    @Transactional
    public void updateQcallApiKey(String username, String apiKey) {
        AppUser u = users.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "User not found."));
        // NOTE: consider encrypting apiKey at rest if required
        u.setQcallApiKey(apiKey);
        users.save(u);
    }

    @Override
    public void revokeOtherSessions(String username) {
        if (sessionRegistry == null) return; // no-op
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            if (principal != null && principal.toString().equalsIgnoreCase(username)) {
                for (SessionInformation si : sessionRegistry.getAllSessions(principal, false)) {
                    si.expireNow();
                }
            }
        }
    }
    @Override
    public UserProfileResponse me(String username) {
        AppUser u = users.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "User not found."));
        String email = username != null && username.contains("@") ? username : null; // if you store email in username
        String maskedKey = maskLast4(u.getQcallApiKey());
        return new UserProfileResponse(u.getId(), u.getUsername(), email, u.getFullName(), u.isEnabled(), maskedKey);
    }

    private String maskLast4(String v) {
        if (v == null || v.isBlank()) return null;
        if (v.length() <= 4) return "••••";
        return "••••••••••••••••" + v.substring(v.length() - 4);
    }
}
