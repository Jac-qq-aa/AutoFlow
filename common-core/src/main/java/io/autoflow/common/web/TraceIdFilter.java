package io.autoflow.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TraceIdFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        var incoming = request.getHeader(HEADER);
        var traceId = incoming == null || incoming.isBlank() ? UUID.randomUUID().toString().replace("-", "") : incoming;
        MDC.put("traceId", traceId);
        response.setHeader(HEADER, traceId);
        try { chain.doFilter(request, response); } finally { MDC.remove("traceId"); }
    }
}

