# AWS 선착순 드로우 부하 테스트와 Grafana 관측 기록

## 1. 테스트 목적

선착순 드로우 API는 요청을 즉시 DB에서 모두 처리하지 않고, 먼저 Redis Sorted Set 큐에 적재한 뒤 worker가 비동기로 소비한다.

따라서 k6 결과만 보면 "API가 요청을 받았는지"는 알 수 있지만, 다음 질문에는 답하기 어렵다.

- Redis 큐가 얼마나 쌓였는가?
- worker가 큐를 언제까지 처리했는가?
- 최종 `SUCCESS`, `FAILED`, `PENDING_PAYMENT` 상태가 어떻게 수렴했는가?
- 재고가 0 밑으로 내려가지 않고 정합성이 유지됐는가?

이를 확인하기 위해 draw-service에 Prometheus metric을 추가하고, 로컬 PC의 Prometheus/Grafana에서 AWS draw-service를 관측했다.

## 2. 테스트 환경

- draw-service: AWS EC2
- draw-service public endpoint: `http://13.221.46.192:8081`
- payment-service: 별도 AWS EC2
- draw DB: AWS RDS MySQL
- payment DB: AWS RDS MySQL
- Redis: AWS ElastiCache Serverless Redis, TLS 사용
- metric 수집: 로컬 PC Docker Prometheus
- 시각화: 로컬 PC Docker Grafana
- 부하 생성: 로컬 PC k6

Prometheus scrape 대상은 AWS draw-service의 actuator endpoint다.

```text
http://13.221.46.192:8081/actuator/prometheus
```

## 3. 애플리케이션 관측 지표

draw-service에 다음 custom metric을 추가했다.

```promql
draw_queue_size{product_id="1"}
```

상품별 Redis 큐 대기 수를 나타낸다.

```promql
draw_entries{product_id="1",status="PENDING_PAYMENT"}
draw_entries{product_id="1",status="SUCCESS"}
draw_entries{product_id="1",status="FAILED"}
```

상품별 드로우 엔트리의 상태별 개수를 나타낸다.

```promql
draw_product_quantity{product_id="1",type="remaining"}
```

상품의 잔여 재고를 나타낸다.

## 4. 로컬 Prometheus 설정

로컬 PC에서 AWS draw-service를 scrape하도록 `prometheus-aws.yml`을 생성했다.

```powershell
@"
global:
  scrape_interval: 5s

scrape_configs:
  - job_name: "aws-draw-service"
    metrics_path: "/actuator/prometheus"
    static_configs:
      - targets: ["13.221.46.192:8081"]
"@ | Set-Content -Encoding UTF8 prometheus-aws.yml
```

Prometheus 컨테이너 실행:

```powershell
docker rm -f prometheus-aws 2>$null

docker run -d --name prometheus-aws `
  -p 9090:9090 `
  -v "${PWD}\prometheus-aws.yml:/etc/prometheus/prometheus.yml" `
  prom/prometheus:latest
```

Prometheus target 확인:

```promql
up{job="aws-draw-service"}
```

결과가 `1`이면 로컬 Prometheus가 AWS draw-service metric을 정상 수집 중이라는 뜻이다.

## 5. 로컬 Grafana 설정

Grafana 컨테이너 실행:

```powershell
docker rm -f grafana-local 2>$null

docker run -d --name grafana-local `
  -p 3000:3000 `
  grafana/grafana:latest
```

Grafana 접속:

```text
http://localhost:3000
```

기본 계정:

```text
admin / admin
```

Prometheus data source URL:

```text
http://host.docker.internal:9090
```

테스트 중에는 Grafana Explore에서 다음처럼 설정했다.

```text
Time range: Last 15 minutes
Refresh: 5s
```

## 6. 테스트 전 초기화 명령어

### 6.1 draw-service 일시 중지

이전 부하 테스트의 큐 처리 후유증을 피하기 위해 draw-service를 먼저 멈췄다.

```bash
cd ~/FCFS_Draw_Service
docker stop draw-service
docker update --restart=no draw-service
```

### 6.2 Redis 큐 초기화

ElastiCache Serverless Redis는 TLS를 사용하므로 `--tls --insecure` 옵션으로 redis-cli를 접속했다.

```bash
docker run -it --rm redis:7.4 redis-cli \
  -h fcfs-redis-6qrvvl.serverless.use1.cache.amazonaws.com \
  -p 6379 \
  --tls \
  --insecure
