# 🗺️ Architecture & API Design Guide

이 문서는 외부 커머스 시스템과 연동되는 '독립형 선착순 API 모듈(MSA)'의 거시적인 시스템 아키텍처와 API 통신 표준을 정의합니다.

## 1. MSA 및 도메인 분리 (Modular Architecture)
- **독립 모듈:** 본 서버는 외부 시스템(쇼핑몰, 결제 서버 등)에 종속되지 않는 독립적인 트래픽 제어 및 대기열 모듈이다.
- **물리적 외래키 제거:** 회원(Member) 정보는 외부 인증 서버가 관리한다고 가정하며, 본 시스템은 전달받은 `userId`를 '소프트 외래키(Soft FK)'로만 사용하여 느슨한 결합(Loose Coupling)을 유지한다.

## 2. API 및 통신 표준 (API Standards)
- **RESTful 표준 엄수:** API URI에 동사(Verb)를 사용하지 않고 복수형 명사(Noun)를 사용하며, 행위는 HTTP 메서드(GET, POST, PUT, DELETE)로만 구분한다. (예: `POST /api/v1/payments`)
- **공통 응답 포맷 (Common Response):** 외부 시스템이 결과를 쉽게 해석할 수 있도록, 모든 API는 성공/실패 여부, 상태 코드, 데이터 객체를 포함하는 일관된 규격의 JSON 응답 포맷을 반환한다.
- **추적 ID (Trace ID) 전파:** 에러 로깅 및 분산 트랜잭션 추적을 위해, 서버 간 HTTP 통신(OpenFeign 등) 시 반드시 Header에 고유한 추적 ID를 포함하여 요청을 주고받는다.

## 3. 분산 시스템 안전성 (Distributed System Safety)
- **멱등성(Idempotency) 보장:** 네트워크 지연 등으로 동일한 API 요청이 재시도(Retry)되더라도, 서버의 상태(잔액, 재고 등)는 단 한 번만 변경되도록 고유한 `requestId`를 통해 검증한다. (결제 도메인 필수 규칙)