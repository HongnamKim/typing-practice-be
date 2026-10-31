#!/usr/bin/env bash
# 멈춰 둔 옛 색으로 되돌린다. 현재 색이 계속 서비스하므로 중단은 없고, Spring 기동 시간만큼 걸린다.
# 사용법: scripts/rollback.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=scripts/lib.sh
source "$SCRIPT_DIR/lib.sh"

cd "$PROJECT_DIR"

acquire_lock
check_preconditions
current="$(active_color)" || die "활성 색을 읽을 수 없습니다: $UPSTREAM_FILE"

if [ ! -f "$ROLLBACK_FILE" ]; then
  echo "되돌릴 색이 없습니다. 직전 배포가 실패했다면 활성 색은 정상 버전 그대로입니다." >&2
  die "더 이전 버전으로 돌아가려면 scripts/deploy.sh <ECR 의 이전 이미지 URI> 를 사용하세요."
fi

previous="$(cat "$ROLLBACK_FILE")"
[ "$previous" = "$(other_color "$current")" ] || die "rollback-color 내용이 잘못됐습니다: $previous"

log "app-$previous 기동"
# 멈춰 둔 컨테이너를 그대로 쓴다. 비밀 값을 교체했다면 헬스체크에서 실패하고 전환은 일어나지 않는다.
if ! compose --profile "$previous" up -d --no-deps --no-recreate --wait --wait-timeout 180 "app-$previous"; then
  stop_color "$previous"
  die "app-$previous 이 정상 상태가 되지 못했습니다. 비밀 값을 교체했다면 deploy.sh 로 되돌리세요."
fi

log "nginx 를 $previous 로 전환"
if ! switch_upstream "$previous"; then
  stop_color "$previous"
  die "nginx 전환에 실패했습니다. 트래픽은 $current 에 그대로 있습니다."
fi

if ! verify_color "$previous"; then
  restore_or_keep "$current" "$previous"
  die "전환을 확인하지 못했습니다."
fi

sleep 15
stop_color "$current"
echo "$current" > "$ROLLBACK_FILE"
log "롤백 완료: $previous"