```

Redis 명령:

```redis
DEL draw:queue:products
KEYS draw:queue:*
DEL draw:queue:1
```

`draw:queue:1`의 `1`은 이전 상품 ID다. 상품 ID가 다르면 해당 ID로 바꿔 삭제한다.

### 6.3 draw DB 초기화

```bash
docker run -it --rm mysql:8.4 mysql \
  -h DRAW_RDS_ENDPOINT \
  -u fcfs \
  -pfcfsdrawtest \
  fcfs_draw
```

SQL:

```sql
SET FOREIGN_KEY_CHECKS=0;
TRUNCATE TABLE draw_entries;
TRUNCATE TABLE products;
SET FOREIGN_KEY_CHECKS=1;
```

### 6.4 payment DB 초기화와 지갑 생성

10,000명 테스트를 위해 `wallets`를 10,000개 생성했다. 홀수 유저는 잔액 부족, 짝수 유저는 결제 가능 상태로 구성했다.

```bash
docker run -it --rm mysql:8.4 mysql \
  -h PAYMENT_RDS_ENDPOINT \
  -u fcfs \
  -pfcfspaymenttest \
  fcfs_payment
```

SQL:

```sql
SET SESSION cte_max_recursion_depth = 11000;

TRUNCATE TABLE payment_transactions;
TRUNCATE TABLE wallets;

INSERT INTO wallets (user_id, balance, created_at, updated_at)
WITH RECURSIVE seq AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 10000
)
SELECT
  n,
  CASE WHEN MOD(n, 2) = 1 THEN 5000 ELSE 15000 END,
  NOW(),
  NOW()
FROM seq;
```

## 7. draw-service 재기동

초기화 이후 draw-service를 다시 실행했다.

```bash
cd ~/FCFS_Draw_Service
docker compose -f docker-compose.draw.yml up -d
docker logs -f draw-service
```

정상 기동 로그:

```text
Started DrawServiceApplication
```

## 8. 테스트 상품 생성

로컬 PowerShell에서 상품을 생성했다.

```powershell
$body = @{
  name = "aws-ticket-10000-15s"
  totalQuantity = 100
  price = 10000
} | ConvertTo-Json -Compress

Invoke-RestMethod `
  -Method Post `
  -Uri "http://13.221.46.192:8081/api/v1/products" `
  -ContentType "application/json; charset=utf-8" `
  -Body $body
```

테스트 상품 조건:

```text
상품 수량: 100
상품 가격: 10000
상품 ID: 1
```

## 9. k6 부하 테스트 조건

이번 테스트는 10,000개의 선착순 요청을 15초 동안 발생시키는 조건으로 실행했다.

```powershell
k6 run `
  -e BASE_URL=http://13.221.46.192:8081 `
  -e PRODUCT_ID=1 `
  -e TOTAL_REQUESTS=10000 `
  -e DURATION_SECONDS=15 `
  -e VUS=800 `
  -e MAX_VUS=2000 `
  draw-service/test/load/draw-entry.js
```

k6 스크립트는 `constant-arrival-rate` executor를 사용한다.

```text
rate = ceil(TOTAL_REQUESTS / DURATION_SECONDS)
```

즉 이번 조건에서는 초당 약 667개의 요청을 목표로 한다.

## 10. Grafana에서 사용한 PromQL

```promql
up{job="aws-draw-service"}
```

```promql
draw_queue_size{product_id="1"}
```

```promql
draw_entries{product_id="1",status="PENDING_PAYMENT"}
```

```promql
draw_entries{product_id="1",status="SUCCESS"}
```

```promql
draw_entries{product_id="1",status="FAILED"}
```

```promql
draw_product_quantity{product_id="1",type="remaining"}
```

## 11. Grafana 관측 결과

스크린샷 기준으로 관측된 흐름은 다음과 같다.

