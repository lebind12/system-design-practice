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

# 부하/벤치마크 실행 — VARIANT 환경변수로 시나리오 태그를 분리
#   make bench VARIANT=mvp     → bench-results/c*/mvp_{shorten,redirect}.txt
#   make bench VARIANT=cache   → ... cache_{shorten,redirect}.txt
# 같은 동시성 디렉토리에 여러 VARIANT 가 함께 쌓이면 summarize.py 가 비교 차트를 만든다
bench:
	VARIANT=$(or $(VARIANT),mvp) ./bench/run.sh

viz:
	@python3 bench/summarize.py

clean:
	docker compose down -v
	cd app && ./gradlew clean || true
