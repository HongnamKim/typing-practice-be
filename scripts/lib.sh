#!/usr/bin/env bash
# deploy.sh, rollback.sh 공용 함수
# shellcheck disable=SC2034,SC2016

PROJECT_DIR="${PROJECT_DIR:-/home/ubuntu/typing-practice-be}"
RUNTIME_DIR="$PROJECT_DIR/nginx/runtime"
UPSTREAM_FILE="$RUNTIME_DIR/app-upstream.conf"
ROLLBACK_FILE="$RUNTIME_DIR/rollback-color"
LOCK_FILE="$RUNTIME_DIR/.deploy.lock"
API_HOST="api.typing-practice.com"
AWS_REGION="ap-northeast-2"
SSM_PATH="/typing-practice/prod"
MIN_MEMORY_MIB=1000

log() { echo "[$(TZ=Asia/Seoul date '+%H%M')] $*"; }
die() {
  echo "실패: $*" >&2
  exit 1
}

compose() { docker compose -f docker-compose.prod.yaml "$@"; }

# 03시·04시 배치가 두 앱에서 함께 도는 것을 막는다. 시작 시각은 훅 제한 시간 600초를 뺀 값이다.
in_blackout() {
  local now=$((10#$1))
  [ "$now" -ge 245 ] && [ "$now" -lt 405 ]
}

has_memory() {
  free -m | awk -v min="$MIN_MEMORY_MIB" '
    /^Mem:/ { available = $7 }
    /^Swap:/ { swap_free = $4 }
    END { exit !(available + swap_free >= min) }'
}

active_color() {
  local color upstream
  [ -f "$UPSTREAM_FILE" ] || return 1
  color="$(sed -n 's/.*set \$app_color \([a-z]*\);.*/\1/p' "$UPSTREAM_FILE")"
  upstream="$(sed -n 's#.*set \$app_upstream http://app-\([a-z]*\):8080;.*#\1#p' "$UPSTREAM_FILE")"
  [ "$color" = "$upstream" ] || return 1
  case "$color" in
    blue | green) echo "$color" ;;
    *) return 1 ;;
  esac
}

other_color() {
  case "$1" in
    blue) echo green ;;
    green) echo blue ;;
    *) return 1 ;;
  esac
}

acquire_lock() {
  exec 9> "$LOCK_FILE"
  flock -n 9 || die "다른 배포가 실행 중입니다."
}

check_preconditions() {
  in_blackout "$(TZ=Asia/Seoul date +%H%M)" && die "배포 금지 시간입니다 (KST 02:45~04:05). 시간이 지난 뒤 재시도하세요."
  has_memory || die "메모리 여유가 ${MIN_MEMORY_MIB}MiB 미만입니다."
  return 0
}

write_upstream() {
  local tmp="$UPSTREAM_FILE.tmp"
  printf 'set $app_upstream http://app-%s:8080; set $app_color %s;\n' "$1" "$1" > "$tmp"
  mv "$tmp" "$UPSTREAM_FILE"
}

# 설정 검사나 reload 가 실패하면 파일을 되돌린다. 이때 nginx 는 기존 설정 그대로다.
switch_upstream() {
  local backup="$UPSTREAM_FILE.prev"
  cp "$UPSTREAM_FILE" "$backup"
  write_upstream "$1"
  if compose exec -T nginx nginx -t && compose exec -T nginx nginx -s reload; then
    rm -f "$backup"
    return 0
  fi
  mv "$backup" "$UPSTREAM_FILE"
  return 1
}

# reload 는 신호만 보내므로 새 연결의 응답 헤더로 전환을 확인한다.
verify_color() {
  local attempt=0 headers
  while [ "$attempt" -lt 30 ]; do
    headers="$(curl -s -o /dev/null -D - --max-time 3 \
      --resolve "$API_HOST:443:127.0.0.1" "https://$API_HOST/actuator/health" | tr -d '\r' || true)"
    if grep -qE '^HTTP/[0-9.]+ 200' <<< "$headers" && grep -qix "x-app-color: $1" <<< "$headers"; then
      return 0
    fi
    attempt=$((attempt + 1))
    sleep 1
  done
  return 1
}

stop_color() { compose --profile "$1" stop "app-$1"; }

# 전환 후 검증이 실패했을 때 옛 색으로 되돌린다. 되돌리지 못하면 트래픽을 받는 새 색을 멈추지 않는다.
restore_or_keep() {
  local previous="$1" current="$2"
  if switch_upstream "$previous"; then
    stop_color "$current"
    echo "옛 색($previous)으로 되돌렸습니다." >&2
  else
    echo "되돌리기에 실패했습니다. 트래픽은 $current 에 있으니 EC2 에서 직접 확인하세요." >&2
  fi
}
