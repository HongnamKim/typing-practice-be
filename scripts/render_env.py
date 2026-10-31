#!/usr/bin/env python3
"""ssm get-parameters-by-path 의 JSON 을 읽어 compose env 파일을 출력한다. 값은 오류에도 남기지 않는다."""
import json
import sys

REQUIRED_KEYS = [
    "DB_URL", "DB_USERNAME", "DB_PASSWORD", "MONGODB_URI", "JWT_SECRET",
    "GOOGLE_CLIENT_ID", "GOOGLE_CLIENT_SECRET", "GOOGLE_REDIRECT_URI", "ADMIN_EMAIL",
]


def main():
    try:
        parameters = json.load(sys.stdin)["Parameters"]
    except (json.JSONDecodeError, KeyError, TypeError):
        print("SSM 응답을 읽을 수 없습니다.", file=sys.stderr)
        return 1

    values = {p["Name"].rsplit("/", 1)[-1]: p["Value"] for p in parameters}
    missing = [k for k in REQUIRED_KEYS if not values.get(k)]
    if missing:
        print("값이 없는 키: " + ", ".join(missing), file=sys.stderr)
        return 1

    multiline = [k for k, v in values.items() if "\n" in v]
    if multiline:
        print("줄바꿈이 있는 값은 쓸 수 없습니다: " + ", ".join(multiline), file=sys.stderr)
        return 1

    # compose 의 작은따옴표 값은 \\ 를 접지 않아 이 두 형태를 표현할 수 없다. 파일이 깨지면 오류에 다른 값이 찍힌다.
    unquotable = [k for k, v in values.items() if "\\'" in v or v.endswith("\\")]
    if unquotable:
        print("백슬래시 위치 때문에 쓸 수 없는 값: " + ", ".join(unquotable), file=sys.stderr)
        return 1

    for key in sorted(values):
        # compose 의 작은따옴표 값은 $ 를 변수로 해석하지 않는다.
        print(f"{key}='" + values[key].replace("'", "\\'") + "'")
    return 0


if __name__ == "__main__":
    sys.exit(main())
