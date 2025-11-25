package com.typingpractice.typing_practice_be.common;

import java.time.LocalDateTime;

public record ApiResponse<T>(boolean success, T data, LocalDateTime timestamp) {
  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>(true, data, LocalDateTime.now());
  }
}
