# Database Migrations

이 디렉토리는 Flyway 도입을 상정한 DB 마이그레이션 스크립트 보관소입니다.
**현재 Flyway는 적용되어 있지 않으며, 운영 DB에는 SQL을 직접 실행해 적용합니다.**

## 파일 명명 규칙

V{YYYYMMDD}_{seq}__{description}.sql

예: `V20260513_1__create_notice_tables.sql`

- 같은 날 여러 마이그레이션이 있으면 `_1`, `_2` 순서로 증가
- `description`은 스네이크 케이스, 영문
- 더블 언더스코어 `__` 필수 (Flyway 표준)

## 적용 절차

1. 운영 DB에 `psql` 등으로 접속
2. 트랜잭션 안에서 SQL 실행 후 commit
3. 적용 후 결과를 운영 일지에 기록

## Flyway 도입 시 계획

- 현재 운영 DB의 전체 스키마를 `pg_dump --schema-only` 로 추출해 baseline 스크립트로 저장
- `flyway baseline` 명령으로 baseline 표시 (실제 실행은 안 됨)
- 새 환경 셋업 시에는 baseline 부터 차례로 적용