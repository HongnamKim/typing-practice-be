#!/usr/bin/env bash
# env 파일의 값을 SSM Parameter Store 의 SecureString 으로 등록한다. 값은 출력하지 않는다.
# 사용법: scripts/ssm-put-env.sh <env-file>
set -euo pipefail

PARAM_PREFIX="/typing-practice/prod"
env_file="${1:?사용법: scripts/ssm-put-env.sh <env-file>}"

if [ ! -f "$env_file" ]; then
  echo "파일이 없습니다: $env_file" >&2
  exit 1
fi

# CLI v1 은 http:// 로 시작하는 값을 URL 로 보고 내용을 내려받는다.
case "$(aws --version 2>&1)" in
  aws-cli/2.*) ;;
  *)
    echo "AWS CLI v2 가 필요합니다." >&2
    exit 1
    ;;
esac

while IFS= read -r line || [ -n "$line" ]; do
  case "$line" in
    '' | '#'*) continue ;;
  esac

  key="${line%%=*}"
  value="${line#*=}"

  case "$value" in
    \'*\') value="${value#\'}"; value="${value%\'}" ;;
    \"*\") value="${value#\"}"; value="${value%\"}" ;;
  esac

  aws ssm put-parameter \
    --name "$PARAM_PREFIX/$key" \
    --type SecureString \
    --overwrite \
    --value "$value" > /dev/null
  echo "등록: $PARAM_PREFIX/$key"
done < "$env_file"
