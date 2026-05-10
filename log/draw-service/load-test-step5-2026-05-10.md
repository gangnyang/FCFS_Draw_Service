# Step 5 부하 테스트 결과

## 테스트 조건
- 테스트 기준 시각: 2026-05-10 KST
- 구조: draw-service는 Redis ZSET 대기열에 요청을 적재하고, `DrawQueueWorker`가 1초마다 상위 요청을 꺼내 DB 처리와 payment-service 결제를 수행한다.
- 상품 조건: 상품 1개, 가격 10000원, 총 재고 100개
- 결제 데이터: wallet 2000개
- 잔액 분포: 5000원 지갑 1000개, 15000원 지갑 1000개로 시작
- 테스트 후 관측된 잔액 분포: 5000원 지갑 1100개, 15000원 지갑 900개
- 성공 조건: 결제 성공 수와 최종 성공 draw 수가 재고 100개를 넘지 않아야 한다.

## 테스트 내용
- draw DB의 `products`, `draw_entries`를 초기화했다.
- payment DB의 `wallets`, `payment_transactions`를 초기화하고 2000명의 지갑 데이터를 다시 적재했다.
- Redis draw queue를 초기화했다.
- k6로 draw-service의 대기열 진입 API에 부하를 주고, 백그라운드 worker가 큐를 소비하도록 대기했다.
- 테스트 종료 후 payment DB와 draw DB를 SQL로 직접 조회했다.

## 테스트 결과
- `payment_transactions`는 100건 생성되었다.
- `wallets`는 총 2000건 유지되었다.
- `wallets.balance = 5000`은 1100건, `wallets.balance = 15000`은 900건으로 확인되었다.
- `draw_entries` 집계 결과는 다음과 같다.

| result | status | fail_reason | count |
| --- | --- | --- | ---: |
| WIN | FAILED | NULL | 1000 |
| WIN | SUCCESS | NULL | 100 |
| LOSE | FAILED | SOLD_OUT | 454 |

- `products`의 최종 상태는 `total_quantity = 100`, `remaining_quantity = 0`, `status = SOLD_OUT`이다.

## 판정
- 결제 성공 건수 100건과 draw 성공 건수 100건이 재고 100개와 일치하므로 초과 발주는 발생하지 않았다.
- 잔액 15000원 사용자가 900명으로 줄고 잔액 5000원 사용자가 1100명으로 늘어, 결제 성공 100명에게서 10000원씩 정상 차감된 것으로 확인된다.
- 결제 실패 보상 트랜잭션 자체는 동작했지만, `WIN/FAILED` 1000건의 `fail_reason`이 `NULL`로 남아 실패 원인 추적성이 부족했다.
- 후속 조치로 결제 실패 보상 시 `PAYMENT_FAILED` 사유를 저장하도록 변경했다.
