# CLAUDE.md

이 파일은 Claude Code가 이 저장소에서 작업할 때 따라야 할 지침이다.

## 프로젝트 개요

**공고모아 (gonggomoa)** — 공기업 채용공고를 한곳에 모아, **직렬별 필기시험 정보(필기 유무 / NCS만 보는지 전공까지 보는지 / 시험일 / NCS 세부 영역)** 를 한눈에 비교할 수 있게 하는 웹서비스.

- 저장소: `gonggomoa`
- 패키지: `com.gonggomoa`

기존 서비스(잡알리오, 채용 카페)는 공고의 일정·자격요건까지만 보여준다. 이 서비스의 존재 이유는 **공고 첨부파일 안에 묻혀 있는 시험 구성 정보를 구조화해서 필터링 가능하게 만드는 것** 하나다. 이 기능을 희생하는 방향의 제안은 하지 않는다.

- **1차 대상 직렬**: 전산직, 행정직 (2개만. 확장은 v2 이후)
- **사용자**: 공기업 필기시험 준비생. 로그인 없이 익명 열람.
- **운영자**: 1인 (개발자 본인). 검수도 본인이 한다.

## 기술 스택

고정. 임의로 바꾸지 말 것. 변경이 필요하면 먼저 제안하고 승인을 받는다.

**Backend**

- Java 21, Spring Boot 3.x
- Spring Data JPA, MySQL 8
- Redis (조회 캐시)
- Flyway (스키마 마이그레이션 — DDL 자동 생성 금지, `ddl-auto: validate`)
- 스케줄링은 `@Scheduled` + `ShedLock`. Spring Batch는 현 규모에 과하므로 쓰지 않는다.

**Frontend**

- React 18 + TypeScript + Vite
- TanStack Query (서버 상태), Tailwind CSS
- FullCalendar (캘린더 뷰)

**문서 추출 / LLM**

- PDF: Apache PDFBox
- HWP: `hwplib` (구형 .hwp), `hwpxlib` (.hwpx)
- LLM: `ExamPlanExtractor` 인터페이스 뒤에 두고 구현체를 교체 가능하게 한다. 기본 구현은 Anthropic Messages API. 응답은 반드시 JSON 스키마 강제.

**인프라**

- Docker Compose, Nginx + Let's Encrypt
- GitHub Actions (CI: build + test)
- 모니터링: Spring Boot Actuator + 텔레그램 봇 알림
- 대시보드: Grafana (MySQL 데이터소스 직접 연결). 지표 집계용 별도 배치·API를 만들지 않는다. **대시보드 정의는 JSON으로 `infra/grafana/`에 커밋한다**

## 아키텍처 원칙

1. **패키지 구조는 도메인 기준.** `com.gonggomoa.recruitment`, `...institution`, `...extraction`, `...admin`. 레이어 기준(`controller`, `service`, `repository`를 최상위로)으로 나누지 않는다.
2. **시험 정보는 공고가 아니라 직렬(Position)에 붙는다.** 같은 공고 안에서 전산직은 전공+NCS, 행정직은 NCS만 보는 경우가 흔하다. 이 구조를 절대 단순화하지 말 것.
3. **외부 데이터 수집(collector)과 추출(extractor)은 분리된 모듈.** 수집이 실패해도 추출은 이전 데이터로 동작해야 하고, 반대도 마찬가지.
4. **수집 소스는 `RecruitmentSource` 인터페이스로 추상화한다.** 공공데이터포털 API가 여러 개이고, 운영 중 주 소스를 교체하거나 보조 소스를 추가할 가능성이 높다. 특정 API의 응답 구조가 도메인 엔티티에 직접 새어 들어오지 않게 어댑터를 둔다.
5. **`RECRUITMENT.source_type`으로 어느 소스에서 온 데이터인지 항상 기록한다.** 소스별 커버리지·정확도 비교에 필요하다.
6. **LLM 호출도 `ExamPlanExtractor` 인터페이스로 추상화한다.** 모델별 추출 정확도를 비교하고 운영 중 교체할 수 있어야 한다. 특정 벤더의 요청·응답 형식이 도메인 로직에 새어 들어오지 않게 한다. `EXTRACTION_LOG`에 사용한 모델 식별자를 함께 기록한다.
7. **모든 외부 API 호출은 재시도 + 타임아웃 필수.** 공공데이터포털 API는 응답이 느리거나 간헐적으로 실패한다.
8. **DB 스키마 변경은 반드시 Flyway 마이그레이션 파일로.** 기존 마이그레이션 파일은 절대 수정하지 않는다.

## 절대 규칙

