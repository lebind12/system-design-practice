# bench/ — 벤치마크 하네스 규약

이 디렉토리는 챕터 간 재사용 가능한 벤치마크 도구를 포함합니다. 새 챕터를 `cp -r _template <chapter>` 로 시작하면 자동으로 딸려옵니다.

## 파일

| 파일 | 역할 |
|---|---|
| `summarize.py` | `bench-results/` 를 파싱해 `summary.csv` · `summary.md` · `charts/*.svg` 생성. stdlib 전용 (pip 불필요). |
| `README.md` | 이 문서. 각 챕터가 따라야 할 규약. |

## 출력 디렉토리 규약

```
bench-results/
├── c50/                 ← 동시성 그룹 (임의 이름 가능, c<N> 권장)
│   ├── scenario-a.txt   ← hey 원본 stdout
│   ├── scenario-b.txt
│   └── ...
├── c500/
│   └── ...
├── summary.csv          ← summarize.py 생성
├── summary.md           ← summarize.py 생성
└── charts/
    ├── rps.svg
    ├── p95.svg
    └── errors.svg
```

- **원본 텍스트 파일은 절대 삭제하지 않습니다.** summary.* 는 언제든 재생성 가능하지만 원본은 재벤치 없이는 복원 불가
- 시나리오 이름(파일명) 은 챕터마다 자유. `stage1.txt`, `mvp.txt`, `with-cache.txt` 등
- 동시성 그룹 디렉토리 이름은 `c<숫자>` 규약을 쓰면 summary 가 숫자 순 정렬
- **`bench-results/` 는 `.gitignore` 에 들어가 커밋되지 않습니다.** 결과는 챕터 README 표에 옮겨 적고, 차트 SVG 만 필요하면 선택적으로 커밋

## 벤치 스크립트 작성 규약 (챕터 bench/ 에 직접 작성하는 부분)

### 반드시 지킬 것

1. **hey 출력은 `| tee <path>` 로 파일과 stdout 에 동시에** — 중간에 스크립트가 죽어도 부분 결과가 남는다
2. **각 시나리오 실행은 `set +e` 블록 안에서** — 앞 시나리오가 깨져도 뒤 시나리오가 계속 돌아야 한다. 깨진 시나리오는 summarize.py 의 에러율 경고로 포착됨
3. **시나리오마다 워밍업 5~10초** — JIT·connection pool·buffer cache 가 안정될 시간을 준다
4. **벤치 끝에 `python3 bench/summarize.py` 자동 호출** — 결과 파일이 즉시 가시화되도록

### 권장 스켈레톤

```bash
#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."  # 챕터 루트

command -v hey >/dev/null || { echo "hey not installed. brew install hey" >&2; exit 1; }

C="${CONCURRENCY:-50}"
OUT_DIR="bench-results/c${C}"
mkdir -p "$OUT_DIR"

run_one() {
  local name="$1" url="$2"
  echo "== ${name} @ c=${C} =="
  # 시나리오 준비 (컨테이너 재기동 등) 는 여기에
  hey -z 5s  -c "$C" "$url" > /dev/null                    # 워밍업
  hey -z 30s -c "$C" "$url" | tee "${OUT_DIR}/${name}.txt" # 측정
}

# 각 시나리오를 set +e 블록 안에서 실행 → 하나 깨져도 나머지 진행
set +e
run_one mvp    http://localhost:XXXX/...
run_one exp1   http://localhost:XXXX/...
run_one exp2   http://localhost:XXXX/...
set -e

python3 bench/summarize.py || echo "(viz skipped)"
```

## 흔한 함정 (이전 챕터에서 드러난 것)

### nginx LB 뒤에 백엔드를 둘 때

기본 nginx 설정은 upstream 에 **keepalive 가 꺼져 있습니다**. c ≥ 500 동시성에서 이 상태로 부하를 주면 요청마다 새 TCP 연결을 열어 `TIME_WAIT` 이 쌓이다가 `EOF` 에러로 무너집니다. 최소한 다음 3개 지시어가 필요합니다:

```nginx
events { worker_connections 4096; }   # 기본 512 는 c=1000 에 부족

http {
  upstream backend {
    server app1:8080;
    server app2:8080;
    keepalive 64;                     # worker 당 유휴 upstream 커넥션 수
  }
  server {
    location / {
      proxy_pass http://backend;
      proxy_http_version 1.1;         # (a) keepalive 가 동작하려면 HTTP/1.1
      proxy_set_header Connection ""; # (b) upstream 방향으로 Connection 헤더를 비움
    }
  }
}
```

(출처: `scaling-foundations` 챕터에서 stage3/4 의 EOF 에러 원인 분석)

### bench 스크립트가 중간에 죽는 경우

`set -e` + `set -o pipefail` 환경에서 `hey` 가 부분 실패하거나 `make stage<n>` 의 헬스체크가 타임아웃하면 스크립트 전체가 종료되어 뒷 시나리오가 통째로 날아갑니다. 위의 `set +e` 블록 패턴으로 격리하세요.

### summary 가 없는 결과는 "없는 거나 마찬가지"

벤치를 돌리고 바로 `summarize.py` 를 부르지 않으면, 원본 텍스트는 사람이 눈으로 파싱해야 합니다. 챕터 bench 스크립트 말미에 반드시 `python3 bench/summarize.py` 를 넣으세요. 에러율 ≥ 1% 인 시나리오가 있으면 stderr 에 경고가 뜹니다 — **경고가 떴다면 "벤치가 끝났다" 가 아니라 "문제 있는 측정이다"** 로 읽어야 합니다.
