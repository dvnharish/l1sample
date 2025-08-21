package com.example.converge.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RedactionLoggingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RedactionLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } finally {
            String path = request.getRequestURI();
            if (path != null && path.contains("/api/v1/payments/sale")) {
                log.info("Handled sale request: path={} status={}", path, response.getStatus());
            }
        }
    }
}


