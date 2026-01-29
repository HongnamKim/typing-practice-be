# API 문서

Base URL: `http://localhost:8080` (로컬) / 배포 시 변경

Swagger UI: `/swagger-ui/index.html`

## 공통 응답 형식

```json
{
  "success": true,
  "data": { ... },
  "timestamp": "2026-01-29T12:00:00"
}
```

## 인증

인증이 필요한 엔드포인트는 `Authorization` 헤더에 Bearer 토큰을 포함해야 합니다.

```
Authorization: Bearer {accessToken}
```

---

## Auth

### POST /auth/test

테스트 로그인 (개발용)

**Request Body**

```json
{
  "providerId": "test123"
}
```

**Response**

```json
{
  "id": 1,
  "email": "email",
  "nickname": "user_abc12345",
  "role": "USER",
  "createdAt": "2026-01-29T12:00:00",
  "isNewMember": true,
  "accessToken": "eyJ...",
  "refreshToken": "abc123..."
}
```

### POST /auth/google

Google OAuth 로그인

**Request Body**

```json
{
  "code": "google_authorization_code"
}
```

**Response**

```json
{
  "id": 1,
  "email": "user@gmail.com",
  "nickname": "사용자",
  "role": "USER",
  "createdAt": "2026-01-29T12:00:00",
  "isNewMember": false,
  "accessToken": "eyJ...",
  "refreshToken": "abc123..."
}
```

### POST /auth/logout

로그아웃 (🔒 인증 필요)

**Response**

```json
{
  "success": true,
  "data": null
}
```

### POST /auth/refresh

토큰 갱신

**Request Body**

```json
{
  "refreshToken": "abc123..."
}
```

**Response**

```json
{
  "accessToken": "eyJ...",
  "refreshToken": "newToken..."
}
```

---

## Member

### GET /members/me

내 정보 조회 (🔒 인증 필요)

**Response**

```json
{
  "id": 1,
  "email": "user@gmail.com",
  "nickname": "사용자",
  "role": "USER",
  "createdAt": "2026-01-29T12:00:00"
}
```

### GET /members/check-nickname

닉네임 중복 확인

**Query Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| nickname | string | ✅ | 확인할 닉네임 (2-10자) |

**Response**

```json
{
  "success": true,
  "data": true  // true: 중복, false: 사용 가능
}
```

### PATCH /members/me

닉네임 수정 (🔒 인증 필요)

**Request Body**

```json
{
  "nickname": "새닉네임"
}
```

**Response**

```json
{
  "id": 1,
  "email": "user@gmail.com",
  "nickname": "새닉네임",
  "role": "USER",
  "createdAt": "2026-01-29T12:00:00"
}
```

### DELETE /members/me

회원 탈퇴 (🔒 인증 필요)

**Response**: 204 No Content

---

## Quote

### GET /quotes

공개 문장 랜덤 조회 (🔒 인증 필요)

**Query Parameters**
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| page | int | - | 1 | 페이지 번호 (1 이상) |
| count | int | - | 100 | 문장 개수 (100-300) |
| seed | float | ✅ | - | 랜덤 시드 (-1.0 ~ 1.0) |
| onlyMyQuotes | boolean | - | false | 내 문장만 포함 |

**Response**

```json
{
  "page": 1,
  "size": 100,
  "hasNext": true,
  "content": [
    {
      "quoteId": 1,
      "sentence": "타자 연습 문장입니다.",
      "author": "작자 미상",
      "type": "PUBLIC",
      "status": "ACTIVE",
      "reportCount": 0,
      "createdAt": "2026-01-29T12:00:00",
      "updatedAt": "2026-01-29T12:00:00"
    }
  ]
}
```

### GET /quotes/my

내 문장 목록 조회 (🔒 인증 필요)

**Query Parameters**
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| page | int | - | 1 | 페이지 번호 |
| size | int | - | 10 | 페이지 크기 (1-100) |
| status | string | - | - | PENDING, ACTIVE, HIDDEN |
| type | string | - | - | PUBLIC, PRIVATE |

**Response**

```json
{
  "page": 1,
  "size": 10,
  "hasNext": false,
  "content": [...]
}
```

### GET /quotes/{quoteId}

문장 상세 조회 (🔒 인증 필요)

**Response**

```json
{
  "quoteId": 1,
  "sentence": "타자 연습 문장입니다.",
  "author": "작자 미상",
  "type": "PUBLIC",
  "status": "ACTIVE",
  "reportCount": 0,
  "createdAt": "2026-01-29T12:00:00",
  "updatedAt": "2026-01-29T12:00:00"
}
```

### POST /quotes/public

