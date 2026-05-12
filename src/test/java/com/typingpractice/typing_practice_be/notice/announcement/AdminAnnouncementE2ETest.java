package com.typingpractice.typing_practice_be.notice.announcement;

import static org.assertj.core.api.Assertions.*;

import com.typingpractice.typing_practice_be.BaseE2ETest;
import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.common.dto.CursorPage;
import com.typingpractice.typing_practice_be.common.utils.TimeUtils;
import com.typingpractice.typing_practice_be.notice.announcement.dto.*;
import java.time.LocalDateTime;
import java.util.List;

import com.typingpractice.typing_practice_be.notice.dto.LocalizedTextRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;

public class AdminAnnouncementE2ETest extends BaseE2ETest {

  private static final ParameterizedTypeReference<ApiResponse<AnnouncementDetail>> DETAIL_RESPONSE =
      new ParameterizedTypeReference<>() {};
  private static final ParameterizedTypeReference<ApiResponse<AnnouncementListResponse>>
      LIST_RESPONSE = new ParameterizedTypeReference<>() {};
  private static final ParameterizedTypeReference<
          ApiResponse<CursorPage<AnnouncementSummary, AnnouncementCursor>>>
      PAGE_RESPONSE = new ParameterizedTypeReference<>() {};
  private static final ParameterizedTypeReference<ApiResponse<Void>> VOID_RESPONSE =
      new ParameterizedTypeReference<>() {};

  private HttpHeaders getAdminHeaders() {
    String adminToken = getAccessToken(ADMIN_PROVIDER_ID);
    return createAuthHeader(adminToken);
  }

  private HttpHeaders getUserHeaders() {
    String userToken = getAccessToken(USER_PROVIDER_ID);
    return createAuthHeader(userToken);
  }

  /** 어드민으로 공지사항 등록 후 id 반환. */
  private Long createAnnouncement(
      LocalDateTime postedAt, String titleKo, boolean published, boolean pinned) {
    HttpHeaders adminHeaders = getAdminHeaders();

    CreateAnnouncementRequest request =
        new CreateAnnouncementRequest(
            postedAt,
            new LocalizedTextRequest(titleKo, null, null),
            new LocalizedTextRequest("내용", null, null),
            published,
            pinned);

    ResponseEntity<ApiResponse<AnnouncementDetail>> response =
        restTemplate.exchange(
            "/admin/announcements",
            HttpMethod.POST,
            new HttpEntity<>(request, adminHeaders),
            DETAIL_RESPONSE);

    assert response.getBody() != null;
    return response.getBody().data().id();
  }

  @Autowired private JdbcTemplate jdbcTemplate;

  @AfterEach
  void cleanUp() {
    jdbcTemplate.execute("DELETE FROM announcement");
  }

