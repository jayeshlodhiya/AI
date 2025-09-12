package com.retailai.controller;




import com.retailai.api.dto.PerformResponse;
import com.retailai.api.dto.UpdatePasswordRequest;
import com.retailai.api.dto.UpdateQcallKeyRequest;
import com.retailai.api.dto.UserProfileResponse;
import com.retailai.service.UserAccountService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/account", produces = MediaType.APPLICATION_JSON_VALUE)
public class AccountController {

    private final UserAccountService svc;

    public AccountController(UserAccountService svc) {
        this.svc = svc;
    }
    @GetMapping("/me")
    public UserProfileResponse me(Authentication auth) {
        String username = auth != null ? auth.getName() : null;
        if (username == null) throw new IllegalStateException("Unauthenticated.");
        return svc.me(username);
    }


    @PostMapping(path = "/password", consumes = MediaType.APPLICATION_JSON_VALUE)
    public PerformResponse updatePassword(@Valid @RequestBody UpdatePasswordRequest req, Authentication auth) {
        String username = auth != null ? auth.getName() : null;
        if (username == null) throw new IllegalStateException("Unauthenticated.");
        svc.updatePassword(username, req.currentPassword(), req.newPassword(), req.confirmPassword());
        return new PerformResponse(true, "Password updated.");
    }

    @PostMapping(path = "/qcall-key", consumes = MediaType.APPLICATION_JSON_VALUE)
    public PerformResponse updateQcallKey(@Valid @RequestBody UpdateQcallKeyRequest req, Authentication auth) {
        String username = auth != null ? auth.getName() : null;
        if (username == null) throw new IllegalStateException("Unauthenticated.");
        svc.updateQcallApiKey(username, req.qcallApiKey());
        return new PerformResponse(true, "API key saved.");
    }

    @PostMapping(path = "/sessions/revoke")
    public PerformResponse revokeOtherSessions(Authentication auth) {
        String username = auth != null ? auth.getName() : null;
        if (username == null) throw new IllegalStateException("Unauthenticated.");
        svc.revokeOtherSessions(username);
        return new PerformResponse(true, "Other sessions revoked.");
    }
}
