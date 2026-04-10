# 저장소 규약 (세션 공통 지시문)

이 저장소는 『가상 면접 사례로 배우는 대규모 시스템 설계 기초』(알렉스 쉬) 를 **직접 구현**으로 학습합니다. 새 대화 세션 하나 = 한 챕터 실습입니다. 아래 규약을 반드시 따르세요.

## 0. 세션 시작 시 읽을 문서

매 세션 시작 시 다음을 순서대로 확인합니다.

1. 이 `CLAUDE.md`
2. [docs/CHAPTER_WORKFLOW.md](docs/CHAPTER_WORKFLOW.md) — 단계별 진행 규약
3. [docs/NOTION_TEMPLATE.md](docs/NOTION_TEMPLATE.md) — 세션 종료 시 기록 규칙
4. 루트 [README.md](README.md) — 챕터 인덱스 (이번 챕터가 이미 존재하는지 확인)

## 1. 사용자에게 먼저 물을 것

- **어느 챕터**를 진행할지 (책 목차 번호 또는 주제명)
- **구현 언어/프레임워크** (예: Go, Python+FastAPI, Python+Django, Java+Spring Boot, 등)
- **MVP 범위 스코프** — 책의 설계 전체를 다 만들 필요 없음. 최소 구동 + 확장 실험 2~3개를 합의

## 2. 디렉토리 및 네이밍 규칙

- 챕터 디렉토리는 **루트 바로 아래**에 생성합니다.
- 네이밍은 **kebab-case**, 주제를 나타내는 이름 (예: `url-shortener`, `rate-limiter`, `consistent-hash`, `key-value-store`, `unique-id-generator`, `web-crawler`, `notification-system`, `news-feed`, `chat-system`, `search-autocomplete`, `youtube`, `google-drive`).
- 시작 시 `_template/` 디렉토리를 **복사**해 새 챕터 디렉토리로 만듭니다.
  ```bash
  cp -r _template <chapter-name>
  ```
- 인프라(docker-compose.yml)와 Makefile, README.md는 **챕터 디렉토리 내부에 소유**합니다. 루트에는 인프라 파일이 없습니다.

## 3. Docker Compose 규약

- 각 챕터의 `docker-compose.yml` 최상단에 반드시 `name: <chapter-dir-name>` 필드를 두어, Docker Desktop에서 챕터별로 컨테이너가 그룹핑되도록 합니다.
- 외부 포트 충돌을 피하기 위해 챕터마다 포트 대역을 달리합니다 (README에 포트 맵 명시).

## 4. Makefile 공통 타겟

언어가 달라도 아래 타겟명은 통일합니다 (내부 명령은 자유):

| 타겟 | 용도 |
|---|---|
| `make up` | 인프라 기동 (`docker compose up -d`) |
| `make down` | 인프라 종료 |
| `make logs` | 인프라 로그 |
| `make run` | 애플리케이션 실행 |
| `make test` | 테스트 |
| `make bench` | 부하/벤치마크 실행 |
| `make clean` | 볼륨·빌드 산출물 정리 |

## 4-b. 환경 변수(`.env`) 관리

- 민감 정보(DB 비밀번호, 외부 API 키 등)는 **각 챕터 디렉토리의 `.env` 파일**에서 관리합니다.
- `.env` 는 커밋되지 않습니다 (`.gitignore` 에 등록되어 있음). 커밋되는 것은 구조만 담긴 `.env.example` 입니다.
- 챕터 시작 시 `cp .env.example .env` 로 실제 파일을 만들고 값을 채웁니다.
- **챕터 `README.md` 의 "환경 변수 (.env)" 섹션 표는 반드시 실제 사용 변수로 업데이트**합니다. 이 표는 포트폴리오를 보는 사람이 시크릿 없이도 시스템 구조를 이해하게 해주는 장치입니다.
- `docker-compose.yml` 과 애플리케이션 코드가 참조하는 변수명은 일치시킵니다.

## 5. 챕터 진행 흐름 (상세는 CHAPTER_WORKFLOW.md)

요구사항 정의 → 개략적 규모 추정 → 상위 설계 → MVP 범위 확정 → 구현 → 확장 실험 → 벤치마크 → 회고 → git 커밋 → 노션 기록 → 루트 README 갱신.

## 6. 세션 종료 시 반드시 할 3가지

1. **git 커밋** — 챕터 단위로 의미 있는 커밋. 사용자가 승인하면 진행.
2. **노션 기록** — `docs/NOTION_TEMPLATE.md` 규약에 따라 지정 DB에 페이지 생성.
   - DB URL: `https://www.notion.so/woolee/696870c4d2784a95b3b0b736254778b3`
   - Data source ID: `cc597a9d-1bb4-47b5-9c86-8b214d92dbda`
   - **페이지 제목은 반드시** `[가상 면접 사례로 배우는 대규모 시스템 설계 기초] <챕터 주제>` 형식
   - 학습모드: `대화형학습` (직접 구현 실습이므로)
3. **루트 README.md 인덱스 갱신** — 해당 챕터 행을 완료 상태로 업데이트하고 디렉토리/언어/노션 링크 채우기.

## 7. 해서는 안 되는 것

- 루트에 `infra/`, `docker-compose.yml`, `Makefile` 등을 새로 만들지 않습니다. 인프라는 항상 챕터 내부에 위치합니다.
- 언어·프레임워크를 사용자 확인 없이 임의로 결정하지 않습니다.
- 책 원문을 그대로 옮기지 않습니다 — 구현 경험과 관찰 결과(벤치 수치 등) 중심으로 기록합니다.
- 노션 페이지 제목 접두어 `[가상 면접 사례로 배우는 대규모 시스템 설계 기초]` 를 누락하지 않습니다.
- **`git push` 는 절대 스스로 실행하지 않습니다.** 로컬 커밋까지만 Claude 가 진행하며, 원격 저장소로의 푸시는 **항상 사용자가 직접** 수행합니다. 사용자가 명시적으로 "push 해줘" 라고 요청하더라도, 재차 확인 후에만 실행하고, 기본값은 "사용자가 직접 수행" 입니다. `git push --force`, `git push` 대상 브랜치 변경 등은 어떤 경우에도 Claude 가 수행하지 않습니다.