  @Nested
  @DisplayName("POST /admin/announcements - 공지사항 등록")
  class PostAnnouncement {
    @Test
    @DisplayName("성공")
    void success() {
      HttpHeaders adminHeaders = getAdminHeaders();

      LocalDateTime kstPostedAt = LocalDateTime.of(2026, 5, 9, 15, 0);
      CreateAnnouncementRequest request =
          new CreateAnnouncementRequest(
              kstPostedAt,
              new LocalizedTextRequest("저작권 안내", "Copyright Notice", null),
              new LocalizedTextRequest("내용", "content", null),
              true,
              false);

      ResponseEntity<ApiResponse<AnnouncementDetail>> response =
          restTemplate.exchange(
              "/admin/announcements",
              HttpMethod.POST,
              new HttpEntity<>(request, adminHeaders),
              DETAIL_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assert response.getBody() != null;
      AnnouncementDetail data = response.getBody().data();
      assertThat(response.getBody().success()).isTrue();
      assertThat(data.id()).isNotNull();
      assertThat(data.postedAt()).isEqualTo(TimeUtils.kstToUtc(kstPostedAt));
    }

    @Test()
    @DisplayName("title.ko 빈 문자열 - 400")
    void badRequestWhenTitleKoBlank() {
      HttpHeaders adminHeaders = getAdminHeaders();

      CreateAnnouncementRequest request =
          new CreateAnnouncementRequest(
              LocalDateTime.now(),
              new LocalizedTextRequest("", null, null),
              new LocalizedTextRequest("내용", null, null),
              true,
              false);

      ResponseEntity<ApiResponse<AnnouncementDetail>> response =
          restTemplate.exchange(
              "/admin/announcements",
              HttpMethod.POST,
              new HttpEntity<>(request, adminHeaders),
              DETAIL_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("postedAt null - 400")
    void badRequestWhenPostedAtNull() {
      HttpHeaders adminHeaders = getAdminHeaders();

      CreateAnnouncementRequest request =
          new CreateAnnouncementRequest(
              null,
              new LocalizedTextRequest("제목", null, null),
              new LocalizedTextRequest("내용", null, null),
              true,
              false);

      ResponseEntity<ApiResponse<AnnouncementDetail>> response =
          restTemplate.exchange(
              "/admin/announcements",
              HttpMethod.POST,
              new HttpEntity<>(request, adminHeaders),
              DETAIL_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("관리자 아닌 유저 - 403")
    void forbiddenWhenNotAdmin() {
      HttpHeaders headers = getUserHeaders();

      CreateAnnouncementRequest request =
          new CreateAnnouncementRequest(
              LocalDateTime.now(),
              new LocalizedTextRequest("제목", null, null),
              new LocalizedTextRequest("내용", null, null),
              true,
              false);

      ResponseEntity<ApiResponse<AnnouncementDetail>> response =
          restTemplate.exchange(
              "/admin/announcements",
              HttpMethod.POST,
              new HttpEntity<>(request, headers),
              DETAIL_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("토큰 없이 요청 - 401")
    void unauthorizedWithoutToken() {
      CreateAnnouncementRequest request =
          new CreateAnnouncementRequest(
              LocalDateTime.now(),
              new LocalizedTextRequest("제목", null, null),
              new LocalizedTextRequest("내용", null, null),
              true,
              false);

      ResponseEntity<ApiResponse<AnnouncementDetail>> response =
          restTemplate.exchange(
              "/admin/announcements", HttpMethod.POST, new HttpEntity<>(request), DETAIL_RESPONSE);

      assertThat(
              restTemplate
                  .exchange(
                      "/admin/announcements",
                      HttpMethod.POST,
                      new HttpEntity<>(request),
                      DETAIL_RESPONSE)
                  .getStatusCode())
          .isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }

  @Nested
  @DisplayName("PATCH /admin/announcements/{id} - 공지사항 수정")
  class PatchAnnouncement {
    @Test
    @DisplayName("성공 - 모든 필드 변경")
    void successAllFields() {
      LocalDateTime prevTime = LocalDateTime.now();
      Long id = createAnnouncement(prevTime, "원래 제목", true, false);

      HttpHeaders adminHeaders = getAdminHeaders();

      LocalDateTime newPostedAt = prevTime.plusDays(1);
      UpdateAnnouncementRequest request =
          new UpdateAnnouncementRequest(
              newPostedAt,
              new LocalizedTextRequest("바뀐 제목", null, null),
              new LocalizedTextRequest("바뀐 내용", null, null),
              false,
              true);

      ResponseEntity<ApiResponse<Void>> response =
          restTemplate.exchange(
              "/admin/announcements/" + id,
              HttpMethod.PATCH,
              new HttpEntity<>(request, adminHeaders),
              VOID_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

      // 변경 확인
      ResponseEntity<ApiResponse<AnnouncementDetail>> getResponse =
          restTemplate.exchange(
              "/admin/announcements/" + id,
              HttpMethod.GET,
              new HttpEntity<>(adminHeaders),
              DETAIL_RESPONSE);

      assert getResponse.getBody() != null;
      AnnouncementDetail detail = getResponse.getBody().data();
      assertThat(detail.postedAt()).isEqualTo(TimeUtils.kstToUtc(newPostedAt));
      assertThat(detail.title().ko()).isEqualTo("바뀐 제목");
      assertThat(detail.content().ko()).isEqualTo("바뀐 내용");
      assertThat(detail.published()).isFalse();
      assertThat(detail.pinned()).isTrue();
    }

    @Test
    @DisplayName("성공 - null 필드는 미변경")
    void successPartial() {
      Long id = createAnnouncement(LocalDateTime.now(), "원래 제목", true, false);

      HttpHeaders adminHeaders = getAdminHeaders();

      UpdateAnnouncementRequest request =
          new UpdateAnnouncementRequest(null, null, null, null, true);

      ResponseEntity<ApiResponse<AnnouncementDetail>> response =
          restTemplate.exchange(
              "/admin/announcements/" + id,
              HttpMethod.PATCH,
              new HttpEntity<>(request, adminHeaders),
              DETAIL_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assert response.getBody() != null;
      AnnouncementDetail detail = response.getBody().data();
      assertThat(detail.title().ko()).isEqualTo("원래 제목"); // 미변경
      assertThat(detail.published()).isTrue(); // 미변경
      assertThat(detail.pinned()).isTrue(); // 변경
    }

    @Test
    @DisplayName("미존재 - 404")
    void notFound() {
      HttpHeaders adminHeaders = getAdminHeaders();

      UpdateAnnouncementRequest request =
          new UpdateAnnouncementRequest(LocalDateTime.now(), null, null, null, null);

      ResponseEntity<ApiResponse<Void>> response =
          restTemplate.exchange(
              "/admin/announcements/99999999",
              HttpMethod.PATCH,
              new HttpEntity<>(request, adminHeaders),
              VOID_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("관리자 아닌 유저 - 403")
    void forbiddenWhenNotAdmin() {
      String token = getAccessToken(USER_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);

      UpdateAnnouncementRequest request =
          new UpdateAnnouncementRequest(LocalDateTime.now(), null, null, null, null);

      ResponseEntity<ApiResponse<Void>> response =
          restTemplate.exchange(
              "/admin/announcements/1",
              HttpMethod.PATCH,
              new HttpEntity<>(request, headers),
              VOID_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("토큰 없이 요청 - 401")
    void unauthorizedWithoutToken() {
      UpdateAnnouncementRequest request =
          new UpdateAnnouncementRequest(LocalDateTime.now(), null, null, null, null);

      ResponseEntity<ApiResponse<Void>> response =
          restTemplate.exchange(
              "/admin/announcements/1", HttpMethod.PATCH, new HttpEntity<>(request), VOID_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }

  @Nested
  @DisplayName("DELETE /admin/announcements/{id} - 공지사항 삭제")
  class DeleteAnnouncement {
    @Test
    @DisplayName("성공 - 삭제 후 조회 시 404")
    void success() {
      Long id = createAnnouncement(LocalDateTime.now(), "삭제 대상", true, false);

      HttpHeaders adminHeaders = getAdminHeaders();

      ResponseEntity<ApiResponse<Void>> deleteResponse =
          restTemplate.exchange(
              "/admin/announcements/" + id,
              HttpMethod.DELETE,
              new HttpEntity<>(adminHeaders),
              VOID_RESPONSE);

      assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

      ResponseEntity<ApiResponse<AnnouncementDetail>> getResponse =
          restTemplate.exchange(
              "/admin/announcements/" + id,
              HttpMethod.GET,
              new HttpEntity<>(adminHeaders),
              DETAIL_RESPONSE);

      assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("미존재 - 404")
    void notFound() {
      HttpHeaders adminHeaders = getAdminHeaders();

      ResponseEntity<ApiResponse<Void>> deleteResponse =
          restTemplate.exchange(
              "/admin/announcements/999999",
              HttpMethod.DELETE,
              new HttpEntity<>(adminHeaders),
              VOID_RESPONSE);

      assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("관리자 아닌 유저 - 403")
    void forbiddenWhenNotAdmin() {
      HttpHeaders userHeaders = getUserHeaders();

      ResponseEntity<ApiResponse<Void>> response =
          restTemplate.exchange(
              "/admin/announcements/1",
              HttpMethod.DELETE,
              new HttpEntity<>(userHeaders),
              VOID_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("토큰 없이 요청 - 401")
    void unauthorizedWithoutToken() {
      ResponseEntity<ApiResponse<Void>> response =
          restTemplate.exchange("/admin/announcements/1", HttpMethod.DELETE, null, VOID_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }

  @Nested
  @DisplayName("GET /admin/announcements - 어드민 목록 조회 (커서)")
  class GetAnnouncements {
    @Test
    @DisplayName("성공 - 페이지네이션 정확성 검증")
    void successPagination() {
      // 5개 등록 (모두 비고정), postedAt 시간 순서
      LocalDateTime base = LocalDateTime.now().minusHours(10);
      Long id1 = createAnnouncement(base, "first", true, false);
      Long id2 = createAnnouncement(base.plusHours(1), "second", true, false);
      Long id3 = createAnnouncement(base.plusHours(2), "third", false, false);
      Long id4 = createAnnouncement(base.plusHours(3), "fourth", true, false);
      Long id5 = createAnnouncement(base.plusHours(4), "fifth", true, false);

      HttpHeaders adminHeaders = getAdminHeaders();

      ResponseEntity<ApiResponse<CursorPage<AnnouncementSummary, AnnouncementCursor>>> page1 =
          restTemplate.exchange(
              "/admin/announcements?size=2",
              HttpMethod.GET,
              new HttpEntity<>(adminHeaders),
              PAGE_RESPONSE);

      assertThat(page1.getStatusCode()).isEqualTo(HttpStatus.OK);
      assert page1.getBody() != null;
      CursorPage<AnnouncementSummary, AnnouncementCursor> data1 = page1.getBody().data();
      assertThat(data1.getContent()).hasSize(2);
      assertThat(data1.getContent().getFirst().id()).isEqualTo(id5);
      assertThat(data1.getContent().get(1).id()).isEqualTo(id4);
      assertThat(data1.isHasNext()).isTrue();
      assertThat(data1.getNextCursor()).isNotNull();
      assertThat(data1.getNextCursor().id()).isEqualTo(id4);

      // 두번째 페이지 - 미게시(id3)도 어드민에서는 노출되어야 함
      AnnouncementCursor cursor = data1.getNextCursor();
      ResponseEntity<ApiResponse<CursorPage<AnnouncementSummary, AnnouncementCursor>>> page2 =
          restTemplate.exchange(
              "/admin/announcements?size=2&cursorPostedAt="
                  + cursor.postedAt()
                  + "&cursorId="
                  + cursor.id(),
              HttpMethod.GET,
              new HttpEntity<>(adminHeaders),
              PAGE_RESPONSE);

      assert page2.getBody() != null;
      CursorPage<AnnouncementSummary, AnnouncementCursor> data2 = page2.getBody().data();
      assertThat(data2.getContent()).hasSize(2);
      assertThat(data2.getContent().getFirst().id()).isEqualTo(id3); // 미게시도 노출
      assertThat(data2.getContent().get(1).id()).isEqualTo(id2);
      assertThat(data2.isHasNext()).isTrue();

      // 세번째 페이지 - 마지막 1개, hasNext=false
      cursor = data2.getNextCursor();
      ResponseEntity<ApiResponse<CursorPage<AnnouncementSummary, AnnouncementCursor>>> page3 =
          restTemplate.exchange(
              "/admin/announcements?size=2&cursorPostedAt="
                  + cursor.postedAt()
                  + "&cursorId="
                  + cursor.id(),
              HttpMethod.GET,
              new HttpEntity<>(adminHeaders),
              PAGE_RESPONSE);

      assert page3.getBody() != null;
      CursorPage<AnnouncementSummary, AnnouncementCursor> data3 = page3.getBody().data();
      assertThat(data3.getContent()).hasSize(1);
      assertThat(data3.getContent().getFirst().id()).isEqualTo(id1);
      assertThat(data3.isHasNext()).isFalse();
      assertThat(data3.getNextCursor()).isNull();
    }

    @Test
    @DisplayName("성공 - pinned 제외 검증")
    void excludesPinned() {
      Long pinnedId = createAnnouncement(LocalDateTime.now(), "고정", true, true);
      Long normalId = createAnnouncement(LocalDateTime.now().minusHours(1), "일반", true, false);

      HttpHeaders adminHeaders = getAdminHeaders();

      ResponseEntity<ApiResponse<CursorPage<AnnouncementSummary, AnnouncementCursor>>> response =
          restTemplate.exchange(
              "/admin/announcements?size=20",
              HttpMethod.GET,
              new HttpEntity<>(adminHeaders),
              PAGE_RESPONSE);

      assert response.getBody() != null;
      List<AnnouncementSummary> content = response.getBody().data().getContent();
      assertThat(content).extracting(AnnouncementSummary::id).contains(normalId);
      assertThat(content).extracting(AnnouncementSummary::id).doesNotContain(pinnedId);
    }

    @Test
    @DisplayName("응답의 postedAt이 UTC 형식")
    void postedAtIsUtc() {
      LocalDateTime kstPostedAt = LocalDateTime.of(2026, 5, 9, 15, 0);
      createAnnouncement(kstPostedAt, "공지", true, false);

      HttpHeaders adminHeaders = getAdminHeaders();
      ResponseEntity<ApiResponse<CursorPage<AnnouncementSummary, AnnouncementCursor>>> response =
          restTemplate.exchange(
              "/admin/announcements",
              HttpMethod.GET,
              new HttpEntity<>(adminHeaders),
              PAGE_RESPONSE);

      assert response.getBody() != null;
      AnnouncementSummary first = response.getBody().data().getContent().getFirst();
      assertThat(first.postedAt()).isEqualTo(TimeUtils.kstToUtc(kstPostedAt));
    }

    @Test
    @DisplayName("cursor 한쪽만 - 400")
    void badRequestWhenCursorIncomplete() {
      HttpHeaders adminHeaders = getAdminHeaders();

      ResponseEntity<ApiResponse<CursorPage<AnnouncementSummary, AnnouncementCursor>>> response =
          restTemplate.exchange(
              "/admin/announcements?cursorId=1",
              HttpMethod.GET,
              new HttpEntity<>(adminHeaders),
              PAGE_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("size 범위 위반 - 400")
    void badRequestWhenSizeOutOfRange() {
      String adminToken = getAccessToken(ADMIN_PROVIDER_ID);
      HttpHeaders adminHeaders = createAuthHeader(adminToken);

      ResponseEntity<ApiResponse<CursorPage<AnnouncementSummary, AnnouncementCursor>>> response =
          restTemplate.exchange(
              "/admin/announcements?size=101",
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

      ResponseEntity<ApiResponse<CursorPage<AnnouncementSummary, AnnouncementCursor>>> response =
          restTemplate.exchange(
              "/admin/announcements", HttpMethod.GET, new HttpEntity<>(headers), PAGE_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("토큰 없이 요청 - 401")
    void unauthorizedWithoutToken() {
      ResponseEntity<ApiResponse<CursorPage<AnnouncementSummary, AnnouncementCursor>>> response =
          restTemplate.exchange("/admin/announcements", HttpMethod.GET, null, PAGE_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }

  @Nested
  @DisplayName("GET /admin/announcements/pinned - 어드민 고정 공지 조회")
  class GetPinned {
    @Test
    @DisplayName("성공 - 게시 여부 무관 고정만")
    void success() {
      Long publishedPinnedId = createAnnouncement(LocalDateTime.now(), "게시 고정", true, true);
      Long unpublishedPinnedId =
          createAnnouncement(LocalDateTime.now().minusHours(1), "미게시 고정", false, true);
      Long normalId = createAnnouncement(LocalDateTime.now().minusHours(2), "일반", true, false);

      String adminToken = getAccessToken(ADMIN_PROVIDER_ID);
      HttpHeaders adminHeaders = createAuthHeader(adminToken);

      ResponseEntity<ApiResponse<AnnouncementListResponse>> response =
          restTemplate.exchange(
              "/admin/announcements/pinned",
              HttpMethod.GET,
              new HttpEntity<>(adminHeaders),
              LIST_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assert response.getBody() != null;
      List<Long> ids =
          response.getBody().data().items().stream().map(AnnouncementSummary::id).toList();
      assertThat(ids).contains(publishedPinnedId, unpublishedPinnedId);
      assertThat(ids).doesNotContain(normalId);
    }

    @Test
    @DisplayName("관리자 아닌 유저 - 403")
    void forbiddenWhenNotAdmin() {
      String token = getAccessToken(USER_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);

      ResponseEntity<ApiResponse<AnnouncementListResponse>> response =
          restTemplate.exchange(
              "/admin/announcements/pinned",
              HttpMethod.GET,
              new HttpEntity<>(headers),
              LIST_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("토큰 없이 요청 - 401")
    void unauthorizedWithoutToken() {
      ResponseEntity<ApiResponse<AnnouncementListResponse>> response =
          restTemplate.exchange("/admin/announcements/pinned", HttpMethod.GET, null, LIST_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }

  @Nested
  @DisplayName("GET /admin/announcements/{id} - 어드민 단건 조회")
  class GetById {
    @Test
    @DisplayName("성공 - 게시 공지")
    void successPublished() {
      LocalDateTime kstPostedAt = LocalDateTime.of(2026, 5, 9, 15, 0);
      Long id = createAnnouncement(kstPostedAt, "게시", true, false);

      String adminToken = getAccessToken(ADMIN_PROVIDER_ID);
      HttpHeaders adminHeaders = createAuthHeader(adminToken);

      ResponseEntity<ApiResponse<AnnouncementDetail>> response =
          restTemplate.exchange(
              "/admin/announcements/" + id,
              HttpMethod.GET,
              new HttpEntity<>(adminHeaders),
              DETAIL_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assert response.getBody() != null;
      AnnouncementDetail detail = response.getBody().data();
      assertThat(detail.id()).isEqualTo(id);
      assertThat(detail.title().ko()).isEqualTo("게시");
      assertThat(detail.postedAt()).isEqualTo(TimeUtils.kstToUtc(kstPostedAt));
      assertThat(detail.published()).isTrue();
    }

    @Test
    @DisplayName("성공 - 미게시 공지도 조회 (어드민)")
    void successUnpublished() {
      Long id = createAnnouncement(LocalDateTime.now(), "미게시", false, false);
      System.out.println("id = " + id);

      String adminToken = getAccessToken(ADMIN_PROVIDER_ID);
      HttpHeaders adminHeaders = createAuthHeader(adminToken);

      ResponseEntity<ApiResponse<AnnouncementDetail>> response =
          restTemplate.exchange(
              "/admin/announcements/" + id,
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
      String adminToken = getAccessToken(ADMIN_PROVIDER_ID);
      HttpHeaders adminHeaders = createAuthHeader(adminToken);

      ResponseEntity<ApiResponse<AnnouncementDetail>> response =
          restTemplate.exchange(
              "/admin/announcements/999999",
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

      ResponseEntity<ApiResponse<AnnouncementDetail>> response =
          restTemplate.exchange(
              "/admin/announcements/1", HttpMethod.GET, new HttpEntity<>(headers), DETAIL_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("토큰 없이 요청 - 401")
    void unauthorizedWithoutToken() {
      ResponseEntity<ApiResponse<AnnouncementDetail>> response =
          restTemplate.exchange("/admin/announcements/1", HttpMethod.GET, null, DETAIL_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }
}
