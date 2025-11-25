package com.typingpractice.typing_practice_be.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI()
        .info(
            new Info().title("Typing Practice API").description("타이핑 연습 서비스 API").version("1.0.0"));
  }
}
