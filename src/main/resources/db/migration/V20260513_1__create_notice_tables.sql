-- ============================================================
-- 공지사항 / 업데이트 노트 테이블 생성
--
-- announcement: 사용자에게 노출되는 공지사항
-- update_note: 사용자에게 노출되는 업데이트 이력
-- ============================================================

-- ============================================================
-- announcement
-- ============================================================
CREATE TABLE announcement
(
    announcement_id BIGINT                   NOT NULL,
    posted_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    title           JSONB                    NOT NULL,
    content         JSONB                    NOT NULL,
    published       BOOLEAN                  NOT NULL,
    pinned          BOOLEAN                  NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE,
    updated_at      TIMESTAMP WITH TIME ZONE,
    deleted         BOOLEAN                  NOT NULL,
    deleted_at      TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (announcement_id)
);

CREATE SEQUENCE announcement_seq START WITH 1 INCREMENT BY 50;

-- ============================================================
-- update_note
-- ============================================================
CREATE TABLE update_note
(
    update_note_id BIGINT                   NOT NULL,
    version        VARCHAR(32)              NOT NULL,
    released_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    new_features   JSONB                    NOT NULL,
    improvements   JSONB                    NOT NULL,
    published      BOOLEAN                  NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE,
    updated_at     TIMESTAMP WITH TIME ZONE,
    deleted        BOOLEAN                  NOT NULL,
    deleted_at     TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (update_note_id),
    CONSTRAINT uq_update_note_version UNIQUE (version)
);

CREATE SEQUENCE update_note_seq START WITH 1 INCREMENT BY 50;