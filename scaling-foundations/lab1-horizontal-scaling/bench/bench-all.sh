#!/usr/bin/env bash
# 4단계를 순회하며 동일 부하를 돌려 결과를 bench-results/c<동시성>/ 에 저장한다.
# 기본 동시성은 50. CONCURRENCY 환경 변수로 덮어쓸 수 있다.
#
#   bash bench-all.sh                    # c=50
#   CONCURRENCY=500 bash bench-all.sh    # c=500
#   CONCURRENCY=1000 bash bench-all.sh   # c=1000

set -euo pipefail

cd "$(dirname "$0")/../.."

if ! command -v hey >/dev/null 2>&1; then
  echo "hey not found. install via: brew install hey" >&2
  exit 1
fi

C="${CONCURRENCY:-50}"
OUT_DIR="bench-results/c${C}"
mkdir -p "$OUT_DIR"

run_one() {
  local stage="$1"
  local url="$2"
  echo
  echo "============================================================"
  echo "stage${stage}  ->  ${url}  (c=${C})"
  echo "============================================================"
  make stage${stage}
  echo ">> warmup 5s ..."
  hey -z 5s -c "${C}" "${url}/api/items/seed-1" > /dev/null
  echo ">> measure 30s @ c=${C} ..."
  hey -z 30s -c "${C}" "${url}/api/items/seed-1" | tee "${OUT_DIR}/stage${stage}.txt"
}

# 각 단계는 독립적으로 실행해야 후속 단계가 전 단계의 실패에 영향받지 않는다.
# (예: stage3 에서 nginx upstream 이 터져도 stage4 벤치는 돌아야 한다.)
# 따라서 이 블록만 set -e 를 일시 해제한다.
set +e
run_one 1 http://localhost:18081
run_one 2 http://localhost:18081
run_one 3 http://localhost:18080
run_one 4 http://localhost:18080
set -e

make down
echo
echo ">> 4단계 결과는 ${OUT_DIR}/ 에 저장되었습니다."
echo ">> 시각화 생성 ..."
python3 lab1-horizontal-scaling/bench/summarize.py || echo "(viz skipped)"
