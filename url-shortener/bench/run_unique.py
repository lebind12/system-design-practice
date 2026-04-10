#!/usr/bin/env python3
"""
url-shortener 용 미니 벤치 러너 — 요청마다 고유한 POST body 를 보낸다.

hey 는 -D 로 정적 body 만 지원해서, hash strategy 나 Bloom filter dedup 처럼
"같은 URL 이면 같은 결과가 나오는" 경로를 현실적으로 측정할 수 없다. 이 스크립트는
stdlib 만 사용해 c 개의 스레드로 POST /shorten 을 요청마다 다른 longUrl 로 쏜다.

출력은 hey 와 호환되는 포맷(`Requests/sec`, `N%% in X secs`, `[NNN] M responses`) 으로
bench-results/c<N>/<variant>_<scenario>.txt 에 저장 — summarize.py 가 그대로 파싱.

사용:
  VARIANT=hash-unique python3 bench/run_unique.py
  VARIANT=hash-unique CONCURRENCIES="10 50 100 200 500" python3 bench/run_unique.py
  VARIANT=mvp-unique  KEY_STRATEGY_NOTE=base62 python3 bench/run_unique.py

환경변수:
  VARIANT         (필수) 결과 파일 접두어
  CONCURRENCIES   (기본 "10 50 100 200 500")
  WARMUP_SECS     (기본 5)
  DURATION_SECS   (기본 20)
  APP_URL         (기본 http://localhost:28080)
  REDIRECT_ONLY   (기본 0) — 1 이면 redirect 시나리오만 측정 (cache 워밍 용도)
"""
from __future__ import annotations

import http.client
import json
import os
import statistics
import sys
import threading
import time
import uuid
from pathlib import Path
from urllib.parse import urlparse

APP_URL = os.environ.get("APP_URL", "http://localhost:28080")
VARIANT = os.environ.get("VARIANT", "unique")
CONCURRENCIES = [int(x) for x in os.environ.get("CONCURRENCIES", "10 50 100 200 500").split()]
WARMUP = float(os.environ.get("WARMUP_SECS", "5"))
DURATION = float(os.environ.get("DURATION_SECS", "20"))

_parsed = urlparse(APP_URL)
HOST = _parsed.hostname or "localhost"
PORT = _parsed.port or 80

CHAPTER_ROOT = Path(__file__).resolve().parents[1]
BENCH_ROOT = CHAPTER_ROOT / "bench-results"


def worker_shorten(duration: float, worker_id: int, stop_at: float, stats: list):
    """POST /shorten with a unique longUrl each call."""
    conn = http.client.HTTPConnection(HOST, PORT, timeout=10)
    local_lats = []
    local_status = {}
    n = 0
    seed = uuid.uuid4().hex[:8]
    while time.monotonic() < stop_at:
        body = json.dumps(
            {"longUrl": f"https://bench.example.com/{seed}/w{worker_id}/n{n}?t={time.time_ns()}"}
        )
        t0 = time.monotonic()
        try:
            conn.request(
                "POST", "/shorten", body=body, headers={"Content-Type": "application/json"}
            )
            resp = conn.getresponse()
            resp.read()
            dt = time.monotonic() - t0
            local_lats.append(dt)
            local_status[resp.status] = local_status.get(resp.status, 0) + 1
        except Exception:
            dt = time.monotonic() - t0
            local_lats.append(dt)
            local_status[-1] = local_status.get(-1, 0) + 1
            try:
                conn.close()
            except Exception:
                pass
            conn = http.client.HTTPConnection(HOST, PORT, timeout=10)
        n += 1
    conn.close()
    stats.append((local_lats, local_status))


