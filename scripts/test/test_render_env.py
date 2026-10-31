#!/usr/bin/env python3
"""render_env.py 테스트. 만든 파일을 실제 compose 로 읽어 앱이 받는 값을 확인한다."""
import json
import pathlib
import subprocess
import sys
import tempfile

ROOT = pathlib.Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "scripts" / "render_env.py"
KEYS = [
    "DB_URL", "DB_USERNAME", "DB_PASSWORD", "MONGODB_URI", "JWT_SECRET",
    "GOOGLE_CLIENT_ID", "GOOGLE_CLIENT_SECRET", "GOOGLE_REDIRECT_URI", "ADMIN_EMAIL",
]
SPECIAL = "p$a'ss#w=rd\"\\x ${HOME} `id`"


def render(values):
    payload = {"Parameters": [{"Name": f"/typing-practice/prod/{k}", "Value": v} for k, v in values.items()]}
    return subprocess.run(
        [sys.executable, str(SCRIPT)], input=json.dumps(payload), capture_output=True, text=True
    )


def test_missing_key_fails():
    values = {k: "x" for k in KEYS[:-1]}
    result = render(values)
    assert result.returncode == 1, result
    assert result.stdout == "", "실패하면 아무것도 출력하지 않아야 한다"
    assert "ADMIN_EMAIL" in result.stderr


def test_empty_value_fails():
    values = {k: "x" for k in KEYS}
    values["JWT_SECRET"] = ""
    result = render(values)
    assert result.returncode == 1, result
    assert result.stdout == ""


def test_error_does_not_print_values():
    values = {k: "supersecret" for k in KEYS[:-1]}
    result = render(values)
    assert "supersecret" not in result.stdout + result.stderr


def test_backslash_before_quote_is_rejected():
    values = {k: "x" for k in KEYS}
    values["JWT_SECRET"] = "abc\\'supersecret"
    result = render(values)
    assert result.returncode == 1, result
    assert result.stdout == ""
    assert "JWT_SECRET" in result.stderr
    assert "supersecret" not in result.stderr


def test_trailing_backslash_is_rejected():
    values = {k: "x" for k in KEYS}
    values["JWT_SECRET"] = "supersecret\\"
    result = render(values)
    assert result.returncode == 1, result
    assert result.stdout == ""
    assert "supersecret" not in result.stderr


def test_special_characters_round_trip():
    values = {k: "x" for k in KEYS}
    values["JWT_SECRET"] = SPECIAL
    result = render(values)
    assert result.returncode == 0, result

    with tempfile.TemporaryDirectory() as tmp:
        path = pathlib.Path(tmp)
        (path / ".env.prod").write_text(result.stdout)
        (path / "compose.yaml").write_text(
            "services:\n  t:\n    image: alpine:3.20\n    env_file: [.env.prod]\n"
        )
        got = subprocess.run(
            ["docker", "compose", "-f", str(path / "compose.yaml"), "run", "--rm", "-T", "t",
             "printenv", "JWT_SECRET"],
            capture_output=True, text=True,
        )
        assert got.returncode == 0, got.stderr
        assert got.stdout.rstrip("\n") == SPECIAL, repr(got.stdout)


for name, fn in list(globals().items()):
    if name.startswith("test_"):
        fn()
        print("PASS", name)
print("ALL PASS")
