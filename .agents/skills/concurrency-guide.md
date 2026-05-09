# ⚡ Concurrency & Transaction Guide

이 문서는 대규모 선착순 트래픽 환경에서 데이터 정합성(재고, 잔액)을 보장하기 위한 정책을 정의합니다. 현재 프로젝트가 어느 단계(Step)에 있는지 확인하고, 해당 단계의 룰을 엄격히 따릅니다.

## 🚧 현재 진행 단계: Step 3 (Redisson 분산 락 및 Fail-Fast 적용)
- **목표:** Step 2의 비관적 락으로 인해 발생한 DB 병목 및 타임아웃 문제를 해결한다. 무거운 MySQL 락을 가벼운 Redis 분산 락으로 교체하고, '빠른 실패(Fail-Fast)'를 통해 서버 스레드를 방어한다.
- **적용 규칙:**
  1. **[JPA 락 제거]** `ProductRepository` 등에 걸려있던 JPA `@Lock(LockModeType.PESSIMISTIC_WRITE)` 및 `@QueryHints`를 모두 제거하고 순수 조회로 되돌린다.
  2. **[Redisson Facade 패턴]** 비즈니스 로직(Service) 바깥에 분산 락을 제어하는 Facade 클래스(또는 AOP)를 분리한다. (반드시 **Lock 획득 👉 트랜잭션 시작 👉 로직 실행 👉 트랜잭션 커밋 👉 Lock 해제** 순서가 보장되어야 한다.)
  3. **[Fail-Fast 설정]** `tryLock(waitTime, leaseTime, TimeUnit)` 사용 시, `waitTime`을 **0.1초 (100ms)** 내외로 아주 짧게 설정하여 유저가 길게 대기하지 않도록 한다.
  4. **[예외 처리]** Lock 획득 실패 시 (대기 시간 초과 시) 즉시 예외(예: `ConcurrencyFailureException`)를 던지고, `@RestControllerAdvice`에서 "현재 접속자가 많아 처리가 지연되고 있습니다."라는 메시지(CommonResponse FAIL)로 부드럽게 응답한다.

## 🔄 트랜잭션 및 롤백 정책 (Transaction & Rollback)
- 재고 차감과 잔액 차감 로직은 여전히 하나의 `@Transactional` 안에서 원자적(Atomic)으로 묶여야 한다. (Facade 클래스가 아닌 내부 Service 클래스에 트랜잭션 적용)
- 실패/예외 발생 시 롤백되어 데이터가 원래 상태로 복구되는 흐름을 유지한다.

## 🗺️ 향후 도입 예정 기술 (참고용 - 지금 구현하지 말 것)
- Step 4: Redis Lua Script 기반 비동기 대기열 구축 (대규모 트래픽 UX 개선용)