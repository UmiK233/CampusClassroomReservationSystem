package org.campus.classroom.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.campus.classroom.common.Result;
import org.campus.classroom.enums.ResultCode;
import org.campus.classroom.utils.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final JWTUserDetailsService jwtUserDetailsService;
    private final Logger log= LoggerFactory.getLogger(this.getClass());

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return "/auth/login".equals(path) || "/auth/register".equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // 没带 token：不认证，直接继续
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("Filter: 没有 Bearer Token，跳过JWT认证");
            filterChain.doFilter(request, response);
            return;
        }

        // 已经有认证信息：直接继续
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            log.debug("Filter: 已有认证信息，直接放行");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authHeader.substring(7);
            String username = jwtUtil.parseToken(token).get("username").toString();

            //做用户存在/账号密码是否正确/状态校验
            UserDetails loginUser = jwtUserDetailsService.loadUserByUsername(username);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            loginUser,
                            null,
                            loginUser.getAuthorities()
                    );

            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            log.debug("Filter: Token解析失败，错误信息: {}", e.getMessage());
            returnUnauthorizedResponse(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void returnUnauthorizedResponse(HttpServletResponse response) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
        new ObjectMapper().writeValue(response.getWriter(), Result.fail(ResultCode.UNAUTHORIZED));
    }
}
