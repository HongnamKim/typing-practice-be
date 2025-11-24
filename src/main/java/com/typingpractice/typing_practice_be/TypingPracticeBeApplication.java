package com.typingpractice.typing_practice_be;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class TypingPracticeBeApplication {

  public static void main(String[] args) {

    SpringApplication.run(TypingPracticeBeApplication.class, args);
  }
}
