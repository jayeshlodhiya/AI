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
                        // Allow leads (and other API) without auth – or narrow to just the exact endpoint:
                        .requestMatchers(HttpMethod.POST, "/api/actions/leads").permitAll()
                        .requestMatchers("/api/**").permitAll()
                        // Everything else requires login (e.g., rs.html)
                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/")                 // your index hosts the modal
                        .loginProcessingUrl("/auth/login")
                        .defaultSuccessUrl("/rs.html", true)
                        .failureUrl("/?error")
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/auth/logout")      // expect POST (default); you can allow GET via requestMatcher if needed
                        .logoutSuccessUrl("/?logout")
                        .deleteCookies("JSESSIONID")
                        .invalidateHttpSession(true)
                        .permitAll()
                )

                // Send CSRF cookie after framework sets token
                .addFilterAfter(new CsrfCookieFilter(), org.springframework.security.web.csrf.CsrfFilter.class)

                // Headers: allow same-origin iframes and a single-line CSP (avoid CR/LF)
                .headers(h -> h
                        .frameOptions(f -> f.sameOrigin())
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; style-src 'self' 'unsafe-inline'; script-src 'self' 'unsafe-inline'; img-src 'self' data:; frame-ancestors 'self'")
                        )
                );

        return http.build();
    }

    // Optional: CORS (helpful during dev if you ever split origins)
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
