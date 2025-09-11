package com.retailai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
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
                // CSRF: keep for browser pages (cookie-based), but ignore for JSON APIs
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers(new AntPathRequestMatcher("/api/**"))
                        .ignoringRequestMatchers(
                                new AntPathRequestMatcher("/auth/signup"),
                                new AntPathRequestMatcher("/auth/login"),
                                new AntPathRequestMatcher("/auth/logout")
                        )
                )
                .cors(Customizer.withDefaults())
                .httpBasic(h -> h.disable())

                .authorizeHttpRequests(auth -> auth
                        // Public pages & assets
                        .requestMatchers("/", "/index.html", "/css/**", "/js/**", "/images/**", "/fonts/**", "/favicon.ico").permitAll()
                        // Auth endpoints are public
                        .requestMatchers("/auth/**").permitAll()
                        // Public API endpoint(s) — narrow if needed
                        .requestMatchers(HttpMethod.POST, "/api/actions/leads").permitAll()
                        .requestMatchers("/api/**").permitAll()
                        // Everything else requires login
                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/")                 // index hosts the modal/login
                        .loginProcessingUrl("/auth/login")
                        .defaultSuccessUrl("/home.html", true)
                        .failureUrl("/?error")
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/auth/logout")      // POST by default
                        .logoutSuccessUrl("/?logout")
                        .deleteCookies("JSESSIONID")
                        .invalidateHttpSession(true)
                        .permitAll()
                )

                // Send CSRF cookie after framework sets token
                .addFilterAfter(new CsrfCookieFilter(), org.springframework.security.web.csrf.CsrfFilter.class)

                .headers(h -> h
                        .frameOptions(f -> f.sameOrigin())
                        .contentSecurityPolicy(csp -> csp
                                // SINGLE LINE (no CR/LF) — includes Google Fonts + placehold.co
                                .policyDirectives(
                                        "default-src 'self'; " +
                                                "base-uri 'self'; " +
                                                "object-src 'none'; " +
                                                // Inline scripts/styles are kept because your pages use them.
                                                "script-src 'self' 'unsafe-inline'; " +
                                                "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                                                "style-src-elem 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                                                // Allow Google Fonts files and data: for self-hosted/base64 fonts
                                                "font-src 'self' https://fonts.gstatic.com data:; " +
                                                // Allow data: images and placeholder CDN
                                                "img-src 'self' data: https://placehold.co; " +
                                                // XHR/fetch targets (same-origin API). Add more if you call other hosts.
                                                "connect-src 'self'; " +
                                                // Prevent this app from being framed by other origins
                                                "frame-ancestors 'self'"
                                )
                        )
                );

        return http.build();
    }

    // Optional: CORS (useful during dev if you hit from other origins)
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOriginPatterns(List.of("http://localhost:*", "https://*"));
        c.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
        c.setAllowedHeaders(List.of("Content-Type","X-XSRF-TOKEN","Authorization"));
        c.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();
        s.registerCorsConfiguration("/**", c);
        return s;
    }
}
