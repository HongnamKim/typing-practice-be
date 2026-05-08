package com.typingpractice.typing_practice_be.notice.announcement;

import static org.assertj.core.api.Assertions.*;

import com.typingpractice.typing_practice_be.BaseE2ETest;
import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.common.dto.CursorPage;
import com.typingpractice.typing_practice_be.notice.announcement.dto.*;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;

public class AnnouncementE2ETest extends BaseE2ETest {
  private static final ParameterizedTypeReference<ApiResponse<AnnouncementIdResponse>> ID_RESPONSE =
      new ParameterizedTypeReference<>() {};
  private static final ParameterizedTypeReference<ApiResponse<AnnouncementDetail>> DETAIL_RESPONSE =
      new ParameterizedTypeReference<>() {};
  private static final ParameterizedTypeReference<ApiResponse<AnnouncementListResponse>>
      LIST_RESPONSE = new ParameterizedTypeReference<>() {};
  private static final ParameterizedTypeReference<
          ApiResponse<CursorPage<AnnouncementSummary, AnnouncementCursor>>>
      PAGE_RESPONSE = new ParameterizedTypeReference<>() {};

  @Autowired private JdbcTemplate jdbcTemplate;

  @AfterEach
  void cleanUp() {
    jdbcTemplate.execute("DELETE FROM announcement");
  }

  private HttpHeaders getAdminHeaders() {
    String adminToken = getAccessToken(ADMIN_PROVIDER_ID);
    return createAuthHeader(adminToken);
  }

  /** 어드민으로 공지사항 등록 후 id 반환. (사용자 측에는 등록 API가 없으므로 어드민 API로 데이터 셋업) */
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

    ResponseEntity<ApiResponse<AnnouncementIdResponse>> response =
        restTemplate.exchange(
            "/admin/announcements",
            HttpMethod.POST,
            new HttpEntity<>(request, adminHeaders),
            ID_RESPONSE);

