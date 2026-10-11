-- ============================================================
-- 난이도 표준화 통계 컬럼 추가 (설계서: 적응형 서빙 개선/3. 난이도 분포 표준화 설계서)
--
-- seed_mean / seed_std : 정적 seed 가중합(rawSeed)의 코퍼스 평균·표준편차
-- perf_mean / perf_std : 동적 보정 신호(cpmDrop + accDrop)의 코퍼스 평균·표준편차
-- NULL 이면 애플리케이션이 기본값을 사용한다 (표준화 전 산식과 동일 동작)
-- ============================================================
ALTER TABLE global_quote_statistics
    ADD COLUMN seed_mean REAL,
    ADD COLUMN seed_std  REAL,
    ADD COLUMN perf_mean REAL,
    ADD COLUMN perf_std  REAL;
