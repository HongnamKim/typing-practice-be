package com.typingpractice.typing_practice_be.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .headers(headers -> headers.frameOptions(frame -> frame.disable()))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/h2-console/**")
                    .permitAll()
                    .anyRequest()
                    .permitAll() // 일단 전체 허용, 나중에 수정
            );

    return http.build();
  }
}
