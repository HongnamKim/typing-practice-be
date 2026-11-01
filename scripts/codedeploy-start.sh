#!/usr/bin/env bash
# CodeDeploy ApplicationStart 훅. 번들의 image-uri.txt 로 번들의 deploy.sh 를 실행한다.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=scripts/lib.sh
source "$SCRIPT_DIR/lib.sh"

image_uri="$(cat "$SCRIPT_DIR/../image-uri.txt")"
[ -n "$image_uri" ] || die "image-uri.txt 가 비어 있습니다."

# compose 는 EC2 의 checkout 을 쓴다. 다르면 옛 정의로 배포되고도 성공으로 보이므로 아무것도 바꾸기 전에 멈춘다.
cmp -s "$SCRIPT_DIR/../docker-compose.prod.yaml" "$PROJECT_DIR/docker-compose.prod.yaml" \
  || die "EC2 의 compose 파일이 이번 커밋과 다릅니다. EC2 에서 git pull --ff-only origin prod 후 Deploy 단계를 재시도하세요."

exec bash "$SCRIPT_DIR/deploy.sh" "$image_uri"