공개 문장 업로드 (🔒 인증 필요)

승인 대기 상태(PENDING)로 생성됩니다.

**Request Body**

```json
{
  "sentence": "새로운 문장입니다.",  // 5-100자
  "author": "작성자"                 // 1-20자, 선택
}
```

**Response**

```json
{
  "quoteId": 101,
  "sentence": "새로운 문장입니다.",
  "author": "작성자",
  "type": "PUBLIC",
  "status": "PENDING",
  "reportCount": 0,
  "createdAt": "2026-01-29T12:00:00",
  "updatedAt": "2026-01-29T12:00:00"
}
```

### POST /quotes/private

비공개 문장 업로드 (🔒 인증 필요)

**Request Body**

```json
{
  "sentence": "개인 연습용 문장입니다.",
  "author": "나"
}
```

**Response**: Quote 객체

### PATCH /quotes/{quoteId}

비공개 문장 수정 (🔒 인증 필요)

PRIVATE + ACTIVE 상태의 본인 문장만 수정 가능

**Request Body**

```json
{
  "sentence": "수정된 문장",  // 선택
  "author": "수정된 작성자"  // 선택
}
```

**Response**: Quote 객체

### DELETE /quotes/{quoteId}

비공개 문장 삭제 (🔒 인증 필요)

PRIVATE + ACTIVE 상태의 본인 문장만 삭제 가능

**Response**

```json
{
  "success": true,
  "data": null
}
```

### POST /quotes/{quoteId}/publish

비공개 → 공개 전환 요청 (🔒 인증 필요)

PRIVATE + ACTIVE 상태의 본인 문장을 PUBLIC + PENDING으로 변경

**Response**: Quote 객체

### POST /quotes/{quoteId}/cancel-publish

공개 전환 취소 (🔒 인증 필요)

PUBLIC + PENDING 상태의 본인 문장을 PRIVATE + ACTIVE로 변경

**Response**: Quote 객체

---

## Report

### POST /reports

문장 신고 (🔒 인증 필요)

**Request Body**

```json
{
  "quoteId": 1,
  "reason": "MODIFY",        // MODIFY 또는 DELETE
  "detail": "오타가 있습니다"  // 1-200자
}
```

**Response**

```json
{
  "id": 1,
  "reason": "MODIFY",
  "status": "PENDING",
  "quoteDeleted": false,
  "detail": "오타가 있습니다",
  "createdAt": "2026-01-29T12:00:00",
  "updatedAt": "2026-01-29T12:00:00",
  "quote": { ... }
}
```

### GET /reports/my

내 신고 목록 조회 (🔒 인증 필요)

**Query Parameters**
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| page | int | - | 1 | 페이지 번호 |
| size | int | - | 10 | 페이지 크기 |
| status | string | - | - | PENDING, PROCESSED |

**Response**

```json
{
  "page": 1,
  "size": 10,
  "hasNext": false,
  "content": [
    {
      "id": 1,
      "reason": "MODIFY",
      "status": "PENDING",
      "quoteDeleted": false,
      "detail": "오타가 있습니다",
      "createdAt": "2026-01-29T12:00:00",
      "updatedAt": "2026-01-29T12:00:00",
      "quote": { ... }
    }
  ]
}
```

### DELETE /reports/{reportId}

내 신고 삭제 (🔒 인증 필요)

본인이 생성한 신고만 삭제 가능

**Response**

```json
{
  "success": true,
  "data": null
}
```

---

## 에러 응답

### 공통 에러 코드

| HTTP Status | 설명                     |
|-------------|------------------------|
| 400         | 잘못된 요청 (Validation 실패) |
| 401         | 인증 필요 / 토큰 만료          |
| 403         | 권한 없음 (BANNED 유저 등)    |
| 404         | 리소스를 찾을 수 없음           |
| 409         | 충돌 (중복 닉네임, 중복 신고 등)   |

### 에러 응답 형식

```json
{
  "success": false,
  "data": null,
  "timestamp": "2026-01-29T12:00:00"
}
```

---

## Enum 값

### MemberRole

- `USER`: 일반 유저
- `ADMIN`: 관리자
- `BANNED`: 차단된 유저

### QuoteType

- `PUBLIC`: 공개 문장
- `PRIVATE`: 비공개 문장

### QuoteStatus

- `PENDING`: 승인 대기
- `ACTIVE`: 활성
- `HIDDEN`: 숨김 (신고 누적)

### ReportReason

- `MODIFY`: 수정 요청
- `DELETE`: 삭제 요청

### ReportStatus

- `PENDING`: 처리 대기
- `PROCESSED`: 처리 완료
