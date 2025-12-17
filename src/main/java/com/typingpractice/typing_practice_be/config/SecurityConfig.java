package com.typingpractice.typing_practice_be.config;

import com.typingpractice.typing_practice_be.common.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션 안 씀
        .authorizeHttpRequests(
            auth -> auth.anyRequest().permitAll()
            //                auth.requestMatchers(
            //                        "/swagger-ui/**",
            //                        "/swagger-ui.html",
            //                        "/swagger-ui/index.html",
            //                        "/api-docs/**",
            //                        "/api-docs",
            //                        "/api-docs/swagger-config",
            //                        "/swagger-resources/**",
            //                        "/webjars/**")
            //                    .permitAll()
            //                    .requestMatchers("/members/login")
            //                    .permitAll() // 로그인은 인증 없이
            //                    .anyRequest()
            //                    .authenticated() // 나머지는 인증 필요
            )
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}