def run_sweep(concurrency: int, warmup: float, duration: float) -> dict:
    # warmup
    print(f"  warmup {warmup}s...")
    stop_warm = time.monotonic() + warmup
    warm_threads = []
    warm_stats = []
    for i in range(concurrency):
        t = threading.Thread(target=worker_shorten, args=(warmup, i, stop_warm, warm_stats))
        t.start()
        warm_threads.append(t)
    for t in warm_threads:
        t.join()

    # measure
    print(f"  measure {duration}s...")
    stop_at = time.monotonic() + duration
    threads = []
    stats = []
    t_start = time.monotonic()
    for i in range(concurrency):
        t = threading.Thread(target=worker_shorten, args=(duration, i, stop_at, stats))
        t.start()
        threads.append(t)
    for t in threads:
        t.join()
    total_secs = time.monotonic() - t_start

    all_lats = []
    agg_status: dict = {}
    for lats, statuses in stats:
        all_lats.extend(lats)
        for k, v in statuses.items():
            agg_status[k] = agg_status.get(k, 0) + v

    return {
        "total_secs": total_secs,
        "lats": all_lats,
        "status": agg_status,
    }


def pct(data: list, p: float) -> float:
    if not data:
        return 0.0
    data_sorted = sorted(data)
    k = (len(data_sorted) - 1) * (p / 100.0)
    lo = int(k)
    hi = min(lo + 1, len(data_sorted) - 1)
    if lo == hi:
        return data_sorted[lo]
    return data_sorted[lo] + (data_sorted[hi] - data_sorted[lo]) * (k - lo)


def write_heylike(path: Path, result: dict) -> None:
    """Format result so summarize.py (hey parser) understands it."""
    lats = result["lats"]
    total = result["total_secs"]
    n = len(lats)
    rps = n / total if total > 0 else 0
    p50 = pct(lats, 50)
    p75 = pct(lats, 75)
    p90 = pct(lats, 90)
    p95 = pct(lats, 95)
    p99 = pct(lats, 99)
    slowest = max(lats) if lats else 0
    fastest = min(lats) if lats else 0
    avg = statistics.mean(lats) if lats else 0

    lines = [
        "Summary:",
        f"  Total:\t{total:.4f} secs",
        f"  Slowest:\t{slowest:.4f} secs",
        f"  Fastest:\t{fastest:.4f} secs",
        f"  Average:\t{avg:.4f} secs",
        f"  Requests/sec:\t{rps:.4f}",
        "",
        "Latency distribution:",
        f"  10%% in {pct(lats, 10):.4f} secs",
        f"  25%% in {pct(lats, 25):.4f} secs",
        f"  50%% in {p50:.4f} secs",
        f"  75%% in {p75:.4f} secs",
        f"  90%% in {p90:.4f} secs",
        f"  95%% in {p95:.4f} secs",
        f"  99%% in {p99:.4f} secs",
        "",
        "Status code distribution:",
    ]
    for code in sorted(result["status"].keys()):
        n_code = result["status"][code]
        if code == -1:
            lines.append(f"  [000]\t{n_code} responses")  # transport errors
        else:
            lines.append(f"  [{code}]\t{n_code} responses")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def check_health() -> None:
    conn = http.client.HTTPConnection(HOST, PORT, timeout=5)
    conn.request("GET", "/health")
    resp = conn.getresponse()
    body = resp.read()
    if resp.status != 200:
        print(f"app not healthy: {resp.status} {body!r}", file=sys.stderr)
        sys.exit(1)
    conn.close()


def main() -> int:
    print(f"variant={VARIANT} app={APP_URL} conc={CONCURRENCIES}")
    check_health()
    for c in CONCURRENCIES:
        print(f"== c={c} ==")
        result = run_sweep(c, WARMUP, DURATION)
        out_path = BENCH_ROOT / f"c{c}" / f"{VARIANT}_shorten.txt"
        write_heylike(out_path, result)
        rps = len(result["lats"]) / result["total_secs"]
        print(f"  -> {out_path.name}: rps={rps:.0f}")

    # 요약 재생성
    try:
        import subprocess

        subprocess.run(
            [sys.executable, str(CHAPTER_ROOT / "bench" / "summarize.py")], check=False
        )
    except Exception as e:
        print(f"(viz skipped: {e})", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
