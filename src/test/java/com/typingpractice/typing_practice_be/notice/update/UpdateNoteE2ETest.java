package com.typingpractice.typing_practice_be.notice.update;

import static org.assertj.core.api.Assertions.*;

import com.typingpractice.typing_practice_be.BaseE2ETest;
import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.common.dto.CursorPage;
import com.typingpractice.typing_practice_be.common.utils.TimeUtils;
import com.typingpractice.typing_practice_be.notice.dto.LocalizedTextRequest;
import com.typingpractice.typing_practice_be.notice.update.dto.CreateUpdateNoteRequest;
import com.typingpractice.typing_practice_be.notice.update.dto.UpdateNoteCursor;
import com.typingpractice.typing_practice_be.notice.update.dto.UpdateNoteResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;

public class UpdateNoteE2ETest extends BaseE2ETest {
  private static final ParameterizedTypeReference<ApiResponse<UpdateNoteResponse>> DETAIL_RESPONSE =
      new ParameterizedTypeReference<>() {};
  private static final ParameterizedTypeReference<
          ApiResponse<CursorPage<UpdateNoteResponse, UpdateNoteCursor>>>
      PAGE_RESPONSE = new ParameterizedTypeReference<>() {};

  @Autowired private JdbcTemplate jdbcTemplate;

  @AfterEach
  void cleanUp() {
    jdbcTemplate.execute("DELETE FROM update_note");
  }

  private HttpHeaders getAdminHeaders() {
    String adminToken = getAccessToken(ADMIN_PROVIDER_ID);
    return createAuthHeader(adminToken);
  }

  /** 어드민으로 업데이트 노트 등록 후 id 반환. (사용자 측에는 등록 API 없음) */
  private Long createUpdateNote(String version, LocalDateTime releasedAtKst, boolean published) {
    HttpHeaders adminHeaders = getAdminHeaders();

    CreateUpdateNoteRequest request =
        new CreateUpdateNoteRequest(
            version,
            releasedAtKst,
            List.of(new LocalizedTextRequest("새 기능", null, null)),
            List.of(new LocalizedTextRequest("개선", null, null)),
            published);

    ResponseEntity<ApiResponse<UpdateNoteResponse>> response =
        restTemplate.exchange(
            "/admin/update-notes",
            HttpMethod.POST,
            new HttpEntity<>(request, adminHeaders),
            DETAIL_RESPONSE);

    assert response.getBody() != null;
    return response.getBody().data().id();
  }

