# draw-service Prometheus 지표 연동

## 2026-05-11 22:10:48 +09:00

### 1. 발생 상황
- AWS 부하 테스트에서 k6가 측정하는 API 접수 시간과 Redis 큐/worker/결제까지 포함한 최종 처리 시간이 분리되어 있었다.
- Grafana/Prometheus로 큐 잔량, 상태별 처리 수, 상품 잔여 수량을 관찰할 수 있는 지표가 필요했다.

### 2. 원인
- 기존 draw-service에는 Actuator/Prometheus endpoint가 없었고, 도메인 처리 상태를 노출하는 Micrometer 지표도 없었다.
- 특히 완료 판단에 필요한 `PENDING_PAYMENT=0`, `queueSize=0` 같은 값은 DB/Redis에 직접 접속해야만 확인할 수 있었다.

### 3. 조치 내용
- draw-service에 `spring-boot-starter-actuator`와 `micrometer-registry-prometheus` 의존성을 추가했다.
- `health,prometheus` actuator endpoint를 노출하고 `/actuator/prometheus`에서 지표를 조회할 수 있게 설정했다.
- 상품별 `draw_entries{product_id,status}`, `draw_queue_size{product_id}`, `draw_product_quantity{product_id,type}` 지표를 주기적으로 갱신하는 `DrawQueueMetrics`를 추가했다.
- 상태별 카운트는 상품별로 `PENDING_PAYMENT/SUCCESS/FAILED`의 0 값까지 채워 Grafana 쿼리에서 완료 상태를 안정적으로 판단할 수 있게 했다.
