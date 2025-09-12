package com.retailai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // ---- CSRF ----
                // Keep CSRF for browser sessions. Expose token via cookie so JS can send X-XSRF-TOKEN.
                .csrf(csrf -> csrf
                                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                                // Do NOT require CSRF on stateless/public APIs:
                                .ignoringRequestMatchers(
                                        new AntPathRequestMatcher("/api/**"),
                                        new AntPathRequestMatcher("/auth/signup"),
                                        new AntPathRequestMatcher("/auth/login"),
                                        new AntPathRequestMatcher("/auth/logout")
                                )
                        // If you use pure JWT (no cookies) for /account/**, you can also ignore CSRF there:
                        // .ignoringRequestMatchers(new AntPathRequestMatcher("/account/**"))
                )

                .cors(Customizer.withDefaults())
                .httpBasic(h -> h.disable())

                // ---- AUTHZ ----
                .authorizeHttpRequests(auth -> auth
                        // Public pages & static
                        .requestMatchers("/", "/index.html", "/css/**", "/js/**", "/images/**", "/fonts/**", "/favicon.ico").permitAll()
                        // Auth endpoints
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/csrf-token").permitAll()

                        // Public API (narrow if needed)
                        .requestMatchers(HttpMethod.POST, "/api/actions/leads").permitAll()
                        .requestMatchers("/api/**").permitAll()

                        // Account endpoints must be logged in (frontend will send CSRF token)
                        .requestMatchers(HttpMethod.GET, "/account/me").authenticated()
                        .requestMatchers("/account/**").authenticated()

                        // Everything else requires login
                        .anyRequest().authenticated()
                )

                // ---- LOGIN / LOGOUT ----
                .formLogin(form -> form
                        .loginPage("/")                 // your SPA/home can host login modal
                        .loginProcessingUrl("/auth/login")
                        .defaultSuccessUrl("/home.html", true)
                        .failureUrl("/?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")      // POST by default
                        .logoutSuccessUrl("/?logout")
                        .deleteCookies("JSESSIONID", "XSRF-TOKEN")
                        .invalidateHttpSession(true)
                        .permitAll()
                )

                // Ensure the XSRF-TOKEN cookie is set after token is created (keep your filter)
                .addFilterAfter(new CsrfCookieFilter(), org.springframework.security.web.csrf.CsrfFilter.class)

                // ---- SECURITY HEADERS / CSP ----
                .headers(h -> h
                        .frameOptions(f -> f.sameOrigin())
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; " +
                                        "base-uri 'self'; object-src 'none'; " +
                                        "script-src 'self' 'unsafe-inline'; " +
                                        "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                                        "style-src-elem 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                                        "font-src 'self' https://fonts.gstatic.com data:; " +
                                        "media-src 'self' data: blob: https://precallai.s3.ap-south-1.amazonaws.com https://*.s3.ap-south-1.amazonaws.com; " +
                                        "img-src 'self' data: https://placehold.co; " +
                                        // 👇 allow your API(s) here (HTTPS strongly recommended)
                                        "connect-src 'self' https://ai-production-40f7.up.railway.app https://localhost:8089 wss://ai-production-40f7.up.railway.app; " +
                                        "frame-ancestors 'self'"
                        ))
                );


        return http.build();
    }

    // ---- CORS (dev-friendly) ----
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOriginPatterns(List.of("http://localhost:*","http://ai-production-40f7.up.railway.app", "https://*"));
        c.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS","PATCH"));
        // Allow both common CSRF header names + auth
        c.setAllowedHeaders(List.of("Content-Type","X-XSRF-TOKEN","X-CSRF-TOKEN","Authorization"));
        c.setExposedHeaders(List.of("X-XSRF-TOKEN","X-CSRF-TOKEN"));
        c.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();
        s.registerCorsConfiguration("/**", c);
        return s;
    }
}
