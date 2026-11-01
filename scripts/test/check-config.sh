#!/usr/bin/env bash
# 운영 compose·nginx 설정 검사. 임시 폴더에서 더미 값으로 확인한다.
set -u
# shellcheck disable=SC2034,SC2016

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
fails=0

pass() { echo "PASS $1"; }
fail() { echo "FAIL $1"; fails=$((fails + 1)); }
expect() { if eval "$2"; then pass "$1"; else fail "$1"; fi; }

cp "$ROOT/docker-compose.prod.yaml" "$TMP/"
touch "$TMP/.env.prod"
compose() { docker compose -f "$TMP/docker-compose.prod.yaml" "$@"; }

blue_services="$(compose --profile blue config --services 2>&1)"
green_services="$(compose --profile green config --services 2>&1)"
default_services="$(compose config --services 2>&1)"
config="$(compose --profile blue config 2>&1)"

expect "blue profile 에 app-blue 있음" 'grep -qx app-blue <<< "$blue_services"'
expect "blue profile 에 app-green 없음" '! grep -qx app-green <<< "$blue_services"'
expect "green profile 에 app-green 있음" 'grep -qx app-green <<< "$green_services"'
expect "profile 없으면 두 색 모두 없음" '! grep -qE "^app-(blue|green)$" <<< "$default_services"'
expect "기존 app 서비스 없음" '! grep -qx app <<< "$default_services"'
expect "nginx 는 profile 없이 실행됨" 'grep -qx nginx <<< "$default_services"'
expect "호스트 8080 공개 없음" '! grep -q "published: \"8080\"" <<< "$config"'
expect "restart unless-stopped" 'grep -q "restart: unless-stopped" <<< "$config"'
expect "pull_policy never" 'grep -q "pull_policy: never" <<< "$config"'
expect "stop_grace_period 35s" 'grep -q "stop_grace_period: 35s" <<< "$config"'
expect "healthcheck 는 health 경로 사용" 'grep -q "actuator/health" <<< "$config"'
expect "runtime 디렉토리 마운트" 'grep -q "target: /etc/nginx/runtime" <<< "$config"'

expect "prometheus 두 색 수집" 'grep -q "app-blue:8080" "$ROOT/prometheus-prod.yml" && grep -q "app-green:8080" "$ROOT/prometheus-prod.yml"'
expect "prometheus 옛 대상 없음" '! grep -q "app:8080" "$ROOT/prometheus-prod.yml"'

mkdir -p "$TMP/certs/live/api.typing-practice.com" "$TMP/runtime"
openssl req -x509 -newkey rsa:2048 -nodes -days 1 -subj "/CN=api.typing-practice.com" \
  -keyout "$TMP/certs/live/api.typing-practice.com/privkey.pem" \
  -out "$TMP/certs/live/api.typing-practice.com/fullchain.pem" > /dev/null 2>&1
printf 'set $app_upstream http://app-blue:8080; set $app_color blue;\n' > "$TMP/runtime/app-upstream.conf"

expect "nginx 설정 문법" 'docker run --rm --add-host grafana:127.0.0.1 \
  -v "$ROOT/nginx/nginx.conf:/etc/nginx/conf.d/default.conf:ro" \
  -v "$TMP/certs:/etc/letsencrypt:ro" \
  -v "$TMP/runtime:/etc/nginx/runtime:ro" \
  nginx:alpine nginx -t > /dev/null 2>&1'
expect "nginx 는 upstream 파일 사용" 'grep -q "include /etc/nginx/runtime/app-upstream.conf" "$ROOT/nginx/nginx.conf"'
expect "nginx 는 X-App-Color 응답" 'grep -q "add_header X-App-Color \$app_color always" "$ROOT/nginx/nginx.conf"'
expect "nginx 7070 Grafana 유지" 'grep -q "proxy_pass http://grafana:3000" "$ROOT/nginx/nginx.conf"'

expect "runtime 은 git 제외" 'git -C "$ROOT" check-ignore -q nginx/runtime/app-upstream.conf'
expect ".env.prod 임시 파일은 git 제외" 'git -C "$ROOT" check-ignore -q .env.prod.tmp123'

expect "appspec 은 ApplicationStart 훅 사용" 'grep -q "ApplicationStart:" "$ROOT/appspec.yml"'
expect "appspec 은 래퍼 실행" 'grep -q "location: scripts/codedeploy-start.sh" "$ROOT/appspec.yml"'
expect "appspec 은 ubuntu 로 실행" 'grep -q "runas: ubuntu" "$ROOT/appspec.yml"'
expect "appspec 제한 시간 600초" 'grep -q "timeout: 600" "$ROOT/appspec.yml"'
expect "appspec 은 파일을 복사하지 않음" '[ -f "$ROOT/appspec.yml" ] && ! grep -q "^files:" "$ROOT/appspec.yml"'
expect "appspec 은 ApplicationStop 훅 없음" '[ -f "$ROOT/appspec.yml" ] && ! grep -q "ApplicationStop:" "$ROOT/appspec.yml"'
expect "빌드 산출물에 appspec 포함" 'grep -q "appspec.yml" "$ROOT/buildspec.yml"'
expect "빌드 산출물에 compose 포함" 'grep -q "docker-compose.prod.yaml" "$ROOT/buildspec.yml"'

if [ "$fails" -ne 0 ]; then
  echo "$fails FAILED"
  exit 1
fi
echo "ALL PASS"
