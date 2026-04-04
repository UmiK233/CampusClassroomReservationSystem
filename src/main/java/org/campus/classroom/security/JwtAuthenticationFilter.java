package org.campus.classroom.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.campus.classroom.utils.JwtUtil;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtils;
    private final JWTUserDetailsService jwtUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            // 1. 解析 Token 拿到用户名 (如果解析失败会抛异常，说明 Token 伪造)
            String username = jwtUtils.parseToken(token).get("username").toString();
            //token 解析成功说明它基本合法 是否要结合权限信息、账号状态等自行抉择
            UserDetails userDetails = jwtUserDetailsService.loadUserByUsername(username);
            // 2. 既然 Token 是真的，我就直接信任这个 username
            // 我们手动给它盖个“已认证”的红章（传入权限列表，哪怕是空的）
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, Collections.emptyList());

            // 3. 塞进 Context，后面的过滤器就会认为这个请求是合法的
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }
}
