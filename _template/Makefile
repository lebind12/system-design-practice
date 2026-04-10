.PHONY: up down logs run test bench clean

# 인프라 기동 (docker-compose 프로젝트명은 docker-compose.yml 의 name: 필드로 고정)
up:
	docker compose up -d

down:
	docker compose down

logs:
	docker compose logs -f

# 애플리케이션 실행 (언어별로 내용을 채워 넣을 것)
run:
	@echo "TODO: run application (e.g. 'go run ./...', 'uvicorn app:app', './gradlew bootRun')"

test:
	@echo "TODO: run unit/integration tests"

# 부하/벤치마크 실행 (k6, wrk, hey 등)
bench:
	@echo "TODO: run load test (e.g. 'k6 run bench/load.js')"

# 볼륨·빌드 산출물 정리
clean:
	docker compose down -v
	@echo "TODO: remove language-specific build artifacts"
