package com.skmcore.orderservice.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        long start = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            log.info("[{}] {} {} → {} ({}ms)",
                    MDC.get(CorrelationIdFilter.MDC_KEY),
                    request.getMethod(),
                    buildUri(request),
                    response.getStatus(),
                    elapsedMs);
        }
    }

    private static String buildUri(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String qs = request.getQueryString();
        return (qs != null && !qs.isBlank()) ? uri + "?" + qs : uri;
    }
}
