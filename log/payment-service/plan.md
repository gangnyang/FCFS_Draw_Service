# Payment Service Plan

## 2026-05-09 - 예약 기반 결제 흐름 도입 계획

### 1. 배경
- 현재 결제 서버는 `POST /api/v1/payments` 요청 시 사용자 지갑 잔액을 즉시 차감하고 결제 기록을 남긴다.
- draw-service와 아직 연계하지 않은 단독 결제 서버 단계에서는 단순하고 충분하지만, 선착순 재고 확정과 결제 확정이 서로 다른 서버에서 일어나면 분산 트랜잭션 문제가 생길 수 있다.
- 특히 선착순 성공 여부가 확정되기 전에 실제 잔액을 차감하면, draw-service 실패 시 환불 또는 보상 트랜잭션이 필요해진다.

### 2. 목표
- 결제 서버를 `잔액 즉시 차감` 방식에서 `예약 후 확정` 방식으로 확장한다.
- draw-service는 재고/당첨 확정 전에 결제 가능 금액을 먼저 예약하고, 재고 차감 성공 후 결제를 확정한다.
- 재고 차감 실패 또는 타임아웃이 발생하면 예약 금액을 해제한다.

### 3. 권장 흐름
```text
1. payment-service: reserve
   - 사용자 잔액 중 상품 금액을 예약한다.
   - 실제 balance는 아직 줄이지 않고 reservedAmount를 증가시킨다.

2. draw-service: stock deduct / winner confirm
   - 재고가 남아 있으면 선착순 당첨 또는 구매 가능 상태를 확정한다.

3. payment-service: capture
   - 예약 금액을 실제 차감 확정한다.
   - balance를 줄이고 reservedAmount를 감소시킨다.

4. 실패 시 보상
   - draw-service 재고 차감 실패: cancel reservation
   - capture 이후 후속 처리 실패: refund 또는 주문 재시도
```

### 4. 도메인 모델 확장안
```text
wallet
- id
- user_id
- balance
- reserved_amount
- created_at
- updated_at

payment_reservation
- id
- reservation_id
- request_id
- user_id
- amount
- status: RESERVED / CAPTURED / CANCELED / REFUNDED
- created_at
- updated_at
```

### 5. API 확장안
```text
POST /api/v1/payment-reservations
- 목적: 잔액 확인 및 금액 예약
- 성공 조건: balance - reservedAmount >= amount

POST /api/v1/payment-reservations/{reservationId}/capture
- 목적: 예약 금액 실제 차감 확정
- 성공 조건: status == RESERVED

POST /api/v1/payment-reservations/{reservationId}/cancel
- 목적: draw-service 실패 또는 타임아웃 시 예약 해제
- 성공 조건: status == RESERVED

POST /api/v1/payments/{paymentId}/refund
- 목적: 이미 확정된 결제의 보상 환불
- 성공 조건: status == CAPTURED
```

### 6. 주의할 점
- `cancel`은 아직 실제 차감 전의 예약 해제이고, `refund`는 이미 차감된 금액을 복구하는 별도 보상 트랜잭션이다.
- 사용자 경험과 정산 복잡도를 고려하면 가능한 한 `refund`보다 `cancel reservation` 경로를 우선한다.
- 같은 `requestId` 또는 `reservationId` 요청은 멱등하게 처리해야 한다.
- draw-service와 연계할 때는 각 API 호출에 `X-Trace-Id`를 전달해 서버 간 로그를 이어서 추적한다.
- 현재 concurrency-guide Step 1에서는 의도적으로 락을 넣지 않으므로, 실제 정합성 강화 단계에서 비관적 락, 낙관적 락, Redis 원자 연산 중 어떤 전략을 적용할지 별도 검토한다.

### 7. 향후 작업 순서
1. `Wallet`에 `reservedAmount` 추가 및 사용 가능 잔액 계산 메서드 도입
2. `PaymentReservation` 엔티티와 상태 enum 추가
3. reserve/capture/cancel API 추가
4. 기존 즉시 차감 `POST /api/v1/payments` API 유지 여부 결정
5. 예약 중복 요청, capture 중복 요청, cancel 중복 요청 테스트 추가
6. draw-service 연계 후 실패 지점별 보상 시나리오 테스트 추가
