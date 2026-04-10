# <챕터 주제>

책 X장 실습. <한 줄 요약>

## 스택

- **언어/프레임워크**: <예: Go 1.22, Python 3.12 + FastAPI, Java 21 + Spring Boot 3>
- **인프라**: <docker-compose 에 포함된 것. 예: Redis 7, PostgreSQL 16>
- **벤치 도구**: <예: k6, wrk, hey>

## 요구사항 정의

### 기능 요구사항
-

### 비기능 요구사항
-

### 명시적 비범위
-

## 개략적 규모 추정

| 항목 | 값 |
|---|---|
| DAU | |
| 평균 QPS | |
| 피크 QPS | |
| 저장 용량 (1년) | |
| 대역폭 | |

## 상위 설계

```mermaid
flowchart LR
  client[Client] --> api[API]
  api --> db[(Database)]
```

## MVP 및 확장 실험

- **MVP**:
- **실험 1**:
- **실험 2**:
- **실험 3**:

## 포트 맵

다른 챕터와 충돌하지 않도록 외부 포트 대역을 명시합니다.

| 서비스 | 내부 포트 | 외부 포트 |
|---|---|---|
| app | | |
| redis | 6379 | |
| postgres | 5432 | |

## 환경 변수 (`.env`)

민감 정보(DB 비밀번호, 외부 API 키 등)는 이 챕터 디렉토리의 `.env` 파일에서 관리되며 커밋되지 않습니다 (`.gitignore`). 아래 구조를 참고해 `.env.example` 을 복사한 뒤 값을 채우세요.

```bash
cp .env.example .env
```

| 변수 | 설명 | 예시 값 | 사용처 |
|---|---|---|---|
| `APP_PORT` | 애플리케이션 HTTP 포트 | `8080` | app 서비스 |
| `POSTGRES_USER` | DB 사용자 | `app` | postgres, app |
| `POSTGRES_PASSWORD` | DB 비밀번호 | `<secret>` | postgres, app |
| `POSTGRES_DB` | DB 이름 | `url_shortener` | postgres, app |
| `DATABASE_URL` | 앱이 사용하는 DSN | `postgres://app:***@localhost:5432/url_shortener` | app |
| `REDIS_URL` | Redis 접속 URL | `redis://localhost:6379/0` | app |

> 챕터마다 필요한 변수가 다릅니다. 실제로 사용한 변수만 위 표에 남기고, 사용하지 않는 행은 삭제하세요.

## 실행 방법

```bash
# 0. .env 준비 (최초 1회)
cp .env.example .env
# .env 파일을 열어 값 채우기

# 1. 인프라 기동
make up

# 2. 애플리케이션 실행
make run

# 3. 테스트
make test

# 4. 벤치마크
make bench

# 5. 정리
make down
make clean
```

## 벤치마크 결과

| 시나리오 | 동시성 | 지속시간 | RPS | p50 | p95 | p99 | 에러율 |
|---|---|---|---|---|---|---|---|
| MVP | | | | | | | |
| 실험1 | | | | | | | |
| 실험2 | | | | | | | |

## 의사결정과 트레이드오프

-

## 막힌 지점과 해결

-

## 배운 것

-

## 다음에 시도할 것

-
