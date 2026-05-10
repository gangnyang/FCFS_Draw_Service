# ⚡ Concurrency & Transaction Guide

## 🚧 현재 진행 단계: Step 5 (MSA 결제 연동 및 보상 트랜잭션 - Saga Pattern)
- **목표:** Redis 대기열에서 빠져나온 유저의 결제를 처리할 때, 재고 차감 트랜잭션과 결제 서버 통신을 완전히 분리한다. 결제 실패 시 보상 트랜잭션(Compensation)을 통해 재고를 원복한다.
- **아키텍처 흐름 (Choreography Saga):**
  1. **[재고 선점 - Tx 1]** 대기열 Worker가 유저를 꺼내면, 즉시 DB 트랜잭션을 열고 재고를 1 차감한 뒤, 주문 상태를 `PENDING_PAYMENT(결제 대기)`로 저장하고 트랜잭션을 커밋한다.
  2. **[외부 결제 호출 - No Tx]** 트랜잭션이 없는 상태에서 외부 Payment API(Mock)를 호출한다.
  3. **[결과 처리 - Tx 2]**
     - **결제 성공:** 새로운 DB 트랜잭션을 열어 주문 상태를 `SUCCESS(결제 완료)`로 변경한다.
     - **결제 실패 (잔액 부족 등):** 새로운 DB 트랜잭션(보상 트랜잭션)을 열어 주문 상태를 `FAILED(결제 실패)`로 변경하고, **차감했던 재고를 다시 +1 증가**시킨다.
- **적용 규칙:**
  - 절대 `@Transactional` 블록 안에서 외부 API(RestTemplate/WebClient/FeignClient 등)를 호출하지 않는다. (DB 커넥션 고갈 방지)