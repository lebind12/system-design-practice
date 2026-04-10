.PHONY: up down logs run test bench viz clean

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

# 부하/벤치마크 실행 (k6, wrk, hey 등). hey 출력은
# bench-results/c<동시성>/<시나리오>.txt 로 저장할 것.
bench:
	@echo "TODO: run load test (e.g. 'hey -z 30s -c 50 <url> | tee bench-results/c50/scenario.txt')"

# 벤치 결과를 summary.csv / summary.md / charts/*.svg 로 시각화.
# 각 챕터는 bench/summarize.py 를 그대로 사용한다 (stdlib 전용, 의존성 없음).
# bench 타겟 말미에서 이 타겟을 호출해 자동 생성되도록 배치할 것.
viz:
	@python3 bench/summarize.py

# 볼륨·빌드 산출물 정리
clean:
	docker compose down -v
	@echo "TODO: remove language-specific build artifacts"