    assert response.getBody() != null;
    return response.getBody().data().id();
  }

  @Nested
  @DisplayName("GET /announcements/latest - 최신 게시 공지 1건")
  class GetLatest {
    @Test
    @DisplayName("성공 - 최신 게시 공지 반환")
    void success() {
      LocalDateTime base = LocalDateTime.now().minusHours(5);
      createAnnouncement(base, "오래된", true, false);
      createAnnouncement(base.plusHours(1), "중간", true, false);
      Long latestId = createAnnouncement(base.plusHours(2), "최신", true, false);

      ResponseEntity<ApiResponse<AnnouncementDetail>> response =
          restTemplate.exchange("/announcements/latest", HttpMethod.GET, null, DETAIL_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assert response.getBody() != null;
      AnnouncementDetail detail = response.getBody().data();
      assertThat(detail).isNotNull();
      assertThat(detail.id()).isEqualTo(latestId);
      assertThat(detail.title().ko()).isEqualTo("최신");
    }

    @Test
    @DisplayName("성공 - 게시된 공지 없음 (data: null)")
    void emptyWhenNoPublished() {
      // 미게시 공지만 등록
      createAnnouncement(LocalDateTime.now(), "미게시", false, false);

      ResponseEntity<ApiResponse<AnnouncementDetail>> response =
          restTemplate.exchange("/announcements/latest", HttpMethod.GET, null, DETAIL_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assert response.getBody() != null;
      assertThat(response.getBody().success()).isTrue();
      assertThat(response.getBody().data()).isNull();
    }

    @Test
    @DisplayName("성공 - 미게시는 무시하고 그 이전 게시 공지 반환")
    void skipUnpublished() {
      LocalDateTime base = LocalDateTime.now().minusHours(5);
      Long publishedId = createAnnouncement(base, "이전 게시", true, false);
      createAnnouncement(base.plusHours(1), "최근 미게시", false, false);

      ResponseEntity<ApiResponse<AnnouncementDetail>> response =
          restTemplate.exchange("/announcements/latest", HttpMethod.GET, null, DETAIL_RESPONSE);

      assert response.getBody() != null;
      AnnouncementDetail detail = response.getBody().data();
      assertThat(detail).isNotNull();
      assertThat(detail.id()).isEqualTo(publishedId);
    }

    @Test
    @DisplayName("성공 - 비인증으로도 200")
    void successWithoutToken() {
      createAnnouncement(LocalDateTime.now(), "공지", true, false);

      ResponseEntity<ApiResponse<AnnouncementDetail>> response =
          restTemplate.exchange("/announcements/latest", HttpMethod.GET, null, DETAIL_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
  }

  @Nested
  @DisplayName("GET /announcements/pinned - 게시된 고정 공지")
  class GetPinned {
    @Test
    @DisplayName("성공 - 게시된 고정만 노출")
    void success() {
      Long publishedPinnedId = createAnnouncement(LocalDateTime.now(), "게시 고정", true, true);
      Long unpublishedPinnedId =
          createAnnouncement(LocalDateTime.now().minusHours(1), "미게시 고정", false, true);
      Long publishedNormalId =
          createAnnouncement(LocalDateTime.now().minusHours(2), "게시 일반", true, false);

      ResponseEntity<ApiResponse<AnnouncementListResponse>> response =
          restTemplate.exchange("/announcements/pinned", HttpMethod.GET, null, LIST_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assert response.getBody() != null;
      List<Long> ids =
          response.getBody().data().items().stream().map(AnnouncementSummary::id).toList();
      assertThat(ids).contains(publishedPinnedId);
      assertThat(ids).doesNotContain(unpublishedPinnedId);
      assertThat(ids).doesNotContain(publishedNormalId);
    }

    @Test
    @DisplayName("성공 - 고정 공지 없음 (빈 목록)")
    void empty() {
      // 게시 일반만 등록
      createAnnouncement(LocalDateTime.now(), "일반", true, false);

      ResponseEntity<ApiResponse<AnnouncementListResponse>> response =
          restTemplate.exchange("/announcements/pinned", HttpMethod.GET, null, LIST_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assert response.getBody() != null;
      assertThat(response.getBody().data().items()).isEmpty();
    }
  }

  @Nested
  @DisplayName("GET /announcements - 사용자 목록 조회 (커서)")
  class GetAnnouncements {
    @Test
    @DisplayName("성공 - 페이지네이션 정확성 + 미게시/고정 제외 검증")
    void successPagination() {
      // 6개 등록: 게시 일반 4개, 미게시 1개, 게시 고정 1개
      LocalDateTime base = LocalDateTime.now().minusHours(10);
      Long id1 = createAnnouncement(base, "첫번째 게시", true, false);
      Long id2 = createAnnouncement(base.plusHours(1), "두번째 게시", true, false);
      createAnnouncement(base.plusHours(2), "세번째 미게시", false, false); // 제외
      Long id4 = createAnnouncement(base.plusHours(3), "네번째 게시", true, false);
      createAnnouncement(base.plusHours(4), "다섯번째 고정", true, true); // 제외
      Long id6 = createAnnouncement(base.plusHours(5), "여섯번째 게시", true, false);

      // 첫 페이지 (size=2) - 최신순이므로 id6, id4 (미게시/고정 건너뛰고)
      ResponseEntity<ApiResponse<CursorPage<AnnouncementSummary, AnnouncementCursor>>> page1 =
          restTemplate.exchange("/announcements?size=2", HttpMethod.GET, null, PAGE_RESPONSE);

      assertThat(page1.getStatusCode()).isEqualTo(HttpStatus.OK);
      assert page1.getBody() != null;
      CursorPage<AnnouncementSummary, AnnouncementCursor> data1 = page1.getBody().data();
      assertThat(data1.getContent()).hasSize(2);
      assertThat(data1.getContent().get(0).id()).isEqualTo(id6);
      assertThat(data1.getContent().get(1).id()).isEqualTo(id4);
      assertThat(data1.isHasNext()).isTrue();
      assertThat(data1.getNextCursor()).isNotNull();
      assertThat(data1.getNextCursor().id()).isEqualTo(id4);

      // 두번째 페이지 - 미게시(세번째)와 고정(다섯번째)은 건너뛰고 id2, id1
      AnnouncementCursor cursor = data1.getNextCursor();
      ResponseEntity<ApiResponse<CursorPage<AnnouncementSummary, AnnouncementCursor>>> page2 =
          restTemplate.exchange(
              "/announcements?size=2&cursorPostedAt="
                  + cursor.postedAt()
                  + "&cursorId="
                  + cursor.id(),
              HttpMethod.GET,
              null,
              PAGE_RESPONSE);

      assert page2.getBody() != null;
      CursorPage<AnnouncementSummary, AnnouncementCursor> data2 = page2.getBody().data();
      assertThat(data2.getContent()).hasSize(2);
      assertThat(data2.getContent().get(0).id()).isEqualTo(id2);
      assertThat(data2.getContent().get(1).id()).isEqualTo(id1);
      assertThat(data2.isHasNext()).isFalse();
      assertThat(data2.getNextCursor()).isNull();
    }

    @Test
    @DisplayName("cursor 한쪽만 - 400")
    void badRequestWhenCursorIncomplete() {
      ResponseEntity<ApiResponse<CursorPage<AnnouncementSummary, AnnouncementCursor>>> response =
          restTemplate.exchange("/announcements?cursorId=1", HttpMethod.GET, null, PAGE_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("size 범위 위반 - 400")
    void badRequestWhenSizeOutOfRange() {
      ResponseEntity<ApiResponse<CursorPage<AnnouncementSummary, AnnouncementCursor>>> response =
          restTemplate.exchange("/announcements?size=101", HttpMethod.GET, null, PAGE_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
  }

  @Nested
  @DisplayName("GET /announcements/{id} - 사용자 단건 조회")
  class GetById {
    @Test
    @DisplayName("성공 - 게시 공지")
    void successPublished() {
      Long id = createAnnouncement(LocalDateTime.now(), "게시", true, false);

      ResponseEntity<ApiResponse<AnnouncementDetail>> response =
          restTemplate.exchange("/announcements/" + id, HttpMethod.GET, null, DETAIL_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assert response.getBody() != null;
      AnnouncementDetail detail = response.getBody().data();
      assertThat(detail.id()).isEqualTo(id);
      assertThat(detail.title().ko()).isEqualTo("게시");
      assertThat(detail.content().ko()).isEqualTo("내용");
      assertThat(detail.published()).isTrue();
    }

    @Test
    @DisplayName("미게시 공지 - 404 (어드민과 정책 차이)")
    void notFoundWhenUnpublished() {
      Long id = createAnnouncement(LocalDateTime.now(), "미게시", false, false);

      ResponseEntity<ApiResponse<AnnouncementDetail>> response =
          restTemplate.exchange("/announcements/" + id, HttpMethod.GET, null, DETAIL_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("미존재 - 404")
    void notFound() {
      ResponseEntity<ApiResponse<AnnouncementDetail>> response =
          restTemplate.exchange("/announcements/999999", HttpMethod.GET, null, DETAIL_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("게시된 고정 공지도 단건 조회 가능")
    void successPinned() {
      Long id = createAnnouncement(LocalDateTime.now(), "게시 고정", true, true);

      ResponseEntity<ApiResponse<AnnouncementDetail>> response =
          restTemplate.exchange("/announcements/" + id, HttpMethod.GET, null, DETAIL_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assert response.getBody() != null;
      assertThat(response.getBody().data().pinned()).isTrue();
    }
  }
}
