package com.typingpractice.typing_practice_be.quote.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
@RequiredArgsConstructor
public class SentenceHashGenerator {
  public String generate(String sentence) {
    String normalized = normalize(sentence);
    return sha256(normalized);
  }

  private String normalize(String sentence) {
    return sentence.trim().replaceAll("\\s+", " ");
  }

  private String sha256(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      return bytesToHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
    }
  }

  private String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }

    return sb.toString();
  }
}
