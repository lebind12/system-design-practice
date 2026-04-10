#!/usr/bin/env python3
"""
hey 벤치 결과(bench-results/[c<동시성>/]*.txt) 를 파싱해
  - bench-results/summary.csv    : 기계 가독 원본 수치
  - bench-results/summary.md     : 마크다운 표
  - bench-results/charts/*.svg   : RPS / p95 / errors 비교 차트
를 생성한다. stdlib 만 사용 (matplotlib 불필요). SVG 는 GitHub · Notion 에서
바로 렌더된다.

디렉토리 규약:
  bench-results/
  ├── c50/
  │   ├── stage1.txt     <- hey 출력
  │   ├── stage2.txt
  │   └── ...
  ├── c500/
  │   └── ...
  └── c1000/
      └── ...

"stage<N>" 대신 임의의 시나리오명도 된다 (파일명이 곧 그룹 이름). 동시성이
하나뿐이면 c0/ 같은 단일 하위 디렉토리에 모으거나, bench-results 직속에
*.txt 를 두면 "default" 동시성 그룹으로 분류된다.

사용:
    python3 summarize.py [--root <chapter-root>]

기본은 스크립트 위치 두 단계 위의 디렉토리(= 챕터 루트) 를 가정한다.
각 챕터는 이 파일을 bench/ 아래 그대로 두고 Makefile 의 viz 타겟에서 호출.
"""
from __future__ import annotations

import argparse
import csv
import re
import sys
from pathlib import Path

# ---- 파싱 ------------------------------------------------------------------

_NUM = r"[0-9]+\.?[0-9]*"
_RE_RPS = re.compile(rf"Requests/sec:\s*({_NUM})")
_RE_TOTAL = re.compile(rf"Total:\s*({_NUM})\s*secs")
_RE_PCTL = re.compile(rf"(\d+)%%\s+in\s+({_NUM})\s*secs")
# "Status code distribution" 섹션의 "[NNN] M responses" 를 모두 잡는다.
# 2xx/3xx = ok, 4xx/5xx = errors 로 분류 (예: POST 201 Created, GET 302 redirect 도 성공으로 집계)
_RE_STATUS_ANY = re.compile(rf"\[(\d{{3}})\]\s*({_NUM})\s*responses")
_RE_ERROR_LINE = re.compile(rf"\[({_NUM})\]\s+(Get|Post|Put|Delete)\s")


def parse_file(path: Path) -> dict:
    text = path.read_text(encoding="utf-8", errors="replace")
    out = {"file": str(path), "rps": None, "p50": None, "p75": None,
           "p90": None, "p95": None, "p99": None,
           "ok": 0, "errors": 0, "total_secs": None}

    m = _RE_RPS.search(text)
    if m:
        out["rps"] = float(m.group(1))

    m = _RE_TOTAL.search(text)
    if m:
        out["total_secs"] = float(m.group(1))

    for m in _RE_PCTL.finditer(text):
        pct = int(m.group(1))
        secs = float(m.group(2))
        if pct in (50, 75, 90, 95, 99):
            out[f"p{pct}"] = secs * 1000.0

    # Status code 분포를 모두 수집하여 2xx/3xx=ok, 4xx/5xx=errors 로 분류
    ok_total = 0
    status_err_total = 0
    for sm in _RE_STATUS_ANY.finditer(text):
        code = int(sm.group(1))
        n = int(float(sm.group(2)))
        if 200 <= code < 400:
            ok_total += n
        else:
            status_err_total += n
    out["ok"] = ok_total

    err_total = status_err_total
    in_err_block = False
    for line in text.splitlines():
        if line.strip().startswith("Error distribution"):
            in_err_block = True
            continue
        if in_err_block:
            m = _RE_ERROR_LINE.search(line)
            if m:
                err_total += int(float(m.group(1)))
    out["errors"] = err_total

    return out


