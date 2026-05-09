# 🏛️ Code Style & Architecture Guide

이 문서는 외부 시스템과 연동 가능한 '독립형 선착순 API 모듈(MSA)'을 구축하기 위한 아키텍처 및 코드 스타일 가이드라인입니다. AI 에이전트는 코드를 작성할 때 이 규칙을 100% 준수해야 합니다.

## 1. MSA 및 모듈형 아키텍처 (Modular Architecture)
- **독립적인 도메인:** 본 서버는 외부 쇼핑몰이나 예매 시스템에 종속되지 않는 '독립된 트래픽 제어 및 선착순 결제 모듈'로 동작해야 한다. 
- **표준화된 API 응답:** 외부 시스템이 이 API를 쉽게 호출하고 결과를 해석할 수 있도록, 성공/실패 여부와 에러 코드를 포함하는 **공통 응답 포맷(Common Response)**을 설계하여 반환한다.
- **명확한 계층 분리:** Controller(HTTP 요청/응답 및 DTO 매핑), Service(핵심 비즈니스 로직 및 트랜잭션 관리), Repository(순수 DB 접근)의 역할을 엄격히 분리한다.

## 2. 의존성 주입 및 객체 지향 (DI & OOP)
- **생성자 주입:** `@Autowired` 필드 주입은 절대 금지하며, 무조건 Lombok의 `@RequiredArgsConstructor`를 이용한 `final` 필드 생성자 주입을 사용한다.
- **엔티티 캡슐화:** 데이터의 상태(잔액, 재고 등)를 변경하는 로직은 Service 계층이 아닌 **Entity 클래스 내부의 메서드**(예: `wallet.deduct()`)로 캡슐화하여 구현한다.

## 3. 코드 품질 및 안전성 (Code Quality & Safety)
- **매직 넘버/스트링 금지:** 코드 내의 의미를 알 수 없는 숫자나 문자열은 반드시 `private static final` 상수로 추출하여 의미를 명확히 한다.
- **NPE 및 예외 방지:** DB에서 단건 객체를 조회할 때는 반드시 `Optional<T>`로 감싸서 반환하고, `orElseThrow()`를 통해 명시적인 커스텀 예외를 발생시킨다.
- **DTO 사용 강제:** Controller와 Service 간 데이터 전달, 혹은 클라이언트 응답 시 Entity를 절대 직접 노출하지 않고 반드시 DTO(`record` 적극 권장)를 사용한다.
- **로깅 정책:** `System.out.println()` 사용을 엄격히 금지한다. 예외 상황이나 중요 비즈니스 흐름은 `@Slf4j`를 사용하여 상황에 맞는 로그 레벨(info, warn, error)로 기록한다.

## 4. JPA 및 데이터베이스 (JPA & Database)
- **Lombok 제한:** 엔티티 클래스에서 무한 참조를 유발할 수 있는 `@Data`와 `@ToString` 사용을 절대 금지한다. (`@Getter`와 `@NoArgsConstructor(access = AccessLevel.PROTECTED)` 위주로 사용)
- **자동 타임스탬프:** 모든 엔티티는 생성 시간과 수정 시간이 자동으로 기록되도록 JPA Auditing(`@CreatedDate`, `@LastModifiedDate`)이 적용된 `BaseEntity`를 상속받는다.
- **N+1 문제 해결:** 연관된 엔티티를 함께 조회하는 경우, 지연 로딩(Lazy Loading)으로 인한 N+1 문제가 발생하지 않도록 Repository 인터페이스에서 `Fetch Join` 또는 `@EntityGraph`를 명시적으로 사용한다.

## 5. 트랜잭션 관리 (Transaction Management)
- **무방비한 트랜잭션 금지:** `@Transactional`은 클래스 레벨에 습관적으로 걸지 않고, 데이터 변경이 일어나는 Service 클래스의 특정 메서드에만 최소한의 범위로 적용한다.
- **읽기 전용 트랜잭션:** 데이터 변경 없이 순수하게 조회만 하는 메서드에는 반드시 `@Transactional(readOnly = true)`를 적용하여 성능을 최적화하고 의도치 않은 더티 체킹을 방지한다.

## 6. 테스트 코드 (Testing)
- **BDD 스타일 적용:** 모든 단위 테스트 및 통합 테스트는 구조의 가독성을 높이기 위해 `// given`, `// when`, `// then` 주석을 달아 명확히 3단계로 분리하여 작성한다.