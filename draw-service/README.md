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

concurrency-guide의 Step 1에 맞춰 재고 차감에는 `@Lock`, `@Version`, Redis 원자 연산을 넣지 않았습니다.
동시 요청에서 Lost Update 또는 oversell 현상을 관찰하기 위한 의도적인 무방비 상태입니다.
