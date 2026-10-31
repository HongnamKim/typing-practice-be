package com.typingpractice.typing_practice_be.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.typingpractice.typing_practice_be.BaseE2ETest;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ActuatorHealthE2ETest extends BaseE2ETest {

  @Test
  @DisplayName("헬스체크는 인증 없이 호출할 수 있다")
  void healthIsPublic() {
    ResponseEntity<Map> response = restTemplate.getForEntity("/actuator/health", Map.class);

    assertThat(response.getStatusCode()).isNotIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
  }

  @Test
  @DisplayName("헬스체크 외 다른 actuator 경로는 인증 없이 호출할 수 없다")
  void otherActuatorStaysProtected() {
    ResponseEntity<Map> response = restTemplate.getForEntity("/actuator/metrics", Map.class);

    assertThat(response.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
  }
}
