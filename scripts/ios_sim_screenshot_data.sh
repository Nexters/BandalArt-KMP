#!/usr/bin/env bash
# iOS 시뮬레이터의 Room DB를 직접 수정해 스토어 스크린샷·QA용 상태를 만든다.
#
# usage:
#   scripts/ios_sim_screenshot_data.sh [-d <udid>] seed                # 샘플 반다라트 2개 채우기
#   scripts/ios_sim_screenshot_data.sh [-d <udid>] complete <id>       # 반다라트 전체 완료 → 재실행 시 완료 화면 진입
#   scripts/ios_sim_screenshot_data.sh [-d <udid>] backup | restore    # DB 백업/복원
#   scripts/ios_sim_screenshot_data.sh [-d <udid>] shot <inner|outer> <name> [out_dir]
#
# -d를 생략하면 부팅된 첫 시뮬레이터를 사용한다.
# 앱을 종료한 뒤 DB를 수정하고 다시 실행한다. Room은 프로세스 밖의 변경을 감지하지 못하기 때문이다.
# complete는 앱이 저장해 둔 이전 완료 상태(미완료)와 DB의 완료 상태를 비교해 완료 화면을 띄우는 동작을 이용한다.
# 따라서 complete 전에 앱이 해당 반다라트를 미완료 상태로 한 번 이상 로드한 적이 있어야 한다.
# seed 직후라면 홈이 다 뜰 때까지(Debug 빌드 기준 30~60초) 기다린 뒤 complete를 실행한다.
# 완료 상태를 되돌리려면 seed를 다시 실행한다.

set -euo pipefail

BUNDLE_ID="com.nexters.bandalart.iosApp"
APP_GROUP="group.com.nexters.bandalart"
UDID=""

if [[ "${1:-}" == "-d" ]]; then
  UDID="$2"
  shift 2
fi
if [[ -z "$UDID" ]]; then
  UDID=$(xcrun simctl list devices booted | grep -oE '[0-9A-F-]{36}' | head -1)
fi
[[ -n "$UDID" ]] || { echo "부팅된 시뮬레이터가 없습니다." >&2; exit 1; }

db_path() {
  local group_dir
  group_dir=$(xcrun simctl get_app_container "$UDID" "$BUNDLE_ID" groups | awk -v g="$APP_GROUP" '$1 == g { $1 = ""; sub(/^ /, ""); print }')
  echo "$group_dir/bandalart.db"
}

stop_app() { xcrun simctl terminate "$UDID" "$BUNDLE_ID" >/dev/null 2>&1 || true; sleep 1; }
start_app() { xcrun simctl launch "$UDID" "$BUNDLE_ID" >/dev/null; }

run_sql() {
  local db
  db=$(db_path)
  stop_app
  sqlite3 "$db" "$1"
  sqlite3 "$db" "PRAGMA wal_checkpoint(TRUNCATE);" >/dev/null
  sqlite3 "$db" "SELECT id, title, completionRatio, isCompleted FROM bandalarts;"
  start_app
}

