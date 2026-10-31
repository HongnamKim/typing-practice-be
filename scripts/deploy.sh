#!/usr/bin/env bash
# 쉬는 색을 새 이미지로 띄워 확인한 뒤 nginx 를 전환한다.
# 사용법: scripts/deploy.sh <image-uri>
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=scripts/lib.sh
source "$SCRIPT_DIR/lib.sh"

image_uri="${1:?사용법: scripts/deploy.sh <image-uri>}"
tmp_env=""
trap 'rm -f "$tmp_env"' EXIT

cd "$PROJECT_DIR"

# 1. 사전 검사: 실패하면 아무것도 바꾸지 않는다.
acquire_lock
check_preconditions
active="$(active_color)" || die "활성 색을 읽을 수 없습니다: $UPSTREAM_FILE"
idle="$(other_color "$active")"

umask 077
tmp_env="$(mktemp "$PROJECT_DIR/.env.prod.XXXXXX")"
aws ssm get-parameters-by-path --path "$SSM_PATH" --with-decryption --region "$AWS_REGION" --output json \
  | python3 "$SCRIPT_DIR/render_env.py" > "$tmp_env" \
  || die "SSM 에서 비밀 값을 읽지 못했습니다."

# 2. 준비
log "활성 $active, 새 이미지는 $idle 에 배포: $image_uri"
registry="${image_uri%%/*}"
aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$registry"
pull_status=0
docker pull -q "$image_uri" || pull_status=$?
docker logout "$registry" > /dev/null || true
[ "$pull_status" -eq 0 ] || die "이미지를 받지 못했습니다: $image_uri"

mv "$tmp_env" "$PROJECT_DIR/.env.prod"
tmp_env=""
# 쉬는 색을 덮어쓰므로 이 시점부터 그 색으로는 돌아갈 수 없다.
rm -f "$ROLLBACK_FILE"
docker tag "$image_uri" "typing-practice-be:$idle"
compose --profile "$idle" rm -fs "app-$idle"

# 3. 기동: 실패해도 트래픽은 옛 색에 있다.
log "app-$idle 기동"
if ! compose --profile "$idle" up -d --no-deps --wait --wait-timeout 180 "app-$idle"; then
  compose --profile "$idle" logs --tail 50 "app-$idle" || true
  compose --profile "$idle" rm -fs "app-$idle"
  die "app-$idle 이 정상 상태가 되지 못했습니다. 트래픽은 $active 에 그대로 있습니다."
fi

# 4. 예열: 전환 직후 첫 요청이 느려지는 것을 줄인다. 실패해도 계속한다.
# shellcheck disable=SC2016
compose --profile "$idle" exec -T "app-$idle" sh -c '
  for path in /announcements /update-notes \
    "/quotes?seed=0&language=KOREAN" "/quotes?seed=0&language=ENGLISH" \
    "/words?language=KOREAN" "/words?language=ENGLISH"; do
    i=0
    while [ "$i" -lt 10 ]; do
      wget -T 5 -qO /dev/null "http://localhost:8080$path" || true
      i=$((i + 1))
    done
  done' || true

# 5. 전환
log "nginx 를 $idle 로 전환"
if ! switch_upstream "$idle"; then
  stop_color "$idle"
  die "nginx 전환에 실패했습니다. 트래픽은 $active 에 그대로 있습니다."
fi

# 6. 검증
if ! verify_color "$idle"; then
  restore_or_keep "$active" "$idle"
  die "전환을 확인하지 못했습니다."
fi

# 7. 정리: 처리 중인 요청이 끝나길 기다린 뒤 옛 색을 멈춘다. 컨테이너는 롤백용으로 남긴다.
sleep 15
stop_color "$active"
echo "$active" > "$ROLLBACK_FILE"
docker rmi "$image_uri" > /dev/null || true
docker image prune -f > /dev/null || true
log "배포 완료: $idle"
