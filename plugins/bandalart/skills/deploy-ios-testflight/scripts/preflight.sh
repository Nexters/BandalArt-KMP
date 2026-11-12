#!/usr/bin/env bash
# BandalArt iOS TestFlight 배포 전 read-only 점검.
# 원격 상태를 바꾸지 않는다. secret은 이름만 확인하고 값은 읽지 않는다.
# 사용: plugins/bandalart/skills/deploy-ios-testflight/scripts/preflight.sh
set -uo pipefail

REPO="Nexters/BandalArt-KMP"
EXPECTED_LOGIN="easyhooon"
WORKFLOW="release-cd.yml"
ENVIRONMENT="ios-testflight"
ENV_SECRETS=(
  APP_STORE_CONNECT_KEY_ID
  APP_STORE_CONNECT_KEY_P8_BASE64
  IOS_DISTRIBUTION_CERTIFICATE_BASE64
  IOS_DISTRIBUTION_CERTIFICATE_PASSWORD
  IOS_PROVISIONING_PROFILE_BASE64
  IOS_WIDGET_PROVISIONING_PROFILE_BASE64
)
REPO_SECRETS=(
  BANDALART_SUPABASE_URL
  BANDALART_SUPABASE_PUBLISHABLE_KEY
)
WORKFLOW_INPUTS=(target confirm_release ios_ads_mode android_update_priority)

failures=0
pass() { printf '[PASS] %s\n' "$1"; }
fail() { printf '[FAIL] %s\n' "$1"; failures=$((failures + 1)); }
info() { printf '[INFO] %s\n' "$1"; }

repo_root="$(git rev-parse --show-toplevel 2>/dev/null)" || { echo "git 저장소 안에서 실행해야 한다." >&2; exit 2; }
cd "$repo_root" || exit 2

# 1. GitHub 계정
login="$(gh api user --jq .login 2>/dev/null || true)"
if [ "$login" = "$EXPECTED_LOGIN" ]; then
  pass "gh 활성 계정: $login"
else
  fail "gh 활성 계정이 $EXPECTED_LOGIN 이 아니다 (현재: ${login:-없음}). gh auth switch --hostname github.com --user $EXPECTED_LOGIN 후 다시 실행한다."
fi

# 2. 배포 source: workflow는 항상 origin/main 최신 commit을 빌드한다.
origin_url="$(git remote get-url origin 2>/dev/null || true)"
case "$origin_url" in
  *"$REPO"*) pass "origin remote: $REPO" ;;
  *) fail "origin remote가 $REPO 가 아니다: ${origin_url:-없음}" ;;
esac
if git fetch --quiet --no-tags origin main; then
  main_sha="$(git rev-parse origin/main)"
  pass "origin/main: $(git log -1 --format='%h %s' origin/main)"
else
  fail "origin main fetch 실패"
  main_sha=""
fi

# 3. 버전과 What to Test (origin/main 기준. 로컬 working tree는 빌드에 쓰이지 않는다.)
if [ -n "$main_sha" ]; then
  plist_tmp="$(mktemp)"
  git show "origin/main:iosApp/iosApp/Info.plist" > "$plist_tmp"
  marketing_version="$(/usr/libexec/PlistBuddy -c 'Print :CFBundleShortVersionString' "$plist_tmp" 2>/dev/null || true)"
  rm -f "$plist_tmp"
  if [ -n "$marketing_version" ]; then
    pass "marketing version (CFBundleShortVersionString): $marketing_version"
    info "build number는 lane이 App Store Connect의 같은 version 최신 build + 1로 runner에서 정한다."
  else
    fail "origin/main Info.plist에서 CFBundleShortVersionString을 읽지 못했다."
  fi

  for name in what_to_test what_to_test_production_ads what_to_test_test_ads; do
    path="fastlane/metadata/ios/$name.txt"
    content="$(git show "origin/main:$path" 2>/dev/null || true)"
    if [ -n "$(printf '%s' "$content" | tr -d '[:space:]')" ]; then
      pass "$path ($(printf '%s' "$content" | wc -m | tr -d ' ')자)"
    else
      fail "$path 가 origin/main에 없거나 비어 있다."
    fi
  done
  last_change="$(git log -1 --format='%h %ad %s' --date=short origin/main -- fastlane/metadata/ios/what_to_test.txt)"
  info "what_to_test.txt 마지막 변경: $last_change"
  echo "----- what_to_test.txt (origin/main) -----"
  git show origin/main:fastlane/metadata/ios/what_to_test.txt
  echo "------------------------------------------"
fi

# 4. workflow 입력 계약
workflow_yaml="$(gh workflow view "$WORKFLOW" -R "$REPO" --yaml 2>/dev/null || true)"
if [ -z "$workflow_yaml" ]; then
  fail "$WORKFLOW 를 $REPO 에서 읽지 못했다."
