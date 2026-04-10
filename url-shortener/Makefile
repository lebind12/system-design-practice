.PHONY: up down logs run test bench viz clean

APP_URL := http://localhost:28080

# 인프라 기동 (docker-compose 프로젝트명은 docker-compose.yml 의 name 필드로 고정)
up:
	docker compose up -d --build

down:
	docker compose down

logs:
	docker compose logs -f

# 애플리케이션을 로컬 JVM 으로 실행 (인프라는 docker 로 기동된 상태 가정)
run:
	cd app && POSTGRES_HOST=localhost POSTGRES_PORT=25432 REDIS_HOST=localhost REDIS_PORT=26379 ./gradlew bootRun

# 애플리케이션을 docker 이미지로 빌드해 기동 (make up 에 포함되지만 단독으로도 가능)
run-docker:
	docker compose up -d --build app

test:
	cd app && ./gradlew test

# 부하/벤치마크 실행. hey 출력은 bench-results/c<동시성>/<시나리오>.txt 로 저장
bench:
	@echo "TODO: write bench scenarios (shorten / redirect, MVP vs cache)"
	@echo "example: hey -z 30s -c 50 $(APP_URL)/health | tee bench-results/c50/health.txt"

viz:
	@python3 bench/summarize.py

clean:
	docker compose down -v
	cd app && ./gradlew clean || true
