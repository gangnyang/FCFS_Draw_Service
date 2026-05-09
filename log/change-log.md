# 공통 변경 로그

## 2026-05-10 01:07:15 +09:00 - CI 1차 파이프라인 추가

### 1. 발생 상황
- 로컬에서는 변경 때마다 `.\gradlew test`를 수동으로 실행하고 있었지만, 원격 저장소 기준으로 자동 검증하는 CI 설정은 없었다.
- `.github/workflows`가 비어 있어 push 또는 pull request 시 빌드/테스트가 자동으로 수행되지 않았다.

### 2. 원인
- 현재 프로젝트는 Gradle 멀티 모듈과 서버별 Dockerfile은 갖추고 있지만, GitHub Actions 워크플로우가 아직 정의되지 않았다.
- 따라서 로컬 검증 결과가 GitHub에 기록되지 않고, main 브랜치로 깨진 코드가 들어가는 것을 자동으로 막을 장치가 없었다.

### 3. 조치 내용
- `.github/workflows/ci.yml`을 추가해 `main` 브랜치 대상 push 및 pull request에서 CI가 실행되도록 했다.
- GitHub Actions에서 Java 21을 설정하고 Gradle 캐시를 사용하도록 구성했다.
- `./gradlew clean build`를 실행해 `payment-service`, `draw-service`의 테스트와 빌드를 함께 검증하도록 했다.
