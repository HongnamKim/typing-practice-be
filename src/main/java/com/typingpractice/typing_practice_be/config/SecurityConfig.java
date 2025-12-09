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
        .headers(headers -> headers.frameOptions(frame -> frame.disable()))
        .authorizeHttpRequests(
            auth -> auth.anyRequest().permitAll()
            //                auth.requestMatchers("/h2-console/**")
            //                    .permitAll()
            //                    .requestMatchers(
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

  //  @Bean
  //  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
  //    http.csrf(csrf -> csrf.disable())
  //        .headers(headers -> headers.frameOptions(frame -> frame.disable()))
  //        .authorizeHttpRequests(
  //            auth ->
  //                auth.requestMatchers("/h2-console/**")
  //                    .permitAll()
  //                    .anyRequest()
  //                    .permitAll() // 일단 전체 허용, 나중에 수정
  //            );
  //
  //    return http.build();
  //  }
}