else
  for input in "${WORKFLOW_INPUTS[@]}"; do
    if printf '%s\n' "$workflow_yaml" | grep -qE "^[[:space:]]+$input:[[:space:]]*$"; then
      pass "workflow input: $input"
    else
      fail "workflow input 없음: $input (입력 계약이 바뀌었으면 SKILL.md 명령을 갱신해야 한다)"
    fi
  done
fi

# 5. secret 이름 (값은 조회하지 않는다)
env_secret_names="$(gh secret list --env "$ENVIRONMENT" -R "$REPO" 2>/dev/null | awk '{print $1}')"
for secret in "${ENV_SECRETS[@]}"; do
  if printf '%s\n' "$env_secret_names" | grep -qx "$secret"; then
    pass "$ENVIRONMENT secret: $secret"
  else
    fail "$ENVIRONMENT secret 없음: $secret"
  fi
done
repo_secret_names="$(gh secret list -R "$REPO" 2>/dev/null | awk '{print $1}')"
for secret in "${REPO_SECRETS[@]}"; do
  if printf '%s\n' "$repo_secret_names" | grep -qx "$secret"; then
    pass "repository secret: $secret"
  else
    fail "repository secret 없음: $secret"
  fi
done

# 6. 진행 중인 배포 (release-cd concurrency group은 Play 스크린샷 workflow와 공유한다)
for wf in release-cd.yml play-store-screenshots.yml; do
  active="$(gh run list --workflow "$wf" -R "$REPO" --limit 20 --json databaseId,status \
    --jq '[.[] | select(.status != "completed")] | length' 2>/dev/null || echo "?")"
  if [ "$active" = "0" ]; then
    pass "$wf 진행 중 run 없음"
  else
    fail "$wf 진행 중 또는 확인 불가 run: $active"
  fi
done

# 7. origin/main HEAD의 check 상태 (없으면 merge된 PR의 CI 결과를 SKILL.md 절차로 확인한다)
if [ -n "$main_sha" ]; then
  if checks_json="$(gh api "repos/$REPO/commits/$main_sha/check-runs" 2>/dev/null)"; then
    checks="$(printf '%s' "$checks_json" | jq -r '[.check_runs[] | .conclusion // .status] | group_by(.) | map("\(.[0])=\(length)") | join(", ")')"
    not_green="$(printf '%s' "$checks_json" | jq -r '[.check_runs[] | select((.conclusion // .status) | IN("success", "skipped", "neutral") | not)] | length')"
  else
    checks=""
    not_green="조회 실패"
  fi
  info "origin/main HEAD check-runs: ${checks:-없음}"
  if [ -z "$checks" ] || [ "$not_green" != "0" ]; then
    printf '[WARN] CHECKS_NOT_GREEN: HEAD check-run이 없거나 success가 아닌 항목이 있다(%s). SKILL.md 3단계대로 병합 PR의 CI를 확인한다.\n' "$not_green"
  fi
fi

# 8. 직전 성공 TestFlight 배포 이후 변경 범위
last_ios_sha=""
for run_id in $(gh run list --workflow "$WORKFLOW" -R "$REPO" --limit 30 --json databaseId --jq '.[].databaseId' 2>/dev/null); do
  testflight="$(gh run view "$run_id" -R "$REPO" --json jobs,headSha \
    --jq 'select(any(.jobs[]; .name == "TestFlight" and .conclusion == "success")) | .headSha' 2>/dev/null || true)"
  if [ -n "$testflight" ]; then
    last_ios_sha="$testflight"
    info "직전 성공 TestFlight run: $run_id ($(git log -1 --format='%h %ad' --date=short "$last_ios_sha" 2>/dev/null || echo "$last_ios_sha"))"
    break
  fi
done
if [ -z "$last_ios_sha" ]; then
  echo "[WARN] NO_PREVIOUS_TESTFLIGHT: 최근 run 30건에서 TestFlight 성공 run을 찾지 못했다(조회 실패 가능). 변경 범위를 수동으로 확인한다."
fi
if [ -n "$last_ios_sha" ] && [ -n "$main_sha" ]; then
  new_commits="$(git log --oneline --first-parent "$last_ios_sha..origin/main" 2>/dev/null)"
  if [ -z "$new_commits" ]; then
    info "NO_NEW_COMMITS: origin/main이 직전 성공 TestFlight source와 같다. 재업로드 의도를 사용자에게 확인한다."
  else
    echo "----- 직전 TestFlight 이후 origin/main first-parent commit -----"
    printf '%s\n' "$new_commits" | head -40
    echo "-----------------------------------------------------------------"
  fi
fi

echo
if [ "$failures" -eq 0 ]; then
  echo "RESULT: PASS (자동 점검 통과. App Store Connect version 상태, What to Test 내용, 광고 모드는 SKILL.md 절차로 사용자에게 확인한다.)"
  exit 0
fi
echo "RESULT: FAIL ($failures 항목). 배포를 진행하지 않는다."
exit 1
