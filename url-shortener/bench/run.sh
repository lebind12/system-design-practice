#!/usr/bin/env bash
# url-shortener 벤치 러너 — 한 번의 실행으로 A(고정 부하 비교) 와 B(포화 곡선) 를 동시에 채운다.
#
# 사용:
#   VARIANT=mvp ./bench/run.sh
#   VARIANT=cache CONCURRENCIES="50 100" ./bench/run.sh
#
# 출력:
#   bench-results/c<N>/<VARIANT>_shorten.txt
#   bench-results/c<N>/<VARIANT>_redirect.txt
#
# - 같은 동시성 디렉토리에 여러 VARIANT 가 같이 쌓이면 summarize.py 가 자동으로
#   "scenario × concurrency" 그룹화 차트를 만들어 A 와 B 가 동시에 시각화된다.
# - 매 실행 전에 이전 같은 VARIANT 의 결과 파일을 덮어쓴다 (다른 VARIANT 는 보존).

set -euo pipefail
cd "$(dirname "$0")/.."

command -v hey >/dev/null || { echo "hey not installed. brew install hey" >&2; exit 1; }

VARIANT="${VARIANT:-mvp}"
CONCURRENCIES="${CONCURRENCIES:-10 50 100 200 500}"
WARMUP="${WARMUP:-5s}"
DURATION="${DURATION:-20s}"
APP_URL="${APP_URL:-http://localhost:28080}"

# 기동 확인
if ! curl -sf "${APP_URL}/health" >/dev/null; then
  echo "app not reachable at ${APP_URL}/health — make up 먼저 실행" >&2
  exit 1
fi

# redirect 시나리오를 위한 키 한 개 사전 발급 (모든 동시성에서 동일 키 사용)
echo "== preparing redirect target =="
SHORTEN_BODY='{"longUrl":"https://example.com/bench-target/long/path?q=42&trace=bench"}'
REDIRECT_KEY=$(curl -sf -X POST "${APP_URL}/shorten" \
  -H 'Content-Type: application/json' \
  -d "${SHORTEN_BODY}" | sed -E 's/.*"shortKey":"([^"]+)".*/\1/')
if [[ -z "${REDIRECT_KEY}" || "${REDIRECT_KEY}" == *"{"* ]]; then
  echo "failed to obtain redirect key (got: ${REDIRECT_KEY})" >&2
  exit 1
fi
echo "  redirect target: ${APP_URL}/${REDIRECT_KEY}"

BODY_FILE="$(mktemp)"
trap 'rm -f "${BODY_FILE}"' EXIT
printf '%s' "${SHORTEN_BODY}" > "${BODY_FILE}"

run_one() {
  local name="$1" url="$2" method="$3"
  local out_file="bench-results/c${C}/${VARIANT}_${name}.txt"
  echo "  -> ${name} (${method}) c=${C} dur=${DURATION}"
  if [[ "${method}" == "POST" ]]; then
    hey -z "${WARMUP}" -c "${C}" -m POST -H 'Content-Type: application/json' -D "${BODY_FILE}" "${url}" > /dev/null
    hey -z "${DURATION}" -c "${C}" -m POST -H 'Content-Type: application/json' -D "${BODY_FILE}" "${url}" | tee "${out_file}" > /dev/null
  else
    # 302 응답을 따라가지 않게 -disable-redirects 사용 → 순수 서버 처리 시간만 측정
    hey -z "${WARMUP}" -c "${C}" -disable-redirects "${url}" > /dev/null
    hey -z "${DURATION}" -c "${C}" -disable-redirects "${url}" | tee "${out_file}" > /dev/null
  fi
}

for C in ${CONCURRENCIES}; do
  mkdir -p "bench-results/c${C}"
  echo "== c=${C} =="
  set +e
  run_one shorten  "${APP_URL}/shorten"           POST
  run_one redirect "${APP_URL}/${REDIRECT_KEY}"   GET
  set -e
done

echo
echo "== summarize =="
python3 bench/summarize.py || echo "(viz skipped)"
