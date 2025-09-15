package com.retailai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
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
                // 🚫 Disable CSRF entirely (TEMPORARY)
                .csrf(csrf -> csrf.disable())

                .cors(Customizer.withDefaults())
                .httpBasic(h -> h.disable())

                .authorizeHttpRequests(auth -> auth
                        // Public pages & assets
                        .requestMatchers("/", "/index.html", "/css/**", "/js/**", "/images/**", "/fonts/**", "/favicon.ico").permitAll()
                        // Auth endpoints
                        .requestMatchers("/auth/**").permitAll()
                        // Public API endpoint(s) — narrow if needed
                        .requestMatchers(HttpMethod.POST, "/api/actions/leads").permitAll()
                        .requestMatchers("/api/**").permitAll()

                        // Account endpoints now work without CSRF
                        .requestMatchers(HttpMethod.GET, "/account/me").authenticated()
                        .requestMatchers("/account/**").authenticated()

                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/")
                        .loginProcessingUrl("/auth/login")
                        .defaultSuccessUrl("/dashboard.html", true)
                        .failureUrl("/?error")
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessUrl("/?logout")
                        .deleteCookies("JSESSIONID", "XSRF-TOKEN")
                        .invalidateHttpSession(true)
                        .permitAll()
                )

                // ✅ Keep your CSP (adjust connect-src as needed for your API origin)
                .headers(h -> h
                        .frameOptions(f -> f.sameOrigin())
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; " +
                                        "base-uri 'self'; object-src 'none'; " +
                                        "script-src 'self' 'unsafe-inline'; " +
                                        // allow CSS from Google Fonts and cdnjs
                                        "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://cdnjs.cloudflare.com; " +
                                        "style-src-elem 'self' 'unsafe-inline' https://fonts.googleapis.com https://cdnjs.cloudflare.com; " +
                                        // allow font files from Google Fonts and cdnjs (FA webfonts)
                                        "font-src 'self' https://fonts.gstatic.com https://cdnjs.cloudflare.com data:; " +
                                        "media-src 'self' data: blob: https://precallai.s3.ap-south-1.amazonaws.com https://*.s3.ap-south-1.amazonaws.com; " +
                                        "img-src 'self' data: https://placehold.co; " +
                                        "connect-src 'self' https://ai-production-40f7.up.railway.app https://localhost:8089; " +
                                        "frame-ancestors 'self'"
                        ))
                );

        // IMPORTANT: since CSRF is disabled, **remove** any CsrfCookieFilter you had added
        // e.g. remove: .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)

        return http.build();
    }

    // CORS stays — needed if UI calls a different origin and uses cookies
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOriginPatterns(List.of("http://localhost:*", "https://*"));
        c.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS","PATCH"));
        c.setAllowedHeaders(List.of("Content-Type","Authorization","Accept","X-Requested-With"));
        c.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();
        s.registerCorsConfiguration("/**", c);
        return s;
    }
}
