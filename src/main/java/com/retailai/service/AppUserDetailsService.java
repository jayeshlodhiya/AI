package com.retailai.service;




import com.retailai.model.AppUser;
import com.retailai.repo.AppUserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppUserDetailsService implements UserDetailsService {
    private final AppUserRepository repo;
    public AppUserDetailsService(AppUserRepository repo) { this.repo = repo; }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser u = repo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new User(u.getUsername(), u.getPasswordHash(), u.isEnabled(),
                true, true, true, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
