# lab2 — capacity estimator CLI

책 2장 "개략적인 규모 추정" 의 계산을 자동화한 CLI. 입력값을 바꿔가며 즉석에서 QPS / 저장 용량 / 대역폭을 산출하고, 책의 치트시트(2의 제곱수, 지연시간 상수, 가용성 숫자) 를 함께 출력한다.

## 빌드·실행

```bash
./gradlew run --args='--dau=1000000 --reqPerUser=10 --payloadBytes=200 --newItemsPerYear=100000000'
```

플래그:

| 플래그 | 의미 | 기본값 |
|---|---|---|
| `--dau` | Daily Active Users | 필수 |
| `--reqPerUser` | 사용자당 일 요청 수 | 필수 |
| `--payloadBytes` | 요청·응답 평균 바이트 | 필수 |
| `--newItemsPerYear` | 1년 신규 레코드 수 | 0 (저장 추정 생략 가능) |
| `--peakFactor` | 평균 → 피크 배수 | 3 |
| `--cheatsheet` | 치트시트만 출력 | - |

## 출력 예

```
$ ./gradlew run --args='--dau=1000000 --reqPerUser=10 --payloadBytes=200 --newItemsPerYear=100000000'

== 입력 ==
DAU                       1,000,000
사용자당 일 요청           10
평균 페이로드 (B)          200
1년 신규 레코드            100,000,000
피크 배수                  3

== 트래픽 ==
일 요청 수                10,000,000
평균 QPS                  ~115
피크 QPS                  ~347

== 대역폭 ==
평균                      ~22.59 KB/s
피크                      ~67.78 KB/s

== 저장 (1년) ==
원시 데이터               ~19.07 GB
복제 3x                   ~57.22 GB

== 치트시트 ==
[2의 제곱수]
  2^10 ≈ 1 K     (1 KB)
  2^20 ≈ 1 M     (1 MB)
  ...
```
