package com.retailai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

// SecurityConfig.java (Spring Boot 3 / Security 6)
@Configuration
public class SecurityConfig {

    @Bean
    BCryptPasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/auth/signup", "/auth/login", "/auth/logout")) // ← add logout
                .httpBasic(h -> h.disable())
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/", "/index.html", "/css/**", "/js/**", "/images/**", "/auth/**").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(f -> f
                        .loginPage("/")
                        .loginProcessingUrl("/auth/login")
                        .defaultSuccessUrl("/rs.html", true)
                        .failureUrl("/?error")
                        .permitAll()
                )
                .logout(l -> l
                        .logoutUrl("/auth/logout")        // POST
                        .logoutSuccessUrl("/?logout")
                        .deleteCookies("JSESSIONID")
                        .invalidateHttpSession(true)
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessUrl("/?logout")
                        .deleteCookies("JSESSIONID")
                        .invalidateHttpSession(true)
                        .permitAll()
                )
                .addFilterAfter(new CsrfCookieFilter(), org.springframework.security.web.csrf.CsrfFilter.class)
        // ✅ Allow embedding from specific parent origins
                .headers(h -> h
                        .frameOptions(f -> f.sameOrigin())   // or keep disabled + CSP 'self'
                        .contentSecurityPolicy(csp -> csp.policyDirectives("frame-ancestors 'self'"))
                );

        return http.build();
    }

}