  @Nested
  @DisplayName("GET /update-notes/latest - 최신 게시 업데이트 노트 1건")
  class GetLatest {
    @Test
    @DisplayName("성공 - 최신 게시 노트 반환 (KST->UTC 응답)")
    void success() {
      LocalDateTime base = LocalDateTime.of(2026, 5, 1, 10, 0);

      createUpdateNote("v1.0.0", base, true);
      createUpdateNote("v1.0.1", base.plusDays(1), true);
      LocalDateTime latestKst = base.plusDays(2);
      Long latestId = createUpdateNote("v1.0.2", latestKst, true);

      ResponseEntity<ApiResponse<UpdateNoteResponse>> response =
          restTemplate.exchange("/update-notes/latest", HttpMethod.GET, null, DETAIL_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assert response.getBody() != null;

      UpdateNoteResponse data = response.getBody().data();
      assertThat(data).isNotNull();
      assertThat(data.id()).isEqualTo(latestId);
      assertThat(data.version()).isEqualTo("v1.0.2");
      assertThat(data.releasedAt()).isEqualTo(TimeUtils.kstToUtc(latestKst));
    }

    @Test
    @DisplayName("성공 - 게시된 노트 없음 (data: null)")
    void emptyWhenNoPublished() {
      createUpdateNote("v1.0.0", LocalDateTime.now(), false);

      ResponseEntity<ApiResponse<UpdateNoteResponse>> response =
          restTemplate.exchange("/update-notes/latest", HttpMethod.GET, null, DETAIL_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assert response.getBody() != null;
      assertThat(response.getBody().success()).isTrue();
      assertThat(response.getBody().data()).isNull();
    }

    @Test
    @DisplayName("성공 - 미게시는 무시하고 그 이전 게시 노트 반환")
    void skipUnpublished() {
      LocalDateTime base = LocalDateTime.of(2026, 5, 1, 10, 0);
      Long publishedId = createUpdateNote("v1.0.0", base, true);
      createUpdateNote("v1.0.1", base.plusDays(1), false); // 최근 미게시

      ResponseEntity<ApiResponse<UpdateNoteResponse>> response =
          restTemplate.exchange("/update-notes/latest", HttpMethod.GET, null, DETAIL_RESPONSE);

      assert response.getBody() != null;
      UpdateNoteResponse data = response.getBody().data();
      assertThat(data).isNotNull();
      assertThat(data.id()).isEqualTo(publishedId);
    }

    @Test
    @DisplayName("성공 - 비인증으로도 200")
    void successWithoutToken() {
      createUpdateNote("v1.0.0", LocalDateTime.now(), true);

      ResponseEntity<ApiResponse<UpdateNoteResponse>> response =
          restTemplate.exchange("/update-notes/latest", HttpMethod.GET, null, DETAIL_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
  }

  @Nested
  @DisplayName("GET /update-notes - 사용자 목록 조회 (커서)")
  class getUpdateNotes {
    @Test
    @DisplayName("성공 - 페이지네이션 정확성 + 미게시 제외 검증")
    void successPagination() {
      // 5개 등록: 게시 4개, 미게시 1개
      LocalDateTime base = LocalDateTime.of(2026, 5, 1, 10, 0);
      Long id1 = createUpdateNote("v1.0.0", base, true);
      Long id2 = createUpdateNote("v1.0.1", base.plusDays(1), true);
      createUpdateNote("v1.0.2", base.plusDays(2), false); // 미게시 → 제외
      Long id4 = createUpdateNote("v1.0.3", base.plusDays(3), true);
      Long id5 = createUpdateNote("v1.0.4", base.plusDays(4), true);

      // 첫 페이지 (size=2) - 최신순이므로 id5, id4
      ResponseEntity<ApiResponse<CursorPage<UpdateNoteResponse, UpdateNoteCursor>>> page1 =
          restTemplate.exchange("/update-notes?size=2", HttpMethod.GET, null, PAGE_RESPONSE);

      assertThat(page1.getStatusCode()).isEqualTo(HttpStatus.OK);
      assert page1.getBody() != null;
      CursorPage<UpdateNoteResponse, UpdateNoteCursor> data1 = page1.getBody().data();
      assertThat(data1.getContent()).hasSize(2);
      assertThat(data1.getContent().get(0).id()).isEqualTo(id5);
      assertThat(data1.getContent().get(1).id()).isEqualTo(id4);
      assertThat(data1.isHasNext()).isTrue();
      assertThat(data1.getNextCursor()).isNotNull();
      assertThat(data1.getNextCursor().id()).isEqualTo(id4);

      // 두번째 페이지 - 미게시(v1.0.2) 건너뛰고 id2, id1
      UpdateNoteCursor cursor = data1.getNextCursor();
      ResponseEntity<ApiResponse<CursorPage<UpdateNoteResponse, UpdateNoteCursor>>> page2 =
          restTemplate.exchange(
              "/update-notes?size=2&cursorReleasedAt="
                  + cursor.releasedAt()
                  + "&cursorId="
                  + cursor.id(),
              HttpMethod.GET,
              null,
              PAGE_RESPONSE);

      assert page2.getBody() != null;
      CursorPage<UpdateNoteResponse, UpdateNoteCursor> data2 = page2.getBody().data();
      assertThat(data2.getContent()).hasSize(2);
      assertThat(data2.getContent().get(0).id()).isEqualTo(id2);
      assertThat(data2.getContent().get(1).id()).isEqualTo(id1);
      assertThat(data2.isHasNext()).isFalse();
      assertThat(data2.getNextCursor()).isNull();
    }

    @Test
    @DisplayName("응답의 releasedAt이 UTC 형식")
    void releasedAtIsUtc() {
      LocalDateTime kstReleasedAt = LocalDateTime.of(2026, 5, 9, 15, 0);
      createUpdateNote("v1.0.0", kstReleasedAt, true);

      ResponseEntity<ApiResponse<CursorPage<UpdateNoteResponse, UpdateNoteCursor>>> response =
          restTemplate.exchange("/update-notes", HttpMethod.GET, null, PAGE_RESPONSE);

      assert response.getBody() != null;
      UpdateNoteResponse first = response.getBody().data().getContent().getFirst();
      assertThat(first.releasedAt()).isEqualTo(TimeUtils.kstToUtc(kstReleasedAt));
    }

    @Test
    @DisplayName("cursor 한쪽만 - 400")
    void badRequestWhenCursorIncomplete() {
      ResponseEntity<ApiResponse<CursorPage<UpdateNoteResponse, UpdateNoteCursor>>> response =
          restTemplate.exchange("/update-notes?cursorId=1", HttpMethod.GET, null, PAGE_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("size 범위 위반 - 400")
    void badRequestWhenSizeOutOfRange() {
      ResponseEntity<ApiResponse<CursorPage<UpdateNoteResponse, UpdateNoteCursor>>> response =
          restTemplate.exchange("/update-notes?size=101", HttpMethod.GET, null, PAGE_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
  }
}
