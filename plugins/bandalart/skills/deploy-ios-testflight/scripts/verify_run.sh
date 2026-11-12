#!/usr/bin/env bash
# 완료된 Release CD run이 승인한 조건으로 iOS TestFlight 업로드를 검증했는지 read-only로 확인한다.
# 사용: plugins/bandalart/skills/deploy-ios-testflight/scripts/verify_run.sh <run-id> <production|test> <approved-sha>
# 모든 조건을 만족할 때만 exit 0. 조회 실패나 조건 불일치는 exit 1, 인자 오류는 exit 2.
set -uo pipefail

REPO="Nexters/BandalArt-KMP"
run_id="${1:-}"
expected_ads_mode="${2:-}"
approved_sha="${3:-}"
usage() { echo "사용: $0 <run-id> <production|test> <approved-sha>" >&2; exit 2; }
printf '%s' "$run_id" | grep -qE '^[0-9]+$' || usage
case "$expected_ads_mode" in production|test) ;; *) usage ;; esac
printf '%s' "$approved_sha" | grep -qE '^[0-9a-f]{7,40}$' || usage

failures=0
fail() { printf '[FAIL] %s\n' "$1"; failures=$((failures + 1)); }

run_json="$(gh run view "$run_id" -R "$REPO" --json status,headSha,headBranch,event,url,jobs 2>/dev/null)" \
  || { echo "[FAIL] run $run_id 조회 실패"; exit 1; }
field() { printf '%s' "$run_json" | jq -r "$1"; }

if [ "$(field .status)" != "completed" ]; then
  echo "[FAIL] run $run_id 상태: $(field .status). 완료 후 다시 실행한다."
  exit 1
fi

head_sha="$(field .headSha)"
echo "[INFO] source: $(field .headBranch) ${head_sha:0:8} ($(field .event))"
echo "[INFO] url: $(field .url)"
printf '%s' "$run_json" | jq -r '.jobs[] | "[INFO] job: \(.name) = \(.conclusion)"'

case "$head_sha" in
  "$approved_sha"*) echo "[PASS] source SHA가 승인값과 같다." ;;
  *) fail "source SHA 불일치: 승인 $approved_sha, run $head_sha" ;;
esac

android="$(field '[.jobs[] | select(.name == "Play Internal Testing") | .conclusion] | first // "missing"')"
if [ "$android" = "skipped" ]; then
  echo "[PASS] Play Internal Testing job skipped"
else
  fail "Play Internal Testing job이 skipped가 아니다 ($android). target=ios 실행이 아니다."
fi

testflight="$(field '[.jobs[] | select(.name == "TestFlight") | .conclusion] | first // "missing"')"
if [ "$testflight" != "success" ]; then
  fail "TestFlight job: $testflight"
  echo "실패 단계는 gh run view $run_id -R $REPO --log-failed 로 확인하고 secret 값은 인용하지 않는다."
  exit 1
fi

log_tmp="$(mktemp)"
trap 'rm -f "$log_tmp"' EXIT
if ! gh run view "$run_id" -R "$REPO" --log > "$log_tmp" 2>/dev/null; then
  echo "[FAIL] run 로그 조회 실패"
  exit 1
fi
verified="$(grep -oE 'TestFlight upload verified: [0-9.]+ \([0-9]+\)' "$log_tmp" | tail -1)"
ads_mode="$(grep -oE 'IOS_ADS_MODE=(test|production)' "$log_tmp" | tail -1)"
if [ -n "$verified" ]; then
  echo "[PASS] $verified"
else
  fail "로그에서 'TestFlight upload verified' 줄을 찾지 못했다."
fi
if [ "$ads_mode" = "IOS_ADS_MODE=$expected_ads_mode" ]; then
  echo "[PASS] $ads_mode"
else
  fail "광고 모드 불일치: 기대 $expected_ads_mode, 로그 ${ads_mode:-없음}"
fi

[ "$failures" -eq 0 ] && { echo "RESULT: PASS"; exit 0; }
echo "RESULT: FAIL ($failures 항목)"
exit 1
