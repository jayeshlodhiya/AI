// src/main/java/com/retailai/config/CsrfCookieFilter.java
package com.retailai.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import java.io.IOException;

public class CsrfCookieFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (token != null) {
            Cookie existing = WebUtils.getCookie(request, "XSRF-TOKEN");
            String value = token.getToken();
            if (existing == null || !value.equals(existing.getValue())) {
                Cookie c = new Cookie("XSRF-TOKEN", value);
                c.setPath("/");             // required so it’s sent back
                c.setHttpOnly(false);       // JS must read it if same-origin
                c.setSecure(request.isSecure());
                response.addCookie(c);
            }
        }
        filterChain.doFilter(request, response);
    }
}
