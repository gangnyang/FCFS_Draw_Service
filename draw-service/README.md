# Draw Service

선착순 요청 처리 서버가 들어갈 모듈 자리입니다.

현재 이 모듈은 상품 재고와 선착순 시도 이력을 관리합니다.

## 주요 API

- `GET /`: 서버 상태 확인
- `GET /api/v1/health`: 서버 상태 확인
- `POST /api/v1/products`: 선착순 대상 상품 생성
- `GET /api/v1/products/{productId}`: 상품 재고 조회
- `POST /api/v1/draws`: 선착순 시도

## 현재 동시성 단계

concurrency-guide의 Step 3에 맞춰 상품 단위 Redisson 분산 락을 사용합니다.
요청은 `product:{productId}` 락을 짧게 시도하고, 락 획득에 실패하면 빠르게 지연 응답을 반환합니다.
