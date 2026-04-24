package org.campus.classroom.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
public class RequestTraceIdFilter extends OncePerRequestFilter {

    private static final String TRACE_ID = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String traceId = UUID.randomUUID().toString().replace("-", "");
        long start = System.currentTimeMillis();

        try {
            MDC.put(TRACE_ID, traceId);

            log.info("[请求开始] traceId={}, method={}, uri={}, query={}",
                    traceId,
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getQueryString());

            filterChain.doFilter(request, response);

            log.info("[请求结束] traceId={}, method={}, uri={}, status={}, cost={}ms",
                    traceId,
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    System.currentTimeMillis() - start);

        } catch (Exception e) {
            log.error("[请求异常] traceId={}, method={}, uri={}, message={}",
                    traceId,
                    request.getMethod(),
                    request.getRequestURI(),
                    e.getMessage(),
                    e);
            throw e;
        } finally {
            MDC.remove(TRACE_ID);
        }
    }
}