# Draw Service Step 4 Change Log

## 2026-05-10 13:12:59 +09:00 - Redis 비동기 대기열 전환

### 1. 발생 상황
- Step 3 Redisson 분산 락은 빠른 실패를 만들었지만, 2000명 이상 동시 진입 시 톰캣 입구에서 연결 실패가 대량 발생했다.
- 유저 입장에서는 서버가 요청을 받지 못해 무한 재시도를 해야 하는 UX가 되었다.

### 2. 원인
- 모든 요청이 즉시 DB 처리 또는 락 획득 경쟁까지 진입하면서, 짧은 시간에 서버 스레드와 연결 수용 한계에 부딪혔다.
- Fail-fast는 DB 대기를 줄이지만, 톰캣 입구로 몰려드는 요청량 자체를 흡수하지는 못한다.

### 3. 조치
- 컨트롤러를 Producer로 바꿔 Redis ZSET 대기열에 요청을 적재하고 즉시 `202 Accepted`를 반환하도록 변경했다.
- `DrawQueueService.enqueue(productId, userId)`는 `draw:queue:{productId}` ZSET에 `userId`를 value로, 현재 시각(ms)을 score로 저장한다.
- `DrawQueueService.getRank(productId, userId)`는 ZRANK 기반 순번을 1부터 시작하는 값으로 반환한다.
- `DrawQueueWorker`를 추가해 1초마다 상품별 상위 50명을 pop하고 기존 `DrawEntryService.draw()`로 DB 처리를 수행하도록 분리했다.
- `GET /draw/queue/rank` 순번 조회 API를 추가했다.

### 4. 검증
- `.\gradlew :draw-service:test` 성공
- `.\gradlew clean build` 성공

### 5. 남은 확인 사항
- Redis Docker 컨테이너를 실행한 상태에서 k6를 재실행해 `draw_queued_count`, 네트워크 실패율, 워커 처리 속도를 확인한다.
- pop 이후 DB 처리 실패 시 재시도/복구 정책은 아직 없다.
