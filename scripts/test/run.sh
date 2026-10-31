#!/usr/bin/env bash
# deploy.sh, rollback.sh 테스트. 외부 명령은 stubs 로 바꾸고 임시 PROJECT_DIR 에서 실행한다.
set -u
# shellcheck disable=SC2016

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
export PATH="$ROOT/scripts/test/stubs:$PATH"
IMAGE="123.dkr.ecr.ap-northeast-2.amazonaws.com/typing-practice-be:abc1234"
fails=0
tests=0

setup() {
  PROJECT_DIR="$(mktemp -d)"
  export PROJECT_DIR
  export CALLS_LOG="$PROJECT_DIR/calls"
  mkdir -p "$PROJECT_DIR/nginx/runtime"
  : > "$CALLS_LOG"
  printf 'ORIGINAL=1\n' > "$PROJECT_DIR/.env.prod"
  set_upstream blue
  unset STUB_NOW STUB_AVAILABLE STUB_SWAP_FREE STUB_SSM_FAIL STUB_SSM_JSON STUB_PULL_FAIL \
    STUB_UP_FAIL STUB_NGINX_T_FAIL STUB_NGINX_T_FAIL_ON STUB_RESPONSE_COLOR STUB_LOCK_FAIL
}

teardown() { rm -rf "$PROJECT_DIR"; }

set_upstream() {
  printf 'set $app_upstream http://app-%s:8080; set $app_color %s;\n' "$1" "$1" \
    > "$PROJECT_DIR/nginx/runtime/app-upstream.conf"
}

upstream() { sed -n 's/.*set \$app_color \([a-z]*\);.*/\1/p' "$PROJECT_DIR/nginx/runtime/app-upstream.conf"; }
rollback_color() { cat "$PROJECT_DIR/nginx/runtime/rollback-color" 2> /dev/null; }
deploy() { bash "$ROOT/scripts/deploy.sh" "$IMAGE" > "$PROJECT_DIR/out" 2>&1; }
rollback() { bash "$ROOT/scripts/rollback.sh" > "$PROJECT_DIR/out" 2>&1; }

check() {
  tests=$((tests + 1))
  if [ "$2" = "$3" ]; then
    echo "PASS $1"
  else
    echo "FAIL $1: expected [$2] got [$3]"
    fails=$((fails + 1))
  fi
}

called() { if grep -qE "$1" "$CALLS_LOG"; then echo yes; else echo no; fi; }
docker_calls() { wc -l < "$CALLS_LOG" | tr -d ' '; }
env_file() { cat "$PROJECT_DIR/.env.prod"; }

blackout() { (PROJECT_DIR="$PROJECT_DIR" bash -c "source '$ROOT/scripts/lib.sh'; if in_blackout $1; then echo yes; else echo no; fi"); }

setup
check "시간 경계 0244" no "$(blackout 0244)"
check "시간 경계 0245" yes "$(blackout 0245)"
check "시간 경계 0404" yes "$(blackout 0404)"
check "시간 경계 0405" no "$(blackout 0405)"
check "시간 경계 0800" no "$(blackout 0800)"
teardown

setup
printf 'set $app_upstream http://app-green:8080; set $app_color blue;\n' > "$PROJECT_DIR/nginx/runtime/app-upstream.conf"
deploy; rc=$?
check "upstream 색 불일치: 실패" 1 "$rc"
check "upstream 색 불일치: docker 0건" 0 "$(docker_calls)"
teardown

setup; export STUB_NOW=0300; deploy; rc=$?
check "금지 시간: 실패" 1 "$rc"
check "금지 시간: docker 0건" 0 "$(docker_calls)"
check "금지 시간: .env.prod 그대로" "ORIGINAL=1" "$(env_file)"
teardown

setup; export STUB_AVAILABLE=500 STUB_SWAP_FREE=499; deploy; rc=$?
check "메모리 999: 실패" 1 "$rc"
check "메모리 999: docker 0건" 0 "$(docker_calls)"
teardown

setup; export STUB_AVAILABLE=500 STUB_SWAP_FREE=500; deploy; rc=$?
check "메모리 1000: 통과" 0 "$rc"
teardown

