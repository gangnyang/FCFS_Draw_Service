## 2026-05-09 17:15:00 KST - 선착순 요청 처리 서버 변경 로그 위치 준비

### 1. 발생 상황
- 선착순 요청 처리 서버는 아직 구현 전이지만, 결제 서버와 독립적으로 변경 이력을 관리할 위치가 필요하다.

### 2. 원인
- 향후 대기열, 재고, 선착순 요청 수락/거절 로직이 추가되면 결제 서버 변경 이력과 분리해서 추적해야 한다.

### 3. 조치
- `draw-service/log/change-log.md` 파일을 추가했다.
- 앞으로 선착순 요청 처리 서버 변경 사항은 이 파일에 기록하도록 구조를 분리했다.

## 2026-05-09 17:25:00 KST - 루트 로그 폴더 기준 서버별 로그 구조로 변경

### 1. 발생 상황
- 선착순 요청 처리 서버 변경 로그도 루트 `log` 폴더 아래에서 결제 서버 로그와 함께 확인될 필요가 있다.

### 2. 원인
- 서버 폴더 내부에 로그를 두면 여러 서버의 변경 이력을 확인하기 위해 각 서버 디렉터리를 따로 열어야 한다.

### 3. 조치
- 선착순 요청 처리 서버 변경 로그를 `log/draw-service/change-log.md` 위치로 이동했다.
- 앞으로 선착순 요청 처리 서버 변경 사항은 루트 `log/draw-service/change-log.md`에 기록하도록 정리했다.

## 2026-05-09 18:55:00 KST - 선착순 요청 처리 서버 초기 구축

### 1. 발생 상황
- 결제 서버 구축 이후, 선착순 성공/실패만 판단하는 독립 draw-service가 필요했다.
- draw-service는 payment-service와 분리되어 상품 재고와 선착순 시도 이력을 자체 DB로 관리해야 한다.

### 2. 원인
- 선착순 재고 차감은 payment-service의 잔액 차감과 별도 책임이며, 이후 두 서버를 연계하려면 draw-service가 성공/실패 결과를 명확히 반환해야 한다.
- 현재 동시성 지침은 Step 1이므로 재고 차감에도 락이나 버전 필드를 넣지 않고 Lost Update를 관찰할 수 있어야 한다.

### 3. 조치
- `draw-service`를 Gradle 멀티모듈에 등록하고 Spring Boot 서버 골격을 추가했다.
- `products` 테이블을 기준으로 상품명, 총 수량, 남은 수량, 가격, 상태를 관리하도록 `Product` 도메인을 추가했다.
- `draw_entries` 테이블을 기준으로 `requestId`, `productId`, `userId`, 결과, 실패 사유를 기록하도록 `DrawEntry` 도메인을 추가했다.
- `POST /api/v1/products`, `GET /api/v1/products/{productId}`, `POST /api/v1/draws`, health API를 추가했다.
- 같은 `requestId`는 멱등 응답을 반환하고, 같은 `requestId`에 다른 payload가 들어오면 `IDEMPOTENCY_CONFLICT`로 실패 처리하도록 구현했다.
- 같은 사용자가 같은 상품에 여러 번 시도하면 기존 결과를 반환해 재고가 중복 차감되지 않도록 했다.
- draw-service 전용 Dockerfile, H2/local 기본 설정, prod 환경 변수 설정, k6 부하 테스트 스크립트, 단위/통합 테스트를 추가했다.
- 현재 단계 지침에 맞춰 `@Lock`, `@Version`, Redis 원자 연산 등 동시성 제어 코드는 추가하지 않았다.

## 2026-05-09 20:35:00 KST - 콘솔 로그 UTF-8 출력 설정 추가

### 1. 발생 상황
- VS Code 터미널에서 draw-service 로그의 한글 예외 메시지가 깨져 보였다.

### 2. 원인
- Windows 터미널의 코드페이지와 JVM 표준 출력 인코딩이 UTF-8로 맞춰지지 않으면, 애플리케이션 메시지는 정상이어도 콘솔 표시가 깨질 수 있다.

### 3. 조치
- draw-service의 Java 컴파일, 테스트, bootRun JVM 인코딩을 UTF-8로 고정했다.
- draw-service의 console/file logging charset을 UTF-8로 설정했다.

## 2026-05-09 20:50:00 KST - k6 선착순 결과 집계 로그 개선

### 1. 발생 상황
- k6 테스트에서 재고가 없어 `LOSE`가 반환되어도 HTTP 200 정상 응답이므로 성공률은 100%로 표시된다.
- 사용자는 성공률과 별개로 선착순 성공/거절 사유를 확인하고 싶었다.

### 2. 원인
- 기존 k6 스크립트는 draw-service의 공통 응답 구조인 `success`, `code`, `data.result`, `data.failReason`을 정확히 파싱하지 않았다.
- HTTP 성공 여부와 비즈니스 결과 집계가 분리되어 있지 않았다.

### 3. 조치
- k6 스크립트에 `draw_win_count`, `draw_lose_count`, `draw_sold_out_count`, `draw_already_entered_count`, `draw_idempotency_conflict_count` 커스텀 카운터를 추가했다.
- `LOG_REJECTIONS=true` 환경 변수를 주면 거절된 요청의 사유를 콘솔에 개별 출력하도록 했다.
- HTTP 응답 성공률은 유지하고, 선착순 비즈니스 결과는 별도 지표로 확인하도록 분리했다.
