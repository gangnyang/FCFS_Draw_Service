# ⚡ Concurrency & Transaction Guide

이 문서는 대규모 선착순 트래픽 환경에서 데이터 정합성(재고, 잔액)을 보장하기 위한 정책을 정의합니다.

## 🚧 현재 진행 단계: Step 4 (Redis 비동기 대기열 구축)
- **목표:** Step 3(분산 락)의 Fail-Fast로 인한 '무한 재시도(광클)' 및 '서버 입구 컷(Connection Refused)' 문제를 해결한다. Redis의 `Sorted Set (ZSET)`을 활용하여 유저를 대기열에 세우고, 백그라운드 워커가 순차적으로 처리하는 비동기 아키텍처를 구현한다.
- **아키텍처 적용 규칙:**
  1. **[기존 락 제거]** Step 3에서 작성한 `DrawEntryFacade` 로직을 걷어내고, 컨트롤러가 비즈니스 로직(DB 접근)을 직접 호출하지 못하게 차단한다.
  2. **[대기열 진입 - Producer]**
     - 유저가 선착순 요청을 하면 DB로 가지 않고 Redis `ZSET`에 유저 정보를 적재한다.
     - Key: `draw:queue:{productId}`
     - Value: `userId` (또는 requestId)
     - Score: `System.currentTimeMillis()` (진입 시간 기준 오름차순 정렬)
     - 진입 직후 클라이언트에게 `202 Accepted` 상태 코드와 함께 대기열 진입 성공 응답을 보낸다.
  3. **[백그라운드 처리 - Consumer]**
     - Spring `@Scheduled`를 사용하여 1초마다 동작하는 Worker를 만든다.
     - Worker는 Redis ZSET에서 상위 N명(예: 100명)을 꺼내어(pop) 기존 `drawEntryService.draw()` (DB 재고 차감) 로직을 순차적으로 실행한다.
  4. **[상태 조회 API]**
     - 클라이언트가 자신의 대기 순번을 확인할 수 있도록 Redis `ZRANK` 명령어를 활용한 상태 조회 API(`GET /draw/status`)를 제공한다.

## 🔄 트랜잭션 및 정합성 정책
- 대규모 트래픽(수천 명)은 Redis 메모리에서 모두 흡수하므로 DB 병목은 사라진다.
- DB 접근은 오직 스케줄러(Worker) 1명만 통제된 속도(초당 N건)로 수행하므로, 별도의 분산 락(Redisson) 없이도 안전하게 처리 가능하다. (트랜잭션 범위는 Service 계층 유지)
