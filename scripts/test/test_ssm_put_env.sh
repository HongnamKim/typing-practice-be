#!/usr/bin/env bash
# scripts/ssm-put-env.sh 테스트. aws 를 스텁으로 바꿔 전달 인자를 확인한다.
set -u

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
fails=0

mkdir -p "$TMP/bin"
cat > "$TMP/bin/aws" <<'STUB'
#!/usr/bin/env bash
if [ "$1" = "--version" ]; then echo "${STUB_AWS_VERSION:-aws-cli/2.17.0 Python/3.12}"; exit 0; fi
while [ $# -gt 0 ]; do
  case "$1" in
    --name) name="$2"; shift 2 ;;
    --value) value="$2"; shift 2 ;;
    --type) type="$2"; shift 2 ;;
    --overwrite) overwrite=yes; shift ;;
    *) shift ;;
  esac
done
printf '%s|%s|%s|%s\n' "$name" "$type" "${overwrite:-no}" "$value" >> "$STUB_LOG"
STUB
chmod +x "$TMP/bin/aws"

check() {
  if [ "$2" = "$3" ]; then echo "PASS $1"; else echo "FAIL $1: expected [$2] got [$3]"; fails=$((fails + 1)); fi
}

export STUB_LOG="$TMP/calls"
cat > "$TMP/env" <<'ENV'
# 주석
DB_URL=jdbc:postgresql://host:5432/db?a=b

JWT_SECRET='p$a"ss#w=rd'
GOOGLE_CLIENT_ID="abc def"
ENV
printf 'ADMIN_EMAIL=me@example.com' >> "$TMP/env"

output="$(PATH="$TMP/bin:$PATH" bash "$ROOT/scripts/ssm-put-env.sh" "$TMP/env" 2>&1)"
check "exit 0" "0" "$?"
check "등록 개수" "4" "$(wc -l < "$STUB_LOG" | tr -d ' ')"
check "= 가 있는 값 유지" "/typing-practice/prod/DB_URL|SecureString|yes|jdbc:postgresql://host:5432/db?a=b" "$(sed -n 1p "$STUB_LOG")"
check "작은따옴표 제거, 특수문자 유지" "/typing-practice/prod/JWT_SECRET|SecureString|yes|p\$a\"ss#w=rd" "$(sed -n 2p "$STUB_LOG")"
check "큰따옴표 제거" "/typing-practice/prod/GOOGLE_CLIENT_ID|SecureString|yes|abc def" "$(sed -n 3p "$STUB_LOG")"
check "끝 줄바꿈 없는 줄도 등록" "/typing-practice/prod/ADMIN_EMAIL|SecureString|yes|me@example.com" "$(sed -n 4p "$STUB_LOG")"
# shellcheck disable=SC2016
case "$output" in
  *'p$a'*|*jdbc*|*me@example*) check "값을 출력하지 않음" "no" "yes" ;;
  *) check "값을 출력하지 않음" "no" "no" ;;
esac

: > "$STUB_LOG"
STUB_AWS_VERSION="aws-cli/1.32.0 Python/3.9" PATH="$TMP/bin:$PATH" bash "$ROOT/scripts/ssm-put-env.sh" "$TMP/env" > /dev/null 2>&1
check "CLI v1 거부" "1" "$?"
check "CLI v1 은 등록 없음" "0" "$(wc -l < "$STUB_LOG" | tr -d ' ')"

PATH="$TMP/bin:$PATH" bash "$ROOT/scripts/ssm-put-env.sh" "$TMP/none" > /dev/null 2>&1
check "파일 없으면 실패" "1" "$?"

if [ "$fails" -ne 0 ]; then
  echo "$fails FAILED"
  exit 1
fi
echo "ALL PASS"