def collect(root: Path) -> list[dict]:
    results = []
    br = root / "bench-results"
    if not br.exists():
        return results

    def add(txt_path: Path, concurrency: str):
        m = re.match(r"(.+)\.txt$", txt_path.name)
        if not m:
            return
        scenario = m.group(1)
        rec = parse_file(txt_path)
        rec["concurrency"] = concurrency
        rec["scenario"] = scenario
        results.append(rec)

    # subdirectory mode: bench-results/c<N>/*.txt
    for sub in sorted(br.iterdir()):
        if not sub.is_dir() or sub.name == "charts":
            continue
        for f in sorted(sub.glob("*.txt")):
            add(f, sub.name)
    # flat mode: bench-results/*.txt
    for f in sorted(br.glob("*.txt")):
        add(f, "default")

    def _ckey(c):
        m = re.match(r"c(\d+)", c)
        return (0, int(m.group(1))) if m else (1, c)

    results.sort(key=lambda r: (_ckey(r["concurrency"]), r["scenario"]))
    return results


# ---- CSV / Markdown --------------------------------------------------------

CSV_COLS = ["concurrency", "scenario", "rps", "p50", "p75", "p90", "p95",
            "p99", "ok", "errors", "total_secs", "file"]


def write_csv(results: list[dict], path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", newline="") as f:
        w = csv.DictWriter(f, fieldnames=CSV_COLS)
        w.writeheader()
        for r in results:
            w.writerow({k: r.get(k, "") for k in CSV_COLS})


def _fmt(x, spec="{:,.0f}"):
    if x is None:
        return "—"
    try:
        return spec.format(x)
    except (TypeError, ValueError):
        return str(x)


def write_markdown(results: list[dict], path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    concurrencies = []
    seen = set()
    for r in results:
        if r["concurrency"] not in seen:
            concurrencies.append(r["concurrency"])
            seen.add(r["concurrency"])
    scenarios = sorted({r["scenario"] for r in results})

    lines = ["# Bench summary", "",
             "자동 생성 — `make viz` 혹은 `bench-all.sh` 실행 시 덮어쓰입니다.", ""]
    for c in concurrencies:
        lines.append(f"## {c}")
        lines.append("")
        lines.append("| scenario | RPS | p50 (ms) | p95 (ms) | p99 (ms) | ok | errors |")
        lines.append("|---|---:|---:|---:|---:|---:|---:|")
        for s in scenarios:
            row = next((r for r in results if r["concurrency"] == c and r["scenario"] == s), None)
            if row is None:
                lines.append(f"| {s} | — | — | — | — | — | — |")
                continue
            lines.append(
                f"| {s} | {_fmt(row['rps'])} | {_fmt(row['p50'], '{:.1f}')} | "
                f"{_fmt(row['p95'], '{:.1f}')} | {_fmt(row['p99'], '{:.1f}')} | "
                f"{_fmt(row['ok'])} | {_fmt(row['errors'])} |"
            )
        lines.append("")
    path.write_text("\n".join(lines), encoding="utf-8")


# ---- SVG 차트 --------------------------------------------------------------

_COLORS = ["#2563eb", "#16a34a", "#f59e0b", "#dc2626", "#7c3aed"]


def _svg_grouped_bar(
    title: str, ylabel: str, groups: list[str],
    series: list[tuple[str, list[float | None]]], out: Path, log: bool = False,
) -> None:
    W, H = 880, 460
    M_L, M_R, M_T, M_B = 70, 30, 60, 90
    plot_w = W - M_L - M_R
    plot_h = H - M_T - M_B

    any_numeric = any(v is not None for _, vs in series for v in vs)
    all_vals = [v for _, vs in series for v in vs if v is not None and v > 0]
    if not any_numeric:
        out.write_text(f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}"><text x="20" y="40">(no data)</text></svg>')
        return
    if not all_vals:
        # 데이터는 있지만 전부 0 — 에러 차트에서 "전 시나리오 0 에러" 상태 표기
        out.write_text(
            f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" font-family="system-ui, sans-serif">'
            f'<rect width="{W}" height="{H}" fill="#f0fdf4"/>'
            f'<text x="{W/2}" y="{H/2 - 10}" text-anchor="middle" font-size="22" font-weight="600" fill="#16a34a">✓ 0 across all scenarios</text>'
            f'<text x="{W/2}" y="{H/2 + 18}" text-anchor="middle" font-size="14" fill="#166534">{title}</text>'
            f'</svg>'
        )
        return

    vmax = max(all_vals)
    if log:
        import math
        vmin = min(all_vals)
        lo = math.log10(max(vmin, 0.1))
        hi = math.log10(vmax * 1.2)
    else:
        lo, hi = 0.0, vmax * 1.15

    def y_of(v: float) -> float:
        if log:
            import math
            lv = math.log10(max(v, 0.1))
            return M_T + plot_h * (1 - (lv - lo) / (hi - lo))
        return M_T + plot_h * (1 - (v - lo) / (hi - lo))

    n_groups = len(groups)
    n_series = len(series)
    group_w = plot_w / n_groups
    bar_w = group_w * 0.7 / max(n_series, 1)

    parts = [
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" font-family="system-ui, sans-serif" font-size="13">',
        f'<rect width="{W}" height="{H}" fill="#ffffff"/>',
        f'<text x="{W/2}" y="28" text-anchor="middle" font-size="17" font-weight="600">{title}</text>',
        f'<text x="20" y="{M_T + plot_h/2}" text-anchor="middle" transform="rotate(-90 20 {M_T + plot_h/2})" font-size="12" fill="#555">{ylabel}</text>',
    ]

    for i in range(5):
        frac = i / 4
        yy = M_T + plot_h * (1 - frac)
        if log:
            import math
            val = 10 ** (lo + frac * (hi - lo))
        else:
            val = lo + frac * (hi - lo)
        parts.append(f'<line x1="{M_L}" y1="{yy}" x2="{M_L + plot_w}" y2="{yy}" stroke="#e5e7eb" stroke-width="1"/>')
        label = f"{val:,.0f}" if val >= 10 else f"{val:.2f}"
        parts.append(f'<text x="{M_L - 8}" y="{yy + 4}" text-anchor="end" fill="#555">{label}</text>')

    for gi, gname in enumerate(groups):
        cx = M_L + group_w * gi + group_w * 0.15
        for si, (sname, vals) in enumerate(series):
            v = vals[gi]
            x = cx + bar_w * si
            if v is None or v <= 0:
                parts.append(f'<text x="{x + bar_w/2}" y="{M_T + plot_h - 4}" text-anchor="middle" fill="#999" font-size="14">×</text>')
                continue
            yy = y_of(v)
            h = M_T + plot_h - yy
            parts.append(
                f'<rect x="{x}" y="{yy}" width="{bar_w}" height="{h}" fill="{_COLORS[si % len(_COLORS)]}" opacity="0.9"/>'
            )
            label = f"{v:,.0f}" if v >= 100 else f"{v:.1f}"
            parts.append(f'<text x="{x + bar_w/2}" y="{yy - 4}" text-anchor="middle" font-size="10" fill="#333">{label}</text>')
        parts.append(f'<text x="{M_L + group_w * (gi + 0.5)}" y="{M_T + plot_h + 22}" text-anchor="middle" font-size="13" fill="#333">{gname}</text>')

    parts.append(f'<line x1="{M_L}" y1="{M_T + plot_h}" x2="{M_L + plot_w}" y2="{M_T + plot_h}" stroke="#333" stroke-width="1"/>')

    lx = M_L
    ly = H - 30
    for si, (sname, _) in enumerate(series):
        parts.append(f'<rect x="{lx}" y="{ly - 10}" width="14" height="14" fill="{_COLORS[si % len(_COLORS)]}"/>')
        parts.append(f'<text x="{lx + 20}" y="{ly + 1}" font-size="12" fill="#333">{sname}</text>')
        lx += 110

    parts.append("</svg>")
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text("\n".join(parts), encoding="utf-8")


def make_charts(results: list[dict], out_dir: Path) -> None:
    concurrencies = []
    seen = set()
    for r in results:
        if r["concurrency"] not in seen:
            concurrencies.append(r["concurrency"])
            seen.add(r["concurrency"])
    scenarios = sorted({r["scenario"] for r in results})
    groups = scenarios

    def extract(metric: str, c: str) -> list[float | None]:
        vals = []
        for s in scenarios:
            row = next((r for r in results if r["concurrency"] == c and r["scenario"] == s), None)
            vals.append(row[metric] if row and row.get(metric) is not None else None)
        return vals

    _svg_grouped_bar(
        "RPS by scenario × concurrency", "requests / sec", groups,
        [(c, extract("rps", c)) for c in concurrencies],
        out_dir / "rps.svg",
    )
    _svg_grouped_bar(
        "p95 latency by scenario × concurrency (log scale)", "latency (ms, log)", groups,
        [(c, extract("p95", c)) for c in concurrencies],
        out_dir / "p95.svg", log=True,
    )
    _svg_grouped_bar(
        "Error count by scenario × concurrency", "errors (30s window)", groups,
        [(c, extract("errors", c)) for c in concurrencies],
        out_dir / "errors.svg",
    )


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--root", type=Path,
                    default=Path(__file__).resolve().parents[1],
                    help="챕터 루트 디렉토리 (bench-results/ 를 포함하는 곳)")
    args = ap.parse_args()

    results = collect(args.root)
    if not results:
        print(f"no results under {args.root}/bench-results/", file=sys.stderr)
        return 1

    br = args.root / "bench-results"
    write_csv(results, br / "summary.csv")
    write_markdown(results, br / "summary.md")
    make_charts(results, br / "charts")

    print(f"parsed {len(results)} result files")
    print(f"  {br/'summary.csv'}")
    print(f"  {br/'summary.md'}")
    print(f"  {br/'charts'}/rps.svg, p95.svg, errors.svg")

    # 에러율 경고 — 벤치가 "끝났으니 OK" 를 암시하지 않도록,
    # 에러율 ≥ 1% 인 결과는 명시적으로 stderr 에 빨간 경고를 찍는다.
    # (scaling-foundations 챕터에서 nginx keepalive 누락으로 stage3·4 가 고에러율로
    #  "측정된" 사건 이후 하네스에 추가된 가드.)
    warned = False
    for r in results:
        ok = r.get("ok", 0) or 0
        errs = r.get("errors", 0) or 0
        total = ok + errs
        rate = (errs / total) if total > 0 else 0
        label = f"{r['concurrency']}/{r.get('scenario', '?')}"
        if total == 0:
            print(f"\033[33m⚠ {label}: empty result (파싱은 됐지만 200/에러 라인 없음)\033[0m", file=sys.stderr)
            warned = True
        elif rate >= 0.01:
            print(
                f"\033[31m⚠ {label}: error rate {rate*100:.1f}% ({errs:,}/{total:,}) — "
                f"안정 상태 측정이 아닐 가능성. 원인 확인 필요.\033[0m",
                file=sys.stderr,
            )
            warned = True
    if warned:
        print(
            "\n⚠ 에러율이 높은 시나리오는 RPS/p95 수치를 그대로 신뢰하면 안 됩니다.\n"
            "   원인 후보: LB/upstream 튜닝, 백엔드 포화, 커넥션 풀·소켓 고갈, 컨테이너 리소스 제한.",
            file=sys.stderr,
        )
    return 0


if __name__ == "__main__":
    sys.exit(main())
