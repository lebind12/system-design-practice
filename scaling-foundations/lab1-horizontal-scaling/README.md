# lab1 — horizontal scaling stages

책 1장 "단일 서버에서 시작하는 시스템" 진화를 4단계로 직접 구동한다.

| stage | 구성 | Spring 모드 | LB | 캐시 |
|---|---|---|---|---|
| 1 | app1 | `STORAGE_MODE=memory` | x | x |
| 2 | app1 + postgres | `STORAGE_MODE=jdbc` | x | x |
| 3 | app1 + app2 + postgres + nginx | `STORAGE_MODE=jdbc` | nginx (RR) | x |
| 4 | app1 + app2 + postgres + nginx + redis | `STORAGE_MODE=jdbc` `CACHE_ENABLED=true` | nginx (RR) | redis (TTL 60s) |

## 동작 방식

- Spring Boot 3.3 / Java 21, 단일 jar 빌드
- 환경 변수 `STORAGE_MODE` (memory|jdbc) 와 `CACHE_ENABLED` (true|false) 로 stage 별 동작 토글
- `INSTANCE_ID` 환경 변수로 어느 replica 가 응답했는지 `/health` 응답에 노출
- stage1 에서는 `SPRING_AUTOCONFIGURE_EXCLUDE` 로 DataSource·Redis autoconfig 를 제외해 외부 의존 없이 기동
- 데이터 모델은 `items(id, name, views)` 단일 테이블

## 기동·벤치는 챕터 루트의 Makefile 에서

```bash
cd ..
make stage1   # 그리고 make bench
make stage2
make stage3
make stage4
make bench-all   # 4단계 자동 순회
```