```text
23:02:00 부근: 큐 대기 수가 급격히 증가
23:02:10 부근: draw_queue_size가 약 8,000까지 상승
23:02:30 ~ 23:04:00: worker가 큐를 지속적으로 소비
23:04:00 부근: draw_queue_size가 0으로 수렴
23:04:00 이후: FAILED 수가 약 8,000 수준에서 평탄화
SUCCESS는 상품 재고인 100 근처에서 유지
PENDING_PAYMENT는 최종 0 근처로 수렴
remaining quantity는 0으로 유지
```

그래프 해석:

- 녹색 `draw_queue_size`: 순간적으로 큐가 쌓인 뒤 worker가 소비하면서 0까지 내려갔다.
- 파란색 `FAILED`: 큐가 빠지는 동안 최종 실패 처리된 엔트리가 증가했다.
- 노란색 `SUCCESS`: 상품 재고 100개만큼만 성공 처리됐다.
- 주황색 `PENDING_PAYMENT`: 결제 대기 상태가 최종적으로 남지 않았다.
- 분홍색 `remaining`: 재고가 0으로 유지되어 초과 차감이 발생하지 않았다.

## 12. 결과 해석

이번 실험에서 중요한 점은 k6 완료 시점과 시스템 최종 처리 완료 시점이 다르다는 것이다.

k6는 요청 접수 API의 응답을 본다. 하지만 실제 시스템은 다음 단계를 거친다.

```text
HTTP 요청
-> Redis Sorted Set 큐 적재
-> worker polling
-> draw_entries 저장/상태 변경
-> payment-service 결제 요청
-> SUCCESS 또는 FAILED 확정
```

따라서 비동기 큐 기반 구조에서는 다음 조건을 함께 봐야 최종 완료를 판단할 수 있다.

```text
draw_queue_size == 0
draw_entries{status="PENDING_PAYMENT"} == 0
SUCCESS + FAILED 증가가 멈춤
remaining quantity >= 0
```

이번 Grafana 결과에서는 큐가 약 8,000까지 쌓인 뒤 약 2분 내에 0으로 빠졌고, 최종적으로 `FAILED`가 대부분을 차지했다. 상품 재고가 100개였기 때문에 `SUCCESS`는 100 근처에서 제한되는 것이 정상이다.

## 13. 이전 25,000명 테스트에서 얻은 교훈

앞서 25,000명 테스트를 60초 동안 `MAX_VUS=5000`으로 실행했을 때는 부하 주입 자체가 무너졌다.

관측된 k6 결과:

```text
http_req_failed: 64.14%
draw_network_error_count: 7883
dropped_iterations: 12730
http_req_duration p95: 1m0s
```

이는 서버의 도메인 정합성 결과라기보다, 로컬 k6 클라이언트와 네트워크, EC2, Tomcat thread, actuator scrape가 동시에 압박받은 결과로 해석하는 것이 맞다.

특히 Grafana가 테스트 중 metric을 읽지 못한 것도 Prometheus/Grafana 자체 문제라기보다, draw-service가 부하 처리로 바빠 `/actuator/prometheus` 응답을 제때 못 준 상황에 가깝다.

## 14. 결론

이번 테스트에서 확인한 내용은 다음과 같다.

- Redis 큐 기반 선착순 구조에서는 API 응답 시간과 최종 처리 시간이 분리된다.
- Prometheus/Grafana를 붙이면 큐 적재량과 worker 처리 흐름을 시간축에서 확인할 수 있다.
- 재고 100개 조건에서 `SUCCESS`는 100 근처로 제한되고, 잔여 재고는 0 아래로 내려가지 않았다.
- `PENDING_PAYMENT`가 최종 0으로 수렴하면서 결제 Saga의 미완료 상태가 남지 않는 것을 확인했다.
- 지나치게 큰 순간 부하는 서버 검증보다 부하 주입 실패를 먼저 만든다.

다음 테스트에서는 10,000명, 15초 조건을 기준선으로 삼고, 다음 순서로 점진적으로 올리는 것이 안전하다.

```text
10,000 requests / 15s
15,000 requests / 30s
25,000 requests / 60~180s
```

순간 트래픽을 더 크게 만들고 싶다면 로컬 PC 하나에서 k6를 실행하기보다, 별도 EC2에 k6 전용 인스턴스를 두거나 분산 부하 생성 환경을 구성하는 편이 더 정확하다.
