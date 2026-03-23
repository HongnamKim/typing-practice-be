package com.typingpractice.typing_practice_be.member.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Getter
@ConfigurationProperties(prefix = "admin")
public class AdminProperties {
  private final List<String> emails;

  public AdminProperties(List<String> emails) {
    this.emails = emails != null ? emails : List.of();
  }
}
