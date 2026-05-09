# ⚡ Concurrency & Transaction Guide

이 문서는 대규모 선착순 트래픽 환경에서 데이터 정합성(재고, 잔액)을 보장하기 위한 정책을 정의합니다. 현재 프로젝트가 어느 단계(Step)에 있는지 확인하고, 해당 단계의 룰을 엄격히 따릅니다.

## 🚧 현재 진행 단계: Step 2 (비관적 락 - Pessimistic Lock 적용)
- **목표:** Step 1에서 발생한 '초과 발주(Lost Update)' 문제를 DB 레벨의 배타락(Exclusive Lock)을 통해 원천 차단하고, 그로 인해 발생하는 성능 저하(커넥션 풀 병목)를 확인한다.
- **적용 규칙:**
  1. 재고 차감(`Product`)을 위해 엔티티를 조회할 때, 반드시 JPA의 `@Lock(LockModeType.PESSIMISTIC_WRITE)`를 사용한다.
  2. 무한 대기(Deadlock) 및 DB 커넥션 풀 고갈을 방지하기 위해, `@QueryHints`를 사용하여 Lock Timeout 시간을 설정한다. (예: `javax.persistence.lock.timeout` = 3000ms)
  3. 락 대기 시간 초과로 예외(`PessimisticLockException` 등)가 발생하면 `@RestControllerAdvice`에서 캐치하여 클라이언트에게 "현재 접속자가 많아 처리가 지연되고 있습니다."라는 메시지와 함께 예외 처리한다.

## 🔄 트랜잭션 및 롤백 정책 (Transaction & Rollback)
- 재고 차감(`Product`)과 잔액 차감(`Wallet`)은 반드시 **하나의 `@Transactional` 안에서 원자적(Atomic)**으로 묶여야 한다.
- 결제 진행 중 `Wallet`의 잔액 부족으로 인해 `IllegalArgumentException`이 발생할 경우, 트랜잭션이 롤백되어 앞서 차감된 `Product`의 재고가 원래 상태로 복구되도록 설계해야 한다. 
- 이를 통해 실패한 요청의 재고가 **다음 대기자에게 정상적으로 넘어가도록** 자연스러운 흐름을 보장한다.

## 🗺️ 향후 도입 예정 기술 (참고용 - 지금 구현하지 말 것)
- Step 3: DB 커넥션 한계를 극복하기 위한 Redis 분산 락 (Redisson) 도입
- Step 4: Redis Lua Script 기반 비동기 대기열 구축 (초고속 TPS 달성) -> 이건 안할수도 있음