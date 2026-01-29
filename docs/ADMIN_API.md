# Admin API 문서

모든 Admin API는 **ADMIN 권한**이 필요합니다. (🔒 인증 + 👑 관리자)

---

## Admin Member

### GET /admin/members

회원 목록 조회

**Query Parameters**
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| page | int | - | 1 | 페이지 번호 |
| size | int | - | 10 | 페이지 크기 (1-100) |
| role | string | - | - | USER, ADMIN, BANNED |

**Response**

```json
{
  "page": 1,
  "size": 10,
  "hasNext": true,
  "content": [
    {
      "id": 1,
      "email": "user@gmail.com",
      "nickname": "사용자",
      "role": "USER",
      "createdAt": "2026-01-29T12:00:00"
    }
  ]
}
```

### GET /admin/members/{memberId}

회원 상세 조회

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

### PATCH /admin/members/{memberId}/role

회원 권한 변경

**Request Body**

```json
{
  "role": "ADMIN"
  // USER, ADMIN, BANNED
}
```

**Response**: Member 객체

### POST /admin/members/{memberId}/ban

회원 차단

**Request Body**

```json
{
  "reason": "부적절한 활동"
  // 선택
}
```

**Response**: Member 객체 (role: BANNED)

### POST /admin/members/{memberId}/unban

회원 차단 해제

**Response**: Member 객체 (role: USER)

---

## Admin Quote

### GET /admin/quotes

문장 목록 조회

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
  "hasNext": true,
  "content": [
    {
      "quoteId": 1,
      "sentence": "문장입니다.",
      "author": "작성자",
      "type": "PUBLIC",
      "status": "PENDING",
      "reportCount": 0,
      "createdAt": "2026-01-29T12:00:00",
      "updatedAt": "2026-01-29T12:00:00"
    }
  ]
}
```

### POST /admin/quotes/{quoteId}/approve

공개 문장 승인

PENDING 상태의 PUBLIC 문장을 ACTIVE로 변경

**Response**: Quote 객체 (status: ACTIVE)

### POST /admin/quotes/{quoteId}/reject

공개 문장 거부

PENDING 상태의 PUBLIC 문장을 PRIVATE + ACTIVE로 변경

**Response**: Quote 객체 (type: PRIVATE, status: ACTIVE)

### PATCH /admin/quotes/{quoteId}

공개 문장 수정

PUBLIC 문장만 수정 가능

**Request Body**

```json
{
  "sentence": "수정된 문장",
  // 선택
  "author": "수정된 작성자"
  // 선택
}
```

**Response**: Quote 객체

### DELETE /admin/quotes/{quoteId}

문장 삭제

해당 문장의 모든 신고도 함께 처리됨 (quoteDeleted: true)

**Response**

```json
{
  "success": true,
  "data": null
}
```

### PATCH /admin/quotes/{quoteId}/hide

문장 숨김

**Response**: Quote 객체 (status: HIDDEN)

### POST /admin/quotes/{quoteId}/restore

숨김 해제

HIDDEN 상태의 문장을 ACTIVE로 변경

**Response**: Quote 객체 (status: ACTIVE)

---

## Admin Report

### GET /admin/reports

신고 목록 조회

**Query Parameters**
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| page | int | - | 1 | 페이지 번호 |
| size | int | - | 10 | 페이지 크기 (1-100) |
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
      "member": {
        "id": 2,
        "email": "reporter@gmail.com",
        "nickname": "신고자",
        "role": "USER",
        "createdAt": "2026-01-29T12:00:00"
      },
      "quote": {
        "quoteId": 1,
        "sentence": "문장입니다.",
        "author": "작성자",
        "type": "PUBLIC",
        "status": "ACTIVE",
        "reportCount": 1,
        "createdAt": "2026-01-29T12:00:00",
        "updatedAt": "2026-01-29T12:00:00"
      }
    }
  ]
}
```

### GET /admin/reports/{reportId}

신고 상세 조회

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
  "quote": {
    ...
  }
}
```

### POST /admin/reports/{quoteId}/process

신고 처리

- sentence와 author 중 하나라도 값이 있으면: 문장 수정 후 처리
- sentence와 author가 null이면: 문장 삭제 후 처리

해당 문장의 모든 신고가 함께 처리됨

**Request Body**

```json
{
  "sentence": "수정된 문장",
  // null이면 문장 삭제
  "author": "수정된 작성자"
  // null이면 문장 삭제
}
```

**Response**

```json
{
  "success": true,
  "data": null
}
```

### DELETE /admin/reports/{reportId}

신고 삭제

**Response**

```json
{
  "success": true,
  "data": null
}
```

---

## 에러 코드

| HTTP Status | 설명                |
|-------------|-------------------|
| 400         | 잘못된 요청            |
| 401         | 인증 필요             |
| 403         | 관리자 권한 필요         |
| 404         | 리소스를 찾을 수 없음      |
| 422         | 처리 불가 (상태 조건 불충족) |
