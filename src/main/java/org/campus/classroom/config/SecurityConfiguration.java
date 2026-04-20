package org.campus.classroom.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.campus.classroom.common.Result;
import org.campus.classroom.enums.ResultCode;
import org.campus.classroom.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/login", "/auth/register", "/swagger-ui.html", "/swagger-ui/**", "/openapi.yaml","/v3/api-docs/**").permitAll() //放行登录接口不走过滤
                        .requestMatchers("/admin/**").hasRole("ADMIN") //只有ADMIN角色可以访问/admin接口
                        .anyRequest().authenticated()
                )
                //在UsernamePasswordAuthenticationFilter前添加jwtAuthenticationFilter，验证Token是否合法，如果合法就直接放行，不需要再走UsernamePasswordAuthenticationFilter了
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        //在过滤链拦截权限不足的请求，返回403错误和自定义的错误信息
        http.exceptionHandling(exception -> exception
                // 401：未登录 / token 无效 / 认证失败
                .authenticationEntryPoint((request, response, e) -> {
                    log.error("Filter: 未认证或Token无效: {}", e.getMessage());
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    new ObjectMapper().writeValue(
                            response.getWriter(),
                            Result.fail(ResultCode.UNAUTHORIZED, "未登录或Token无效")
                    );
                })
                // 403：已登录但权限不足
                .accessDeniedHandler((request, response, e) -> {
                    log.error("Filter: 权限不足，无法访问: {}", e.getMessage());
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    new ObjectMapper().writeValue(
                            response.getWriter(),
                            Result.fail(ResultCode.FORBIDDEN, "权限不足，无法访问")
                    );
                })
        );
        return http.build();
    }


}
