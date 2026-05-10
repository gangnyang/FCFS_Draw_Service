# Draw Service Step 5 Change Log

## 2026-05-10 14:21:20 +09:00 - MSA 결제 연동 및 보상 트랜잭션 전환

### 1. 발생 상황
- Redis 대기열 Worker가 선착순 대상자를 꺼낸 뒤 결제 서버와 연동해야 한다.
- 재고 차감 트랜잭션 안에서 외부 결제 API를 호출하면 결제 서버 지연 동안 DB 커넥션과 트랜잭션을 오래 붙잡을 수 있다.

### 2. 원인
- 외부 HTTP 호출은 네트워크 지연, 결제 서버 오류, 잔액 부족 등으로 실패하거나 느려질 수 있다.
- 하나의 DB 트랜잭션 안에서 재고 차감과 외부 결제 호출을 묶으면 DB 커넥션 고갈과 장시간 락 점유 위험이 커진다.

### 3. 조치
- `DrawEntry.status`를 추가하고 `PENDING_PAYMENT`, `SUCCESS`, `FAILED` 상태를 정의했다.
- `DrawPaymentSagaService`를 추가해 재고 선점 Tx, 외부 결제 호출 No Tx, 성공/보상 Tx를 분리했다.
- `PaymentClient`를 추가해 `RestTemplate`으로 payment-service의 `POST /api/v1/payments`를 호출하도록 했다.
- 결제 실패 시 `compensatePayment`가 `DrawEntry`를 `FAILED`로 바꾸고 `Product` 재고를 다시 +1 복구하도록 했다.

### 4. 검증
- `.\gradlew :draw-service:test` 성공

### 5. 남은 확인 사항
- 실제 payment-service를 실행한 상태에서 Worker가 `SUCCESS` 상태로 마무리하는지 통합 확인이 필요하다.
- 결제 서버가 응답하지 않는 장시간 장애에서는 재시도 큐 또는 dead-letter queue 정책이 추가로 필요하다.
