#!/usr/bin/env bash
# CodeDeploy ApplicationStart 훅. 번들의 image-uri.txt 로 번들의 deploy.sh 를 실행한다.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

image_uri="$(cat "$SCRIPT_DIR/../image-uri.txt")"
if [ -z "$image_uri" ]; then
  echo "실패: image-uri.txt 가 비어 있습니다." >&2
  exit 1
fi

exec bash "$SCRIPT_DIR/deploy.sh" "$image_uri"
