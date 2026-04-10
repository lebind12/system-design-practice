#!/usr/bin/env bash
# 보조 실험: HikariCP 어드미션 컨트롤 가설 검증
#
#   가설 (CONTEXT.md 섹션 9 참조): c=1000 에서 stage3 이 stage4 보다 더 빠른 이유는
#   HikariCP 풀 (20 × 2 replica) 이 "어드미션 컨트롤" 로 작동하기 때문이다.
#   풀 크기를 키우면 보호 효과가 약해져 p95 가 오히려 악화될 것이다.
#
#   이 스크립트는 stage2/3/4 를 HIKARI_POOL_SIZE=100 로 c=1000 재벤치하고
#   결과를 bench-results/c1000-tuned/ 에 저장한다. stage1 은 DB 가 없어
#   제외. 기존 bench-results/c1000/ 는 건드리지 않음.
#
#   비교 포인트 (기대):
#     stage2 (단일 replica): 풀 20 → 100 으로 확장 시 RPS 상승, p95 의 형태 변화
#     stage3 (LB+복제): 풀 확장 시 **p95 가 악화** (보호 약화). RPS 는 미세 상승/동일
#     stage4 (+redis): 이미 Redis 경로가 주여서 stage3 만큼 변화 크지 않을 것
#
# 사용:
#   bash bench-tuned.sh
#   CONCURRENCY=1000 bash bench-tuned.sh  # 기본값 1000

set -euo pipefail

cd "$(dirname "$0")/../.."

if ! command -v hey >/dev/null 2>&1; then
  echo "hey not found. install via: brew install hey" >&2
  exit 1
fi

C="${CONCURRENCY:-1000}"
POOL="${HIKARI_POOL_SIZE:-100}"
OUT_DIR="bench-results/c${C}-tuned"
mkdir -p "$OUT_DIR"

echo "============================================================"
echo "HikariCP 튜닝 실험"
echo "  concurrency  : ${C}"
echo "  pool size    : ${POOL}  (기본값 20 대비 ${POOL} 배)"
echo "  저장 경로    : ${OUT_DIR}/"
echo "  비교 대상    : bench-results/c${C}/stage2.txt, stage3.txt, stage4.txt"
echo "============================================================"

run_one() {
  local stage="$1"
  local url="$2"
  echo
  echo "-- stage${stage} @ c=${C}, pool=${POOL} --"
  HIKARI_POOL_SIZE="${POOL}" make stage${stage}
  echo ">> warmup 5s ..."
  hey -z 5s -c "${C}" "${url}/api/items/seed-1" > /dev/null
  echo ">> measure 30s @ c=${C} ..."
  hey -z 30s -c "${C}" "${url}/api/items/seed-1" \
    | tee "${OUT_DIR}/stage${stage}.txt"
}

# stage1 제외 (DB 없음 → 풀 관련 변수 영향 받지 않음)
set +e
run_one 2 http://localhost:18081
run_one 3 http://localhost:18080
run_one 4 http://localhost:18080
set -e

make down
echo
echo ">> 결과: ${OUT_DIR}/"
echo ">> 시각화 생성 ..."
python3 lab1-horizontal-scaling/bench/summarize.py || echo "(viz skipped)"