- **미검수(`AUTO`) 추출 결과의 시험 정보는 사용자 화면에 노출하지 않는다.** 목록에는 공고가 뜨되, 시험 정보 영역은 "원문 확인 필요" 링크만 표시한다. 틀린 정보를 보여주는 것보다 정보가 없는 게 낫다. 이 규칙을 우회하는 코드는 작성하지 않는다.
- **모든 공고 카드/상세에는 원문 링크와 `verify_status` 배지를 반드시 함께 렌더링한다.**
- **개인정보를 수집하지 않는다.** 이메일, 이름, IP 원본 저장 금지. 클릭 집계는 익명 카운터만.
- **LLM 호출 전 `content_hash`로 중복을 차단한다.** 같은 내용을 두 번 추출하지 않는다.
- 푸터에 출처 표기(잡알리오 / 공공데이터포털)와 면책 문구를 항상 유지한다.
- `/list` 호출 시 `instType`은 A2001~A2004를 각각 호출해 병합한다. 콤마 다중값은 동작하지 않는다.

### 보안 (상세는 `docs/07-security.md`)

- **시크릿을 코드나 설정 파일에 쓰지 않는다.** 전부 환경변수. `application.yml`에 키 값을 넣는 코드는 작성하지 않는다.
- **외부 URL 다운로드 시 도메인 화이트리스트와 사설 IP 차단을 반드시 거친다.** SSRF 방어를 생략한 다운로드 코드를 작성하지 않는다.
- **LLM 출력은 신뢰하지 않는다.** Enum·숫자 범위·문자열 길이를 화이트리스트로 검증한 뒤에만 DB에 저장한다.
- **동적 쿼리는 QueryDSL 또는 Specification으로 작성한다.** 문자열 연결로 SQL이나 JPQL을 만들지 않는다.
- **`dangerouslySetInnerHTML`을 사용하지 않는다.** 검수 화면 하이라이트도 텍스트 노드 분할로 구현한다.
- **`docker-compose.yml`에 MySQL/Redis의 `ports` 매핑을 넣지 않는다.**
- **`docker-compose.prod.yml`에 MySQL/Redis의 `ports` 매핑을 넣지 않는다.**
  개발용 `docker-compose.yml`은 `127.0.0.1` 바인딩으로 포트를 열어도 된다.
- 요청 URL을 로깅할 때 `serviceKey` 등 인증 파라미터를 마스킹한다.

## 코딩 컨벤션

- 테스트: 도메인 로직과 추출 파서는 단위 테스트 필수. 컨트롤러는 `@WebMvcTest`, 통합은 Testcontainers.
- 예외: 도메인별 커스텀 예외 + `@RestControllerAdvice`로 일괄 처리. `RuntimeException` 직접 던지지 말 것.
- API 응답: 성공/실패 공통 래퍼 없이 순수 DTO + HTTP 상태코드로 표현.
- 엔티티에 `@Setter` 금지. 상태 변경은 의도가 드러나는 메서드로(`markVerified()`, `close()`).
- Lombok은 `@Getter`, `@Builder`, `@NoArgsConstructor(access = PROTECTED)` 까지만.
- 커밋 메시지: `feat:`, `fix:`, `refactor:`, `docs:`, `test:`, `chore:` 접두어.

## 작업 방식

- **작업 전 `docs/05-roadmap.md`에서 현재 주차와 범위를 확인한다.** 해당 주차 범위를 벗어나는 기능은 구현하지 말고 먼저 알린다.
- 구조에 영향을 주는 결정(라이브러리 선택, 스키마 변경, 외부 의존성 추가)을 내렸다면 `docs/adr/` 에 ADR을 추가한다.
- 한 번에 한 기능. 여러 기능을 묶어서 커밋하지 않는다.
- **커밋은 항상 제안만 한다.** 한 기능이 완성되면 커밋 메시지 한 줄과 `git push -u origin main` 명령을 함께 제안하고, 실제 `git commit`·`git push` 실행은 사용자가 직접 한다. Claude가 먼저 커밋·푸시를 실행하지 않는다. 하나의 전체적인 기능이 완성되면 pull request 내용을 작성하여 제안한다.
- 요구사항이 모호하면 추측해서 구현하지 말고 질문한다.

## 참고 문서

| 파일                      | 내용                                       |
| ------------------------- | ------------------------------------------ |
| `docs/01-requirements.md` | 요구사항 명세                              |
| `docs/02-domain-model.md` | 도메인 모델 · ERD · 상태 전이              |
| `docs/03-api-spec.md`     | API 명세                                   |
| `docs/04-architecture.md` | 시스템 구성도 · 시퀀스 다이어그램          |
| `docs/05-roadmap.md`      | 주차별 개발 계획 · 마일스톤                |
| `docs/06-records.md`      | 개발일지 · ADR · 지표 기록 체계            |
| `docs/07-security.md`     | 위협 모델 · 보안 대응 · 배포 전 체크리스트 |
