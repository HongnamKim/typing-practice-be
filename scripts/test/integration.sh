#!/usr/bin/env bash
# 실제 Docker 로 deploy.sh, rollback.sh 를 돌린다. 맥의 80, 443 포트를 잠시 사용한다.
# AWS 와 이미지 pull 만 흉내 내고, 앱은 헬스체크에 응답하는 작은 이미지로 대신한다.
set -u
# shellcheck disable=SC2016

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
export PROJECT_DIR COMPOSE_PROJECT_NAME=bg-integration-test
PROJECT_DIR="$(mktemp -d)"
BIN="$PROJECT_DIR/.bin"
REAL_DOCKER="$(command -v docker)"
REAL_CURL="$(command -v curl)"
REGISTRY="fake.local/typing-practice-be"
fails=0

cleanup() {
  (cd "$PROJECT_DIR" && "$REAL_DOCKER" compose -f docker-compose.prod.yaml --profile blue --profile green down -v --remove-orphans > /dev/null 2>&1)
  "$REAL_DOCKER" rmi -f "$REGISTRY:v1" "$REGISTRY:v2" "$REGISTRY:v3" "$REGISTRY:crash" typing-practice-be:blue typing-practice-be:green > /dev/null 2>&1
  rm -rf "$PROJECT_DIR"
}
trap cleanup EXIT

check() {
  if [ "$2" = "$3" ]; then
    echo "PASS $1"
  else
    echo "FAIL $1: expected [$2] got [$3]"
    fails=$((fails + 1))
  fi
}

build_image() {
  "$REAL_DOCKER" build -q -t "$1" -f - "$PROJECT_DIR/.empty" > /dev/null <<DOCKERFILE
FROM busybox:1.36
RUN mkdir -p /www/actuator && echo '{"status":"UP"}' > /www/actuator/health && echo '$2' > /www/version
ENTRYPOINT ["sh", "-c", "$3"]
DOCKERFILE
}

# 준비: 프로젝트 사본. 맥에 없는 Grafana 와 사용 중인 7070 포트만 바꾼다.
mkdir -p "$BIN" "$PROJECT_DIR/.empty" "$PROJECT_DIR/nginx/runtime" "$PROJECT_DIR/fallback" \
  "$PROJECT_DIR/certbot/www" "$PROJECT_DIR/certbot/conf/live/api.typing-practice.com"
sed "s/'7070:7070'/'17070:7070'/" "$ROOT/docker-compose.prod.yaml" > "$PROJECT_DIR/docker-compose.prod.yaml"
sed 's#http://grafana:3000#http://127.0.0.1:3000#' "$ROOT/nginx/nginx.conf" > "$PROJECT_DIR/nginx/nginx.conf"
touch "$PROJECT_DIR/.env.prod"
openssl req -x509 -newkey rsa:2048 -nodes -days 1 -subj "/CN=api.typing-practice.com" \
  -keyout "$PROJECT_DIR/certbot/conf/live/api.typing-practice.com/privkey.pem" \
  -out "$PROJECT_DIR/certbot/conf/live/api.typing-practice.com/fullchain.pem" > /dev/null 2>&1

cat > "$BIN/docker" <<WRAP
#!/usr/bin/env bash
case "\$1" in
  login) cat > /dev/null; exit 0 ;;
  logout | pull) exit 0 ;;
esac
exec "$REAL_DOCKER" "\$@"
WRAP
cat > "$BIN/curl" <<WRAP
#!/usr/bin/env bash
exec "$REAL_CURL" -k "\$@"
WRAP
cp "$ROOT/scripts/test/stubs/aws" "$ROOT/scripts/test/stubs/free" "$ROOT/scripts/test/stubs/date" \
  "$ROOT/scripts/test/stubs/flock" "$BIN/"
