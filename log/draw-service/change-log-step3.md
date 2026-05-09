# Draw Service Step 3 Change Log

## 2026-05-10 01:54:58 +09:00 - Redisson 분산 락 전환

### 1. 발생 상황
- Step 2 비관적 락 적용 후 재고 정합성은 맞았지만, 상품 row lock 대기열 때문에 p95 응답 시간이 크게 증가했다.
- 2000명 이상 부하에서는 서버 스레드가 오래 대기하며 연결 실패까지 발생해, DB 락 기반 동시성 제어가 병목이 되었다.

### 2. 원인
- MySQL row lock은 트랜잭션 대기 요청을 DB와 애플리케이션 스레드에 오래 붙잡아 두는 구조라 대규모 선착순 트래픽에 불리하다.
- 기존 구현은 `ProductRepository`의 JPA 비관적 락으로 재고 차감 직전 DB row를 잠그고 있어 요청이 빠르게 실패하지 못했다.

### 3. 조치
- `ProductRepository`의 `@Lock`, `@QueryHints` 기반 조회 메서드를 제거하고 일반 조회로 되돌렸다.
- `DrawEntryFacade`를 추가해 `product:{productId}` 이름의 Redisson 분산 락을 비즈니스 서비스 바깥에서 잡도록 분리했다.
- `tryLock(100ms, 10000ms)`로 대기 시간을 짧게 두어 락 획득 실패 시 빠르게 `LOCK_TIMEOUT` 응답으로 연결되도록 했다.
- 컨트롤러가 Facade를 호출하고, 내부 `DrawEntryService.draw`의 `@Transactional`이 커밋된 뒤 Facade의 `finally`에서 락을 해제하도록 트랜잭션 경계를 정리했다.

### 4. 검증
- `.\gradlew :draw-service:test` 성공
- `.\gradlew clean build` 성공

### 5. 남은 확인 사항
- Redis Docker 컨테이너를 실행한 상태에서 k6를 재실행해 네트워크 실패와 `LOCK_TIMEOUT` 비율을 구분한다.
- 현재 100ms fail-fast 설정이 선착순 UX에 적절한지 부하 결과를 보고 조정한다.
