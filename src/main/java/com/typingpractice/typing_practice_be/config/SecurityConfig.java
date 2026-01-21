package com.typingpractice.typing_practice_be.config;

import com.typingpractice.typing_practice_be.auth.service.AuthService;
import com.typingpractice.typing_practice_be.common.jwt.JwtAuthenticationFilter;
import com.typingpractice.typing_practice_be.common.jwt.JwtTokenProvider;
import com.typingpractice.typing_practice_be.common.security.CustomAccessDeniedHandler;
import com.typingpractice.typing_practice_be.common.security.CustomAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtTokenProvider jwtTokenProvider;
  private final AuthService authService;
  private final CustomAuthenticationEntryPoint authenticationEntryPoint;
  private final CustomAccessDeniedHandler accessDeniedHandler;

  @Bean
  public JwtAuthenticationFilter jwtAuthenticationFilter(
      JwtTokenProvider jwtTokenProvider, AuthService authService) {
    return new JwtAuthenticationFilter(jwtTokenProvider, authService);
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.cors(Customizer.withDefaults())
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션 안 씀
        .authorizeHttpRequests(
            auth ->
                auth
                    // 인증 불필요
                    .requestMatchers("/swagger-ui/**", "/api-docs/**")
                    .permitAll()
                    .requestMatchers("/h2-console/**")
                    .permitAll()
                    .requestMatchers("/auth/google")
                    .permitAll()
                    .requestMatchers("/auth/test")
                    .permitAll()
                    .requestMatchers("/auth/refresh")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/quotes")
                    .permitAll()
                    .requestMatchers("/error")
                    .permitAll()

                    // 관리자 전용
                    .requestMatchers("/admin/**")
                    .hasRole("ADMIN")

                    // BANNED 제외 (USER, ADMIN만)
                    .requestMatchers(HttpMethod.POST, "/quotes/public")
                    .hasAnyRole("USER", "ADMIN")
                    .requestMatchers(HttpMethod.POST, "/quotes/*/publish")
                    .hasAnyRole("USER", "ADMIN")
                    .requestMatchers(HttpMethod.POST, "/reports")
                    .hasAnyRole("USER", "ADMIN")
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            ex ->
                ex.authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
        .addFilterBefore(
            jwtAuthenticationFilter(jwtTokenProvider, authService),
            UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of(("http://localhost:3000")));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
    configuration.setAllowedHeaders(List.of("*"));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