chmod +x "$BIN"/*
export PATH="$BIN:$PATH"
export CALLS_LOG="$PROJECT_DIR/calls"

serve='trap \"exit 0\" TERM; httpd -f -p 8080 -h /www & wait'
build_image "$REGISTRY:v1" v1 "$serve"
build_image "$REGISTRY:v2" v2 "$serve"
build_image "$REGISTRY:v3" v3 "$serve"
build_image "$REGISTRY:crash" crash "exit 1"

api() { "$REAL_CURL" -sk --max-time 5 --resolve "api.typing-practice.com:443:127.0.0.1" "https://api.typing-practice.com$1"; }
color_header() { api_headers="$("$REAL_CURL" -sk -o /dev/null -D - --max-time 5 --resolve "api.typing-practice.com:443:127.0.0.1" "https://api.typing-practice.com/actuator/health" | tr -d '\r')"; grep -i '^x-app-color:' <<< "$api_headers" | awk '{print $2}'; }
state() {
  local value
  value="$("$REAL_DOCKER" inspect -f '{{.State.Status}}' "$COMPOSE_PROJECT_NAME-app-$1-1" 2> /dev/null | tr -d '\n')"
  echo "${value:-none}"
}
upstream_color() { sed -n 's/.*set \$app_color \([a-z]*\);.*/\1/p' "$PROJECT_DIR/nginx/runtime/app-upstream.conf"; }

# 첫 상태: blue 가 v1 로 서비스 중
cd "$PROJECT_DIR" || exit 1
docker tag "$REGISTRY:v1" typing-practice-be:blue
printf 'set $app_upstream http://app-blue:8080; set $app_color blue;\n' > nginx/runtime/app-upstream.conf
docker compose -f docker-compose.prod.yaml --profile blue up -d --no-deps --wait --wait-timeout 60 app-blue > /dev/null 2>&1
docker compose -f docker-compose.prod.yaml up -d --no-deps nginx > /dev/null 2>&1
sleep 2
check "초기: v1 응답" v1 "$(api /version)"
check "초기: 헤더 blue" blue "$(color_header)"

# 배포 중에도 끊기지 않는지 계속 호출한다.
errors_file="$PROJECT_DIR/.errors"
(
  while [ ! -f "$PROJECT_DIR/.stop" ]; do
    code="$("$REAL_CURL" -sk -o /dev/null -w '%{http_code}' --max-time 3 --resolve "api.typing-practice.com:443:127.0.0.1" "https://api.typing-practice.com/version")"
    case "$code" in
      200) ;;
      *) echo "$code" >> "$errors_file" ;;
    esac
    sleep 0.1
  done
) &
loader=$!

bash "$ROOT/scripts/deploy.sh" "$REGISTRY:v2" > "$PROJECT_DIR/deploy.log" 2>&1; rc=$?
check "배포: 성공" 0 "$rc"
[ "$rc" -eq 0 ] || tail -15 "$PROJECT_DIR/deploy.log"
check "배포: v2 응답" v2 "$(api /version)"
check "배포: 헤더 green" green "$(color_header)"
check "배포: blue 정지, 컨테이너 유지" exited "$(state blue)"
check "배포: green 실행" running "$(state green)"
check "배포: .env.prod 값" "DB_URL='secret-DB_URL'" "$(grep '^DB_URL=' .env.prod)"

bash "$ROOT/scripts/rollback.sh" > "$PROJECT_DIR/rollback.log" 2>&1; rc=$?
check "롤백: 성공" 0 "$rc"
[ "$rc" -eq 0 ] || tail -15 "$PROJECT_DIR/rollback.log"
check "롤백: v1 응답" v1 "$(api /version)"
check "롤백: 헤더 blue" blue "$(color_header)"
check "롤백: green 정지" exited "$(state green)"

touch "$PROJECT_DIR/.stop"
wait "$loader"
check "배포·롤백 중 5xx 와 끊김 0건" 0 "$( [ -f "$errors_file" ] && wc -l < "$errors_file" | tr -d ' ' || echo 0)"

bash "$ROOT/scripts/deploy.sh" "$REGISTRY:crash" > "$PROJECT_DIR/crash.log" 2>&1; rc=$?
check "기동 실패: 파이프라인 실패" 1 "$rc"
check "기동 실패: v1 유지" v1 "$(api /version)"
check "기동 실패: upstream 유지" blue "$(upstream_color)"
check "기동 실패: 새 색 컨테이너 삭제" none "$(state green)"

bash "$ROOT/scripts/rollback.sh" > "$PROJECT_DIR/refused.log" 2>&1; rc=$?
check "실패 직후 롤백 거부" 1 "$rc"

bash "$ROOT/scripts/deploy.sh" "$REGISTRY:v3" > "$PROJECT_DIR/again.log" 2>&1; rc=$?
check "실패 후 재배포: 성공" 0 "$rc"
check "실패 후 재배포: v3 응답" v3 "$(api /version)"

if [ "$fails" -ne 0 ]; then
  echo "$fails FAILED"
  exit 1
fi
echo "ALL PASS"
