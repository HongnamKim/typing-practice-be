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

import com.typingpractice.typing_practice_be.notice.update.dto.UpdateUpdateNoteRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;

public class AdminUpdateNoteE2ETest extends BaseE2ETest {
  private static final ParameterizedTypeReference<ApiResponse<UpdateNoteResponse>> DETAIL_RESPONSE =
      new ParameterizedTypeReference<>() {};
  private static final ParameterizedTypeReference<
          ApiResponse<CursorPage<UpdateNoteResponse, UpdateNoteCursor>>>
      PAGE_RESPONSE = new ParameterizedTypeReference<>() {};
  private static final ParameterizedTypeReference<ApiResponse<Void>> VOID_RESPONSE =
      new ParameterizedTypeReference<>() {};

  @Autowired private JdbcTemplate jdbcTemplate;

  @AfterEach
  void cleanUp() {
    jdbcTemplate.execute("DELETE FROM update_note");
  }

  private HttpHeaders getAdminHeaders() {
    String adminToken = getAccessToken(ADMIN_PROVIDER_ID);
    return createAuthHeader(adminToken);
  }

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
  @DisplayName("POST /admin/update-notes - 업데이트 노트 등록")
  class PostUpdateNote {
    @Test
    @DisplayName("성공 - KST 입력이 UTC로 변환되어 응답")
    void success() {
      HttpHeaders adminHeaders = getAdminHeaders();

      LocalDateTime kstReleasedAt = LocalDateTime.of(2026, 5, 9, 15, 0);
      CreateUpdateNoteRequest request =
          new CreateUpdateNoteRequest(
              "v1.0.0",
              kstReleasedAt,
              List.of(new LocalizedTextRequest("새 기능", "New feature", null)),
              List.of(new LocalizedTextRequest("개선", "Improvement", null)),
              true);

      ResponseEntity<ApiResponse<UpdateNoteResponse>> response =
          restTemplate.exchange(
              "/admin/update-notes",
              HttpMethod.POST,
              new HttpEntity<>(request, adminHeaders),
              DETAIL_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assert response.getBody() != null;

      UpdateNoteResponse data = response.getBody().data();
      assertThat(data.id()).isNotNull();
      assertThat(data.version()).isEqualTo("v1.0.0");
      assertThat(data.releasedAt()).isEqualTo(TimeUtils.kstToUtc(kstReleasedAt));
      assertThat(data.newFeatures()).hasSize(1);
      assertThat(data.improvements()).hasSize(1);
      assertThat(data.published()).isTrue();
    }

    @Test
    @DisplayName("version 충돌 - 409")
    void versionConflict() {
      createUpdateNote("v1.0.0", LocalDateTime.now(), true);

      HttpHeaders adminHeaders = getAdminHeaders();
      CreateUpdateNoteRequest request =
          new CreateUpdateNoteRequest(
              "v1.0.0",
              LocalDateTime.now(),
              List.of(new LocalizedTextRequest("새 기능", null, null)),
              List.of(new LocalizedTextRequest("개선", null, null)),
              true);

      ResponseEntity<ApiResponse<UpdateNoteResponse>> response =
          restTemplate.exchange(
              "/admin/update-notes",
              HttpMethod.POST,
              new HttpEntity<>(request, adminHeaders),
              DETAIL_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("version 형식 위반 - 400")
    void badRequestWhenVersionInvalid() {
      HttpHeaders adminHeaders = getAdminHeaders();

      CreateUpdateNoteRequest request =
          new CreateUpdateNoteRequest(
              "1.0.0", // v 누락
              LocalDateTime.now(),
              List.of(new LocalizedTextRequest("새 기능", null, null)),
              List.of(new LocalizedTextRequest("개선", null, null)),
              true);

      ResponseEntity<ApiResponse<UpdateNoteResponse>> response =
          restTemplate.exchange(
              "/admin/update-notes",
              HttpMethod.POST,
              new HttpEntity<>(request, adminHeaders),
              DETAIL_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("newFeatures 항목의 ko 빈 문자열 - 400")
    void badRequestWhenItemKoBlank() {
      HttpHeaders adminHeaders = getAdminHeaders();

      CreateUpdateNoteRequest request =
          new CreateUpdateNoteRequest(
              "v1.0.0",
              LocalDateTime.now(),
              List.of(new LocalizedTextRequest("", null, null)),
              List.of(),
              true);

      ResponseEntity<ApiResponse<UpdateNoteResponse>> response =
          restTemplate.exchange(
              "/admin/update-notes",
              HttpMethod.POST,
              new HttpEntity<>(request, adminHeaders),
              DETAIL_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("releasedAt null - 400")
    void badRequestWhenReleasedAtNull() {
      HttpHeaders adminHeaders = getAdminHeaders();
      CreateUpdateNoteRequest request =
          new CreateUpdateNoteRequest(
              "v1.0.0",
              null,
              List.of(new LocalizedTextRequest("새 기능", null, null)),
              List.of(new LocalizedTextRequest("개선", null, null)),
              true);

      ResponseEntity<ApiResponse<UpdateNoteResponse>> response =
          restTemplate.exchange(
              "/admin/update-notes",
              HttpMethod.POST,
              new HttpEntity<>(request, adminHeaders),
              DETAIL_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("관리자 아닌 유저 - 403")
    void forbiddenWhenNotAdmin() {
      String token = getAccessToken(USER_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);

      CreateUpdateNoteRequest request =
          new CreateUpdateNoteRequest(
              "v1.0.0",
              LocalDateTime.now(),
              List.of(new LocalizedTextRequest("새 기능", null, null)),
              List.of(new LocalizedTextRequest("개선", null, null)),
              true);

      ResponseEntity<ApiResponse<UpdateNoteResponse>> response =
          restTemplate.exchange(
              "/admin/update-notes",
              HttpMethod.POST,
              new HttpEntity<>(request, headers),
              DETAIL_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("토큰 없이 요청 - 401")
    void unauthorizedWithoutToken() {
      CreateUpdateNoteRequest request =
          new CreateUpdateNoteRequest(
              "v1.0.0",
              LocalDateTime.now(),
              List.of(new LocalizedTextRequest("새 기능", null, null)),
              List.of(new LocalizedTextRequest("개선", null, null)),
              true);

      ResponseEntity<ApiResponse<UpdateNoteResponse>> response =
          restTemplate.exchange(
              "/admin/update-notes", HttpMethod.POST, new HttpEntity<>(request), DETAIL_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }

  @Nested
  @DisplayName("PATCH /admin/update-notes/{id} - 업데이트 노트 수정")
  class PatchUpdateNote {
    @Test
    @DisplayName("성공 - 모든 필드 변경, KST 입력이 UTC로 변환되어 응답")
    void successAllFields() {
      Long id = createUpdateNote("v1.0.0", LocalDateTime.of(2026, 1, 1, 0, 0), true);

      HttpHeaders adminHeaders = getAdminHeaders();
      LocalDateTime newKstReleasedAt = LocalDateTime.of(2026, 5, 12, 15, 0);
      UpdateUpdateNoteRequest request =
          new UpdateUpdateNoteRequest(
              "v1.0.1",
              newKstReleasedAt,
              List.of(new LocalizedTextRequest("바뀐 기능", null, null)),
              List.of(new LocalizedTextRequest("바뀐 개선", null, null)),
              false);

      ResponseEntity<ApiResponse<UpdateNoteResponse>> response =
          restTemplate.exchange(
              "/admin/update-notes/" + id,
              HttpMethod.PATCH,
              new HttpEntity<>(request, adminHeaders),
              DETAIL_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assert response.getBody() != null;

      UpdateNoteResponse data = response.getBody().data();
      assertThat(data.version()).isEqualTo("v1.0.1");
      assertThat(data.releasedAt()).isEqualTo(TimeUtils.kstToUtc(newKstReleasedAt));
      assertThat(data.newFeatures().getFirst().ko()).isEqualTo("바뀐 기능");
      assertThat(data.improvements().getFirst().ko()).isEqualTo("바뀐 개선");
      assertThat(data.published()).isFalse();
    }

    @Test
    @DisplayName("성공 - null 필드는 미변경")
    void successPartial() {
      Long id = createUpdateNote("v1.0.0", LocalDateTime.of(2026, 1, 1, 0, 0), true);

      HttpHeaders adminHeaders = getAdminHeaders();
      // published 만 변경
      UpdateUpdateNoteRequest request = new UpdateUpdateNoteRequest(null, null, null, null, false);

      ResponseEntity<ApiResponse<UpdateNoteResponse>> response =
          restTemplate.exchange(
              "/admin/update-notes/" + id,
              HttpMethod.PATCH,
              new HttpEntity<>(request, adminHeaders),
              DETAIL_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assert response.getBody() != null;

      UpdateNoteResponse data = response.getBody().data();
      assertThat(data.version()).isEqualTo("v1.0.0");
      assertThat(data.published()).isFalse();
      assertThat(data.newFeatures().getFirst().ko()).isEqualTo("새 기능");
    }

    @Test
    @DisplayName("version 충돌 - 409")
    void versionConflict() {
      createUpdateNote("v1.0.0", LocalDateTime.now(), true);
      Long otherId = createUpdateNote("v1.0.1", LocalDateTime.now().minusDays(1), true);

      HttpHeaders adminHeaders = getAdminHeaders();

      UpdateUpdateNoteRequest request =
          new UpdateUpdateNoteRequest("v1.0.0", null, null, null, null);

      ResponseEntity<ApiResponse<UpdateNoteResponse>> response =
          restTemplate.exchange(
              "/admin/update-notes/" + otherId,
              HttpMethod.PATCH,
              new HttpEntity<>(request, adminHeaders),
              DETAIL_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("동일한 version으로 수정 - 자기 자신 제외하므로 정상")
    void sameVersionAllowed() {
      Long id = createUpdateNote("v1.0.0", LocalDateTime.of(2026, 1, 1, 0, 0), true);

      HttpHeaders adminHeaders = getAdminHeaders();
      UpdateUpdateNoteRequest request =
          new UpdateUpdateNoteRequest("v1.0.0", null, null, null, false);

      ResponseEntity<ApiResponse<UpdateNoteResponse>> response =
          restTemplate.exchange(
              "/admin/update-notes/" + id,
              HttpMethod.PATCH,
              new HttpEntity<>(request, adminHeaders),
              DETAIL_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("version 형식 위반 - 400")
    void badRequestWhenVersionInvalid() {
      Long id = createUpdateNote("v1.0.0", LocalDateTime.now(), true);

      HttpHeaders adminHeaders = getAdminHeaders();
      UpdateUpdateNoteRequest request =
          new UpdateUpdateNoteRequest("invalid", null, null, null, null);

      ResponseEntity<ApiResponse<UpdateNoteResponse>> response =
          restTemplate.exchange(
              "/admin/update-notes/" + id,
              HttpMethod.PATCH,
              new HttpEntity<>(request, adminHeaders),
              DETAIL_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("미존재 - 404")
    void notFound() {
      HttpHeaders adminHeaders = getAdminHeaders();
      UpdateUpdateNoteRequest request =
          new UpdateUpdateNoteRequest(null, LocalDateTime.now(), null, null, null);

      ResponseEntity<ApiResponse<UpdateNoteResponse>> response =
          restTemplate.exchange(
              "/admin/update-notes/999999",
              HttpMethod.PATCH,
              new HttpEntity<>(request, adminHeaders),
              DETAIL_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("관리자 아닌 유저 - 403")
    void forbiddenWhenNotAdmin() {
      String token = getAccessToken(USER_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);

      UpdateUpdateNoteRequest request =
          new UpdateUpdateNoteRequest(null, LocalDateTime.now(), null, null, null);

      ResponseEntity<ApiResponse<UpdateNoteResponse>> response =
          restTemplate.exchange(
              "/admin/update-notes/1",
              HttpMethod.PATCH,
              new HttpEntity<>(request, headers),
              DETAIL_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("토큰 없이 요청 - 401")
    void unauthorizedWithoutToken() {
      UpdateUpdateNoteRequest request =
          new UpdateUpdateNoteRequest(null, LocalDateTime.now(), null, null, null);

      ResponseEntity<ApiResponse<UpdateNoteResponse>> response =
          restTemplate.exchange(
              "/admin/update-notes/1",
              HttpMethod.PATCH,
              new HttpEntity<>(request),
              DETAIL_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }

  @Nested
  @DisplayName("DELETE /admin/update-notes/{id} - 업데이트 노트 삭제")
  class DeleteUpdateNote {
    @Test
    @DisplayName("성공 - 삭제 후 조회 시 404")
    void success() {
      Long id = createUpdateNote("v1.0.0", LocalDateTime.now(), true);

      HttpHeaders adminHeaders = getAdminHeaders();

      ResponseEntity<ApiResponse<Void>> deleteResponse =
          restTemplate.exchange(
              "/admin/update-notes/" + id,
              HttpMethod.DELETE,
              new HttpEntity<>(adminHeaders),
              VOID_RESPONSE);

      assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

      // soft delete 확인 - 어드민 단건 조회 시 404
      ResponseEntity<ApiResponse<UpdateNoteResponse>> getResponse =
          restTemplate.exchange(
              "/admin/update-notes/" + id,
              HttpMethod.GET,
              new HttpEntity<>(adminHeaders),
              DETAIL_RESPONSE);

      assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("미존재 -404")
    void notFound() {
      HttpHeaders adminHeaders = getAdminHeaders();

      ResponseEntity<ApiResponse<Void>> response =
          restTemplate.exchange(
              "/admin/update-notes/999999999",
              HttpMethod.DELETE,
              new HttpEntity<>(adminHeaders),
              VOID_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("관리자 아닌 유저 - 403")
    void forbiddenWhenNotAdmin() {
      String token = getAccessToken(USER_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);

      ResponseEntity<ApiResponse<Void>> response =
          restTemplate.exchange(
              "/admin/update-notes/1", HttpMethod.DELETE, new HttpEntity<>(headers), VOID_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("토큰 없이 요청 - 401")
    void unauthorizedWithoutToken() {
      ResponseEntity<ApiResponse<Void>> response =
          restTemplate.exchange("/admin/update-notes/1", HttpMethod.DELETE, null, VOID_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }

  @Nested
  @DisplayName("GET /admin/update-notes - 어드민 목록 조회 (커서)")
  class GetUpdateNotes {
    @Test
    @DisplayName("성공 - 페이지네이션 정확성 검증")
    void successPagination() {
      // 5개 등록, releasedAt 순서대로 (KST 입력)
      LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
      Long id1 = createUpdateNote("v1.0.0", base, true);
      Long id2 = createUpdateNote("v1.0.1", base.plusDays(1), true);
      Long id3 = createUpdateNote("v1.0.2", base.plusDays(2), false); // 미게시
      Long id4 = createUpdateNote("v1.0.3", base.plusDays(3), true);
      Long id5 = createUpdateNote("v1.0.4", base.plusDays(4), true);

      HttpHeaders adminHeaders = getAdminHeaders();

      // 첫 페이지 (size=2) - 최신순이므로 id5, id4
      ResponseEntity<ApiResponse<CursorPage<UpdateNoteResponse, UpdateNoteCursor>>> page1 =
          restTemplate.exchange(
              "/admin/update-notes?size=2",
              HttpMethod.GET,
              new HttpEntity<>(adminHeaders),
              PAGE_RESPONSE);

      assertThat(page1.getStatusCode()).isEqualTo(HttpStatus.OK);
      assert page1.getBody() != null;
      CursorPage<UpdateNoteResponse, UpdateNoteCursor> data1 = page1.getBody().data();
      assertThat(data1.getContent()).hasSize(2);
      assertThat(data1.getContent().getFirst().id()).isEqualTo(id5);
      assertThat(data1.getContent().get(1).id()).isEqualTo(id4);
      assertThat(data1.isHasNext()).isTrue();
      assertThat(data1.getNextCursor()).isNotNull();
      assertThat(data1.getNextCursor().id()).isEqualTo(id4);

      // 두번째 페이지 - 미게시(id3)도 어드민에서는 노출
      UpdateNoteCursor cursor = data1.getNextCursor();
      ResponseEntity<ApiResponse<CursorPage<UpdateNoteResponse, UpdateNoteCursor>>> page2 =
          restTemplate.exchange(
              "/admin/update-notes?size=2&cursorReleasedAt="
                  + cursor.releasedAt()
                  + "&cursorId="
                  + cursor.id(),
              HttpMethod.GET,
              new HttpEntity<>(adminHeaders),
              PAGE_RESPONSE);

      assert page2.getBody() != null;
      CursorPage<UpdateNoteResponse, UpdateNoteCursor> data2 = page2.getBody().data();
      assertThat(data2.getContent()).hasSize(2);
      assertThat(data2.getContent().get(0).id()).isEqualTo(id3); // 미게시도 노출
      assertThat(data2.getContent().get(1).id()).isEqualTo(id2);
      assertThat(data2.isHasNext()).isTrue();

      // 세번째 페이지 - 마지막 1개
      cursor = data2.getNextCursor();
      ResponseEntity<ApiResponse<CursorPage<UpdateNoteResponse, UpdateNoteCursor>>> page3 =
          restTemplate.exchange(
              "/admin/update-notes?size=2&cursorReleasedAt="
                  + cursor.releasedAt()
                  + "&cursorId="
                  + cursor.id(),
              HttpMethod.GET,
              new HttpEntity<>(adminHeaders),
              PAGE_RESPONSE);

      assert page3.getBody() != null;
      CursorPage<UpdateNoteResponse, UpdateNoteCursor> data3 = page3.getBody().data();
      assertThat(data3.getContent()).hasSize(1);
      assertThat(data3.getContent().getFirst().id()).isEqualTo(id1);
      assertThat(data3.isHasNext()).isFalse();
      assertThat(data3.getNextCursor()).isNull();
    }

    @Test
    @DisplayName("응답의 releasedAt이 UTC 형식")
    void releasedAtIsUtc() {
      LocalDateTime kstReleasedAt = LocalDateTime.of(2026, 5, 9, 15, 0);
      createUpdateNote("v1.0.0", kstReleasedAt, true);

      HttpHeaders adminHeaders = getAdminHeaders();
      ResponseEntity<ApiResponse<CursorPage<UpdateNoteResponse, UpdateNoteCursor>>> response =
          restTemplate.exchange(
              "/admin/update-notes", HttpMethod.GET, new HttpEntity<>(adminHeaders), PAGE_RESPONSE);

      assert response.getBody() != null;
      UpdateNoteResponse first = response.getBody().data().getContent().getFirst();
      assertThat(first.releasedAt()).isEqualTo(TimeUtils.kstToUtc(kstReleasedAt));
    }

    @Test
    @DisplayName("cursor 한쪽만 - 400")
    void badRequestWhenCursorIncomplete() {
      HttpHeaders adminHeaders = getAdminHeaders();

      ResponseEntity<ApiResponse<CursorPage<UpdateNoteResponse, UpdateNoteCursor>>> response =
          restTemplate.exchange(
              "/admin/update-notes?cursorId=1",
              HttpMethod.GET,
              new HttpEntity<>(adminHeaders),
              PAGE_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("size 범위 위반 - 400")
    void badRequestWhenSizeOutOfRange() {
      HttpHeaders adminHeaders = getAdminHeaders();

      ResponseEntity<ApiResponse<CursorPage<UpdateNoteResponse, UpdateNoteCursor>>> response =
          restTemplate.exchange(
              "/admin/update-notes?size=101",
              HttpMethod.GET,
              new HttpEntity<>(adminHeaders),
              PAGE_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("관리자 아닌 유저 - 403")
    void forbiddenWhenNotAdmin() {
      String token = getAccessToken(USER_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);

      ResponseEntity<ApiResponse<CursorPage<UpdateNoteResponse, UpdateNoteCursor>>> response =
          restTemplate.exchange(
              "/admin/update-notes", HttpMethod.GET, new HttpEntity<>(headers), PAGE_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("토큰 없이 요청 - 401")
    void unauthorizedWithoutToken() {
      ResponseEntity<ApiResponse<CursorPage<UpdateNoteResponse, UpdateNoteCursor>>> response =
          restTemplate.exchange("/admin/update-notes", HttpMethod.GET, null, PAGE_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }

  @Nested
  @DisplayName("GET /admin/update-notes/{id} - 어드임 단건 조회")
  class GetById {
    @Test
    @DisplayName("성공 - 게시 노트")
    void successPublished() {
      LocalDateTime kstReleasedAt = LocalDateTime.of(2026, 5, 12, 15, 0);
      Long id = createUpdateNote("v1.0.0", kstReleasedAt, true);

      HttpHeaders adminHeaders = getAdminHeaders();
      ResponseEntity<ApiResponse<UpdateNoteResponse>> response =
          restTemplate.exchange(
              "/admin/update-notes/" + id,
              HttpMethod.GET,
              new HttpEntity<>(adminHeaders),
              DETAIL_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assert response.getBody() != null;
      UpdateNoteResponse data = response.getBody().data();
      assertThat(data.id()).isEqualTo(id);
      assertThat(data.version()).isEqualTo("v1.0.0");
      assertThat(data.releasedAt()).isEqualTo(TimeUtils.kstToUtc(kstReleasedAt));
      assertThat(data.published()).isTrue();
    }

    @Test
    @DisplayName("성공 - 미게시 노트도 조회 (어드민)")
    void successUnpublished() {
      Long id = createUpdateNote("v1.0.0", LocalDateTime.now(), false);

      HttpHeaders adminHeaders = getAdminHeaders();
      ResponseEntity<ApiResponse<UpdateNoteResponse>> response =
          restTemplate.exchange(
              "/admin/update-notes/" + id,
              HttpMethod.GET,
              new HttpEntity<>(adminHeaders),
              DETAIL_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assert response.getBody() != null;
      assertThat(response.getBody().data().published()).isFalse();
    }

    @Test
    @DisplayName("미존재 - 404")
    void notFound() {
      HttpHeaders adminHeaders = getAdminHeaders();

      ResponseEntity<ApiResponse<UpdateNoteResponse>> response =
          restTemplate.exchange(
              "/admin/update-notes/999999",
              HttpMethod.GET,
              new HttpEntity<>(adminHeaders),
              DETAIL_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("관리자 아닌 유저 - 403")
    void forbiddenWhenNotAdmin() {
      String token = getAccessToken(USER_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);

      ResponseEntity<ApiResponse<UpdateNoteResponse>> response =
          restTemplate.exchange(
              "/admin/update-notes/1", HttpMethod.GET, new HttpEntity<>(headers), DETAIL_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("토큰 없이 요청 - 401")
    void unauthorizedWithoutToken() {
      ResponseEntity<ApiResponse<UpdateNoteResponse>> response =
          restTemplate.exchange("/admin/update-notes/1", HttpMethod.GET, null, DETAIL_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }
}