setup; export STUB_SSM_FAIL=1; deploy; rc=$?
check "SSM 실패: 실패" 1 "$rc"
check "SSM 실패: .env.prod 그대로" "ORIGINAL=1" "$(env_file)"
check "SSM 실패: docker 0건" 0 "$(docker_calls)"
check "SSM 실패: 임시 파일 없음" 0 "$(find "$PROJECT_DIR" -name '.env.prod.*' | wc -l | tr -d ' ')"
teardown

setup; export STUB_SSM_JSON='{"Parameters":[{"Name":"/typing-practice/prod/DB_URL","Value":"x"}]}'; deploy; rc=$?
check "키 누락: 실패" 1 "$rc"
check "키 누락: .env.prod 그대로" "ORIGINAL=1" "$(env_file)"
check "키 누락: docker 0건" 0 "$(docker_calls)"
teardown

setup; export STUB_LOCK_FAIL=1; deploy; rc=$?
check "동시 실행: 실패" 1 "$rc"
check "동시 실행: docker 0건" 0 "$(docker_calls)"
teardown

setup; rm "$PROJECT_DIR/nginx/runtime/app-upstream.conf"; deploy; rc=$?
check "upstream 없음: 실패" 1 "$rc"
check "upstream 없음: .env.prod 그대로" "ORIGINAL=1" "$(env_file)"
check "upstream 없음: docker 0건" 0 "$(docker_calls)"
teardown

setup; set_upstream red; deploy; rc=$?
check "upstream 깨짐: 실패" 1 "$rc"
check "upstream 깨짐: docker 0건" 0 "$(docker_calls)"
teardown

setup; echo green > "$PROJECT_DIR/nginx/runtime/rollback-color"; export STUB_PULL_FAIL=1; deploy; rc=$?
check "pull 실패: 실패" 1 "$rc"
check "pull 실패: upstream 그대로" blue "$(upstream)"
check "pull 실패: 롤백 정보 유지" green "$(rollback_color)"
check "pull 실패: 기동 없음" no "$(called ' up ')"
teardown

setup; export STUB_UP_FAIL=1; deploy; rc=$?
check "기동 실패: 실패" 1 "$rc"
check "기동 실패: upstream 그대로" blue "$(upstream)"
check "기동 실패: 새 색 삭제" yes "$(called 'rm .*app-green')"
check "기동 실패: rollback-color 없음" "" "$(rollback_color)"
check "기동 실패: 옛 색 정지 없음" no "$(called 'stop app-blue')"
unset STUB_UP_FAIL; : > "$CALLS_LOG"; deploy; rc=$?
check "실패 후 재배포: 성공" 0 "$rc"
check "실패 후 재배포: upstream 전환" green "$(upstream)"
check "실패 후 재배포: 남은 컨테이너 먼저 삭제" yes "$(called 'rm .*app-green')"
teardown

setup; export STUB_NGINX_T_FAIL=1; deploy; rc=$?
check "nginx -t 실패: 실패" 1 "$rc"
check "nginx -t 실패: upstream 복구" blue "$(upstream)"
check "nginx -t 실패: 옛 색 정지 없음" no "$(called 'stop app-blue')"
check "nginx -t 실패: 새 색 정지" yes "$(called 'stop app-green')"
teardown

setup; export STUB_RESPONSE_COLOR=blue; deploy; rc=$?
check "전환 검증 실패: 실패" 1 "$rc"
check "전환 검증 실패: upstream 복구" blue "$(upstream)"
check "전환 검증 실패: 새 색 정지" yes "$(called 'stop app-green')"
check "전환 검증 실패: 옛 색 정지 없음" no "$(called 'stop app-blue')"
check "전환 검증 실패: rollback-color 없음" "" "$(rollback_color)"
teardown

setup; export STUB_RESPONSE_COLOR=blue STUB_NGINX_T_FAIL_ON=2; deploy; rc=$?
check "되돌리기 실패: 실패" 1 "$rc"
check "되돌리기 실패: 받고 있는 새 색 유지" no "$(called 'stop app-green')"
check "되돌리기 실패: 옛 색 유지" no "$(called 'stop app-blue')"
teardown

