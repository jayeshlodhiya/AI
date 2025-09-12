package com.retailai.security;



import com.retailai.model.AppUser;
import com.retailai.repo.AppUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CurrentUser {
    private final AppUserRepository repo;

    public CurrentUser(AppUserRepository repo) { this.repo = repo; }

    public Optional<AppUser> get() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated()) return Optional.empty();
        String username = a.getName();
        return repo.findByUsername(username);
    }

    public AppUser require() {
        return get().orElseThrow(() -> new IllegalStateException("Unauthenticated"));
    }

    public String username() {
        return get().map(AppUser::getUsername).orElse(null);
    }

    public Long idOrNull() {
        return get().map(AppUser::getId).orElse(null);
    }
}
