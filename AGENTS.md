# 🎯 FCFS-Draw Service

본 프로젝트는 대규모 트래픽 환경에서 데이터 정합성을 보장하는 **선착순 드로우(FCFS Draw) 결제 시스템**의 백엔드입니다. (Java 21 / Spring Boot 3.5.14)

## ❕ 지켜야 할 최우선 사항
- **읽기 쓰기:** 읽기 쓰기 작업은 무조건 UTF-8로 이루어져야 하고, 한글이 깨지지 않도록 설정.
- **수정 명시:** 코드 수정 시 어느 부분을 수정했는지 명시해야 하고, 어떤 이유로 적용했는지 설명.
- **로그:** 취햔 전략, 혹은 트러블 발생으로 인한 수정 내용, 어떤 상황에서 발생, 왜 발생, 어떻게 조치했는지 3단계로 구성하여 `/log` 폴더 내에 change-log.md 파일로 타임스탬프를 명시하여 기록

## 🛠 명령어 (Root: /)
- **Build:** `./gradlew build -x test`
- **Run:** `./gradlew bootRun`
- **Test:** `./gradlew test` (특히 동시성 테스트 성공 확인 필수)

## 🚀 흐름: 개발 → 테스트 (부하/동시성) → 커밋
1. **기능 개발:** 비즈니스 요구사항에 맞춰 Entity 및 API 개발.
2. **동시성 & 부하 테스트:** 일반 단위 테스트 외에 멀티스레드 환경의 동시성 테스트 통과 필수.
3. **커밋:** 룰에 맞는 커밋 메시지로 기록.

## 📚 상세 지침 (코드 작성 전 반드시 아래 문서를 확인)

| 문서명 | 경로 | 설명 |
| :--- | :--- | :--- |
| **코드 스타일 & JPA** | `.agents/skills/code-style-guide.md` | DTO 분리, Entity 캡슐화 규칙 |
| **동시성 & 트랜잭션** | `.agents/skills/concurrency-guide.md` | **[가장 중요]** 단계별 락(Lock) 적용 및 롤백 정책 |
| **Git & 커밋 규칙** | `.agents/skills/git-workflow.md` | Commit Convention (feat, fix 등) |
| **아키텍처 & API 설계 정책** | `.agents/skills/architecture-guide.md` | 서버 간 통신, API 스펙, 도메인 분리 |
| **배포 & 모니터링** | `.agents/skills/deployment-loadtest-guide.md` | AWS 배포 및 모니터링

## 🔒 보안 & 환경 설정
- **민감 정보 절대 커밋 금지:** DB 접속 정보, Redis 호스트, 외부 API Key.
- **환경 변수 활용:** `application.yml`에서는 `${DB_PASSWORD}` 형태로 작성하고, 실제 값은 `.env` 또는 `application-local.yml`에서 주입받는다.
- .gitignore 파일도 설정한다.