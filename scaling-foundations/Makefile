.PHONY: up down logs run test bench bench-all bench-tuned viz clean stage1 stage2 stage3 stage4 wait seed help

# scaling-foundations / lab1
# 4단계 수평 확장 실험을 stage<n> 타겟으로 토글한다.
# 각 타겟은 (1) 기존 컨테이너 정리 (2) 해당 stage profile + 환경 변수로 기동
# (3) /health 가 200 을 반환할 때까지 대기 (4) seed 데이터 1건 생성 을 수행한다.

SPRING_EXCLUDE_DS := org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration
SPRING_EXCLUDE_REDIS := org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration

# stage1 이 어디서 응답하는지 (app1 직결) — stage3/4 에서는 nginx LB 로 바뀐다.
STAGE1_URL := http://localhost:18081
STAGE2_URL := http://localhost:18081
STAGE3_URL := http://localhost:18080
STAGE4_URL := http://localhost:18080

help:
	@echo "make stage1 | stage2 | stage3 | stage4 — 단계 기동 (이전 stage 자동 정리)"
	@echo "make bench  — 현재 stage 에 대해 hey 부하 1회"
	@echo "make bench-all — 4단계 순회 벤치 (~5분). CONCURRENCY=N 으로 동시성 변경"
	@echo "make bench-tuned — HikariCP pool=100 으로 stage2/3/4 보조 실험 (c=1000)"
	@echo "make viz    — bench-results/ 를 파싱해 summary.md/csv, charts/*.svg 생성"
	@echo "make down   — 모든 컨테이너 정리"
	@echo "make clean  — 볼륨까지 삭제"

# stage1: HashMap 단일 — postgres/redis autoconfig 모두 제외
stage1: down
	STAGE=1 STORAGE_MODE=memory CACHE_ENABLED=false \
	SPRING_EXCLUDE="$(SPRING_EXCLUDE_DS),$(SPRING_EXCLUDE_REDIS)" \
	COMPOSE_PROFILES=stage1 docker compose up -d --build
	@$(MAKE) wait URL=$(STAGE1_URL)
	@$(MAKE) seed URL=$(STAGE1_URL)
	@echo ">> stage1 ready at $(STAGE1_URL)"

# stage2: postgres 추가 — redis 만 제외
stage2: down
	STAGE=2 STORAGE_MODE=jdbc CACHE_ENABLED=false \
	SPRING_EXCLUDE="$(SPRING_EXCLUDE_REDIS)" \
	COMPOSE_PROFILES=stage2 docker compose up -d --build
	@$(MAKE) wait URL=$(STAGE2_URL)
	@$(MAKE) seed URL=$(STAGE2_URL)
	@echo ">> stage2 ready at $(STAGE2_URL)"

# stage3: nginx + 2 replicas — redis 만 제외
stage3: down
	STAGE=3 STORAGE_MODE=jdbc CACHE_ENABLED=false \
	SPRING_EXCLUDE="$(SPRING_EXCLUDE_REDIS)" \
	COMPOSE_PROFILES=stage3 docker compose up -d --build
	@$(MAKE) wait URL=$(STAGE3_URL)
	@$(MAKE) seed URL=$(STAGE3_URL)
	@echo ">> stage3 ready at $(STAGE3_URL)"

# stage4: 모든 것
stage4: down
	STAGE=4 STORAGE_MODE=jdbc CACHE_ENABLED=true \
	SPRING_EXCLUDE="" \
	COMPOSE_PROFILES=stage4 docker compose up -d --build
	@$(MAKE) wait URL=$(STAGE4_URL)
	@$(MAKE) seed URL=$(STAGE4_URL)
	@echo ">> stage4 ready at $(STAGE4_URL)"

# 앱이 응답할 때까지 최대 60s 대기
wait:
	@echo "waiting for $(URL)/health ..."
	@for i in $$(seq 1 30); do \
	  if curl -fsS $(URL)/health > /dev/null 2>&1; then echo "ready ($$i s)"; exit 0; fi; \
	  sleep 2; \
	done; echo "TIMEOUT"; exit 1

# 벤치 대상 1건 시드
seed:
	@curl -fsS -X POST $(URL)/api/items \
	  -H "Content-Type: application/json" \
	  -d '{"id":"seed-1","name":"benchmark seed item"}' > /dev/null && echo "seeded seed-1"

# 현재 떠 있는 stage 의 첫 노출 포트로 hey 부하
bench:
	@bash lab1-horizontal-scaling/bench/bench.sh

bench-all:
	@bash lab1-horizontal-scaling/bench/bench-all.sh

# 보조 실험 — HikariCP 어드미션 컨트롤 가설 검증.
# stage2/3/4 를 pool=100 으로 c=1000 재벤치하고 bench-results/c1000-tuned/ 에 저장.
# 기본값 변경: CONCURRENCY=500 make bench-tuned, HIKARI_POOL_SIZE=50 make bench-tuned
bench-tuned:
	@bash lab1-horizontal-scaling/bench/bench-tuned.sh

# 벤치 결과(bench-results/c*/stage*.txt) 를 파싱해
# summary.csv, summary.md, charts/*.svg 생성.
# bench-all.sh 는 마지막에 이 타겟을 자동으로 호출한다.
viz:
	@python3 lab1-horizontal-scaling/bench/summarize.py

down:
	@docker compose --profile stage1 --profile stage2 --profile stage3 --profile stage4 down 2>/dev/null || true

logs:
	docker compose logs -f

clean:
	docker compose --profile stage1 --profile stage2 --profile stage3 --profile stage4 down -v