seed_sql() {
  cat <<'SQL'
BEGIN;
-- 반다라트 1: 2027 갓생 살기 (완료 10셀 → 40%)
UPDATE bandalarts SET profileEmoji = '🔥', title = '2027 갓생 살기', dueDate = '2027-12-31T23:59:00' WHERE id = 1;
UPDATE bandalart_cells SET title = '2027 갓생 살기', description = '매일 조금씩 나아지는 한 해', dueDate = '2027-12-31T23:59:00' WHERE id = 1;
UPDATE bandalart_cells SET title = CASE id
  WHEN 2 THEN '건강' WHEN 3 THEN '헬스장 주 3회' WHEN 4 THEN '하루 물 2L' WHEN 5 THEN '12시 전에 취침'
  WHEN 6 THEN '매일 아침 스트레칭' WHEN 7 THEN '마라톤 10km 완주'
  WHEN 8 THEN '성장' WHEN 9 THEN '한 달에 책 2권' WHEN 10 THEN '영어 회화 수업' WHEN 11 THEN '사이드 프로젝트'
  WHEN 12 THEN '뉴스레터 읽기' WHEN 13 THEN '블로그 주 1회'
  WHEN 14 THEN '재테크' WHEN 15 THEN '적금 1000만원' WHEN 16 THEN '가계부 쓰기' WHEN 17 THEN '배달 월 2회 이하'
  WHEN 18 THEN '고정 지출 줄이기' WHEN 19 THEN '경제 공부'
  WHEN 20 THEN '관계' WHEN 21 THEN '부모님께 자주 전화' WHEN 22 THEN '가족 여행 가기' WHEN 23 THEN '친구와 월 1회 만남'
  WHEN 24 THEN '감사 일기 쓰기' WHEN 25 THEN '화내지 않기'
  ELSE title END
WHERE bandalartId = 1 AND id BETWEEN 2 AND 25;
UPDATE bandalart_cells SET isCompleted = CASE WHEN id IN (2, 3, 4, 5, 6, 7, 9, 12, 16, 21) THEN 1 ELSE 0 END WHERE bandalartId = 1;
UPDATE bandalarts SET completionRatio = 40, isCompleted = 0 WHERE id = 1;
SQL
  # 반다라트 2가 없으면 앱에서 '취업 준비' 템플릿으로 먼저 만든다.
  cat <<'SQL'
UPDATE bandalarts SET dueDate = '2027-03-31T23:59:00' WHERE id = 2;
UPDATE bandalart_cells SET dueDate = '2027-03-31T23:59:00', description = '상반기 공채 합격하기' WHERE id = 26;
UPDATE bandalart_cells SET title = CASE id
  WHEN 31 THEN '채용 공고 매일 확인' WHEN 32 THEN '현직자 커피챗' WHEN 37 THEN '자격증 서류 정리' WHEN 38 THEN '첨삭 받기'
  WHEN 43 THEN '코딩 테스트 주 3회' WHEN 44 THEN '스터디 참여' WHEN 49 THEN '1분 자기소개' WHEN 50 THEN '면접 복장 준비'
  ELSE title END
WHERE bandalartId = 2;
UPDATE bandalart_cells SET isCompleted = CASE WHEN id IN (27, 28, 29, 30, 31, 32, 34, 40, 46) THEN 1 ELSE 0 END WHERE bandalartId = 2;
UPDATE bandalarts SET completionRatio = 36, isCompleted = 0 WHERE id = 2;
COMMIT;
SQL
}

# Duo처럼 화면이 둘인 기기는 enumerate 결과에서 내부 디스플레이(class 0)를 면적순으로 고른다.
display_id() {
  local which="$1"
  xcrun simctl io "$UDID" enumerate | awk '
    /UUID:/ { uuid = $2 }
    /Display class: 0/ { main = 1 }
    /Default width:/ { w = $3 }
    /Default height:/ { if (main) print w * $3, uuid; main = 0 }
  ' | sort -n | { if [[ "$which" == "inner" ]]; then tail -1; else head -1; fi; } | awk '{ print $2 }'
}

cmd="${1:-}"
case "$cmd" in
  seed)
    run_sql "$(seed_sql)"
    ;;
  complete)
    id="${2:?반다라트 id가 필요합니다}"
    run_sql "BEGIN; UPDATE bandalart_cells SET isCompleted = 1 WHERE bandalartId = $id; UPDATE bandalarts SET isCompleted = 1, completionRatio = 100 WHERE id = $id; COMMIT;"
    ;;
  backup)
    db=$(db_path); stop_app
    sqlite3 "$db" ".backup '${db}.screenshot-backup'"
    echo "백업: ${db}.screenshot-backup"
    start_app
    ;;
  restore)
    db=$(db_path); stop_app
    sqlite3 "$db" ".restore '${db}.screenshot-backup'"
    echo "복원 완료"
    start_app
    ;;
  shot)
    which="${2:?inner 또는 outer}"; name="${3:?파일 이름이 필요합니다}"
    out_dir="${4:-store-assets/screenshots/source/ios/duo/$which}"
    mkdir -p "$out_dir"
    xcrun simctl io "$UDID" screenshot --display="$(display_id "$which")" "$out_dir/$name.png" >/dev/null
    sips -g pixelWidth -g pixelHeight "$out_dir/$name.png" | tail -2 | tr -s ' \n' ' '; echo
    ;;
  *)
    sed -n '2,16p' "$0" | sed 's/^# \{0,1\}//'
    exit 1
    ;;
esac
