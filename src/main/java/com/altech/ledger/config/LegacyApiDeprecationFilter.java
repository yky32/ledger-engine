package com.altech.ledger.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Marks wallet-rail compatibility paths so clients can migrate to {@code /movements/*}.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class LegacyApiDeprecationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        filterChain.doFilter(request, response);
        String path = request.getRequestURI();
        if (path == null) {
            return;
        }
        if (path.startsWith("/ledger/") || path.startsWith("/webhooks/ledger-wallets/")) {
            response.addHeader("Deprecation", "true");
            response.addHeader("Sunset", "2027-01-01");
            response.addHeader("Link", "</movements/deposits>; rel=\"successor-version\"");
        }
    }
}
