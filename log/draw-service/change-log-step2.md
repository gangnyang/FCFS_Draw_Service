# Draw Service Step 2 Change Log

## 2026-05-09 21:10:00 KST - Step 2 비관적 락 적용

### 1. 발생 상황
- k6 부하 테스트에서 재고보다 많은 사용자가 `WIN` 처리되는 초과 발주 문제가 발생했다.
- 동시 요청이 같은 상품 재고 값을 동시에 읽고 각각 차감하면서 갱신 손실이 발생했다.

### 2. 원인
- Step 1 구현은 재고 차감 시 `Product`를 일반 조회로 가져왔고, DB 레벨 배타락이 없어 같은 행을 여러 트랜잭션이 동시에 수정할 수 있었다.

### 3. 조치
- `ProductRepository.findWithPessimisticLockById`에 `@Lock(PESSIMISTIC_WRITE)`를 적용해 재고 차감 대상 상품 행에 배타락을 걸었다.
- `@QueryHints`로 lock timeout을 3000ms로 설정해 락 대기 중 무한 대기와 커넥션 풀 고갈을 줄이도록 했다.
- `DrawEntryService.createDraw`가 재고 차감 시 일반 조회 대신 락 조회를 사용하도록 변경했다.
- 락 획득 실패 계열 예외를 `GlobalExceptionHandler`에서 잡아 `LOCK_TIMEOUT` 코드와 "현재 접속자가 많아 처리가 지연되고 있습니다." 메시지로 반환하도록 했다.

### 4. 검증
- `.\gradlew :draw-service:test` 성공
- `.\gradlew :draw-service:build -x test` 성공

### 5. 남은 확인 사항
- MySQL Docker 환경에서 k6를 재실행해 `WIN` 수가 실제 재고 수를 넘지 않는지 확인한다.
- lock timeout 발생 시 클라이언트 응답이 `LOCK_TIMEOUT`으로 내려오는지 부하 상황에서 확인한다.

## 2026-05-09 21:25:00 KST - k6 실패 유형 분리

### 1. 발생 상황
- k6 summary에서 HTTP 성공률은 보이지만, 선착순 품절 실패와 DB/락/서버 오류를 구분하기 어려웠다.

### 2. 원인
- 기존 스크립트는 `WIN`, `LOSE`, 일부 conflict 정도만 집계했고, 공통 응답의 `code`와 HTTP status를 세분화하지 않았다.

### 3. 조치
- `LOCK_TIMEOUT`, `INVALID_REQUEST`, `PRODUCT_NOT_FOUND`, `NOT_FOUND`, `INTERNAL_SERVER_ERROR`, 네트워크 오류, JSON 파싱 오류를 각각 커스텀 카운터로 분리했다.
- HTTP 4xx/5xx 카운터를 추가해 통신/서버 장애와 비즈니스 `LOSE`를 구분할 수 있게 했다.
- `LOG_FAILURES=true` 환경 변수를 주면 실패 상세 로그를 콘솔에 출력하도록 했다.

## 2026-05-09 21:35:00 KST - k6 커스텀 카운터 표시 보강

### 1. 발생 상황
- k6 summary의 `CUSTOM` 영역에 실제 값이 증가한 일부 카운터만 표시되어, 실패 유형 전체를 한눈에 보기 어려웠다.

### 2. 원인
- k6는 테스트 중 샘플이 기록되지 않은 커스텀 metric을 summary에 표시하지 않는다.

### 3. 조치
- 각 iteration 시작 시 모든 커스텀 카운터에 `0` 샘플을 기록해, 값이 0인 실패 유형도 summary에 표시될 수 있도록 보강했다.

## 2026-05-09 21:45:00 KST - 락 타임아웃 예외 분류 보강

### 1. 발생 상황
- Step 2 부하 테스트 중 락 지연으로 보이는 실패가 `draw_lock_timeout_count`가 아니라 `draw_server_error_count`로 집계되었다.

### 2. 원인
- DB/드라이버/Hibernate/Spring 계층에서 락 타임아웃이 여러 예외 타입으로 감싸질 수 있는데, 기존 핸들러가 일부 Spring DAO 예외만 처리했다.
- 전역 500 로그가 root cause 타입을 별도로 출력하지 않아 실제 실패 원인을 구분하기 어려웠다.

### 3. 조치
- `LockTimeoutException`, `PessimisticLockException`, `JpaSystemException`, `PersistenceException`, `QueryTimeoutException`, `TransactionSystemException` 등을 락 지연 응답 처리 대상으로 추가했다.
- 락 예외와 일반 500 예외 로그에 최상위 예외 타입과 root cause 타입/메시지를 함께 출력하도록 보강했다.

## 2026-05-09 22:00:00 KST - DB 락 대기 타임아웃 설정으로 정정

### 1. 발생 상황
- 1000 VU 부하 테스트에서 `WIN` 수는 재고 수로 제한되었지만, 락 대기 요청이 모두 오래 기다린 뒤 `SOLD_OUT`으로 처리되어 `LOCK_TIMEOUT` 신호가 나타나지 않았다.
- 애플리케이션 트랜잭션 전체에 `timeout = 3`을 거는 방식은 DB row lock 획득 실패만 관찰하기에는 인위적이다.

### 2. 원인
- MySQL/Hibernate 환경에서 JPA `jakarta.persistence.lock.timeout` hint가 기대처럼 lock wait을 3초 안에 강제 종료하지 않을 수 있다.
- 이 경우 요청은 DB row lock 대기열에서 오래 기다린 뒤 순차적으로 처리되어 p95 latency만 증가한다.
- Spring `@Transactional(timeout = 3)`은 락 획득 대기뿐 아니라 트랜잭션 전체 수행 시간을 제한한다.

### 3. 조치
- `DrawEntryService.draw`의 트랜잭션 timeout 설정을 제거해 인위적인 애플리케이션 레벨 제한을 되돌렸다.
- MySQL 세션의 실제 row lock 대기 제한을 낮추기 위해 `application-prod.yml`에 Hikari `connection-init-sql: SET innodb_lock_wait_timeout=${DB_LOCK_WAIT_TIMEOUT_SECONDS:3}` 설정을 추가했다.
- `TransactionTimedOutException`은 락 획득 실패 분류 대상에서 제거했다.