setup; deploy; rc=$?
check "정상 배포: 성공" 0 "$rc"
check "정상 배포: upstream 전환" green "$(upstream)"
check "정상 배포: rollback-color" blue "$(rollback_color)"
check "정상 배포: 옛 색 정지" yes "$(called 'stop app-blue')"
check "정상 배포: 옛 색 컨테이너 유지" no "$(called 'rm .*app-blue')"
check "정상 배포: 이미지 태그" yes "$(called "tag $IMAGE typing-practice-be:green")"
check "정상 배포: 다른 서비스 재생성 없음" no "$(called 'redis|grafana|prometheus|remove-orphans')"
check "정상 배포: up 은 항상 --no-deps" 0 "$(grep ' up ' "$CALLS_LOG" | grep -vc -- '--no-deps')"
check "정상 배포: ECR 로그아웃" yes "$(called 'logout ')"
check "정상 배포: 예열은 문장 조회 파라미터 사용" yes "$(called 'quotes\?seed=0&language=KOREAN')"
check "정상 배포: 예열은 단어 조회 파라미터 사용" yes "$(called 'words\?language=ENGLISH')"
check "정상 배포: 예열 요청 제한 시간" yes "$(called 'wget -T 5')"
check "정상 배포: .env.prod 값" "DB_URL='secret-DB_URL'" "$(grep '^DB_URL=' "$PROJECT_DIR/.env.prod")"
check "정상 배포: .env.prod 권한 600" 600 "$(python3 -c "import os,sys;print(oct(os.stat(sys.argv[1]).st_mode)[-3:])" "$PROJECT_DIR/.env.prod")"
check "정상 배포: 출력에 비밀 값 없음" no "$(if grep -q 'secret-' "$PROJECT_DIR/out"; then echo yes; else echo no; fi)"
deploy; rc=$?
check "연속 배포: 성공" 0 "$rc"
check "연속 배포: upstream 복귀" blue "$(upstream)"
check "연속 배포: rollback-color" green "$(rollback_color)"
teardown

setup; rollback; rc=$?
check "롤백 거부: 실패" 1 "$rc"
check "롤백 거부: docker 0건" 0 "$(docker_calls)"
check "롤백 거부: 안내 출력" yes "$(if grep -q 'scripts/deploy.sh' "$PROJECT_DIR/out"; then echo yes; else echo no; fi)"
teardown

setup; set_upstream green; echo blue > "$PROJECT_DIR/nginx/runtime/rollback-color"; rollback; rc=$?
check "롤백: 성공" 0 "$rc"
check "롤백: upstream 복귀" blue "$(upstream)"
check "롤백: 옛 색을 재생성 없이 기동" yes "$(called 'up .*--no-recreate.*app-blue')"
check "롤백: 현재 색 정지" yes "$(called 'stop app-green')"
check "롤백: rollback-color 교체" green "$(rollback_color)"
check "롤백: 이미지 pull 없음" no "$(called ' pull ')"
teardown

setup; set_upstream green; echo blue > "$PROJECT_DIR/nginx/runtime/rollback-color"; export STUB_UP_FAIL=1; rollback; rc=$?
check "롤백 기동 실패: 실패" 1 "$rc"
check "롤백 기동 실패: upstream 그대로" green "$(upstream)"
check "롤백 기동 실패: 현재 색 정지 없음" no "$(called 'stop app-green')"
check "롤백 기동 실패: 옛 색 정지" yes "$(called 'stop app-blue')"
teardown

setup; set_upstream green; echo blue > "$PROJECT_DIR/nginx/runtime/rollback-color"; export STUB_RESPONSE_COLOR=green; rollback; rc=$?
check "롤백 검증 실패: 실패" 1 "$rc"
check "롤백 검증 실패: upstream 복구" green "$(upstream)"
check "롤백 검증 실패: 현재 색 정지 없음" no "$(called 'stop app-green')"
teardown

setup; set_upstream green; echo green > "$PROJECT_DIR/nginx/runtime/rollback-color"; rollback; rc=$?
check "롤백 정보 불일치: 실패" 1 "$rc"
check "롤백 정보 불일치: docker 0건" 0 "$(docker_calls)"
teardown

setup; set_upstream green; echo blue > "$PROJECT_DIR/nginx/runtime/rollback-color"; export STUB_NOW=0300; rollback; rc=$?
check "금지 시간 롤백: 실패" 1 "$rc"
check "금지 시간 롤백: docker 0건" 0 "$(docker_calls)"
teardown

echo "$tests tests, $fails failed"
[ "$fails" -eq 0 ]
