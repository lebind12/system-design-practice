#!/usr/bin/env bash
# 현재 떠 있는 stage 에 대해 hey 부하를 1회 돌린다.
# 어느 stage 가 떠 있는지는 docker compose ps 로 추정한다.

set -euo pipefail

if ! command -v hey >/dev/null 2>&1; then
  echo "hey not found. install via: brew install hey" >&2
  exit 1
fi

cd "$(dirname "$0")/../.."

HAS_NGINX=$(docker compose ps --services --status running 2>/dev/null | grep -c '^nginx$' || true)
if [[ "$HAS_NGINX" -ge 1 ]]; then
  URL="http://localhost:18080/api/items/seed-1"
else
  URL="http://localhost:18081/api/items/seed-1"
fi

echo ">> bench target: $URL"
echo ">> warmup 5s ..."
hey -z 5s -c 50 "$URL" > /dev/null

echo ">> measure 30s @ c=50 ..."
hey -z 30s -c 50 "$URL"
