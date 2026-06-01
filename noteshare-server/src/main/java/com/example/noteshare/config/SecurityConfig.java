package com.example.noteshare.config;

import com.example.noteshare.common.ApiResponse;
import com.example.noteshare.common.ErrorCode;
import com.example.noteshare.security.JwtAuthFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置（双 SecurityFilterChain 方案）
 *
 * Chain 1（@Order 1，优先匹配）：公开接口，无需认证
 * Chain 2（@Order 2，兜底匹配）：受保护接口，需要 JWT 认证
 *
 * 关键设计：
 * - /api/users/{id} 使用 {id:[0-9]+} 正则，只匹配数字 ID
 *   这样 /api/users/me 不会命中 Chain 1，而是落到 Chain 2 需要认证
 * - Chain 1 的 securityMatchers 精确限定公开路径，避免过度放行
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Chain 1：公开接口（优先匹配）
     */
    @Bean
    @Order(1)
    public SecurityFilterChain publicChain(HttpSecurity http) throws Exception {
        http
            .securityMatchers(matchers -> matchers
                .requestMatchers(HttpMethod.POST, "/api/auth/**")
                .requestMatchers(HttpMethod.GET, "/api/notes/**")
                .requestMatchers(HttpMethod.GET, "/api/users/{id:[0-9]+}")
                .requestMatchers(HttpMethod.GET, "/api/users/{id:[0-9]+}/notes")
                .requestMatchers(HttpMethod.GET, "/uploads/**")
            )
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm ->
                sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }

    /**
     * Chain 2：受保护接口（兜底匹配，需要 JWT 认证）
     */
    @Bean
    @Order(2)
    public SecurityFilterChain protectedChain(HttpSecurity http,
                                               JwtAuthFilter jwtAuthFilter,
                                               ObjectMapper objectMapper) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm ->
                sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    response.setStatus(401);
                    response.getWriter().write(objectMapper.writeValueAsString(
                        ApiResponse.error(ErrorCode.AUTH_TOKEN_MISSING)));
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    response.setStatus(403);
                    response.getWriter().write(objectMapper.writeValueAsString(
                        ApiResponse.error(ErrorCode.AUTH_FORBIDDEN)));
                })
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
