# API 명세서

Base URL: `/api/v1`
인증: 공개 API는 없음. `/api/admin/**` 만 Basic Auth 또는 단일 API Key.

---

## 1. 공고 조회

### `GET /api/v1/recruitments`

공고 목록. 기본 정렬은 마감 임박순.

**Query Parameters**

| 이름 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `category` | enum[] | 전체 | `IT`, `ADMIN` (다중 가능) |
| `examType` | enum[] | 전체 | `NCS_ONLY`, `NCS_PLUS_MAJOR`, `MAJOR_ONLY` |
| `ncsArea` | enum[] | 전체 | `INFORMATION`, `PROBLEM_SOLVING` 등 |
| `employmentType` | enum[] | 전체 | `FULL_TIME`, `INTERN_HIRE` (다중 가능) |
| `status` | enum | `OPEN` | `OPEN`, `UPCOMING`, `CLOSED`, `ALL` |
| `sort` | string | `deadline` | `deadline`, `latest`, `examDate` |
| `page` | int | 0 | |
| `size` | int | 20 | 최대 50 |

**200 Response**

```json
{
  "content": [
    {
      "id": 1024,
      "institutionName": "한국전력공사",
      "title": "2026년도 하반기 신입사원 채용",
      "employmentTypes": ["FULL_TIME", "INTERN_HIRE"],
      "applyStartAt": "2026-08-10",
      "applyEndAt": "2026-08-21",
      "writtenExamAt": "2026-09-19",
      "dDay": 16,
      "status": "OPEN",
      "sourceUrl": "https://recruit.kepco.co.kr/...",
      "positions": [
        {
          "jobCategory": "IT",
          "headcount": 30,
          "workRegion": "나주",
          "examPlan": {
            "verifyStatus": "VERIFIED",
            "examType": "NCS_PLUS_MAJOR",
            "majorSubjects": "전산학(자료구조, 데이터베이스, 네트워크)",
            "totalQuestions": 55,
            "ncsAreas": [
              { "area": "COMMUNICATION", "questionCount": 10 },
              { "area": "MATH", "questionCount": 10 },
              { "area": "PROBLEM_SOLVING", "questionCount": 10 },
              { "area": "INFORMATION", "questionCount": 10 }
            ]
          }
        },
        {
          "jobCategory": "ADMIN",
          "headcount": 45,
          "workRegion": "나주",
          "examPlan": {
            "verifyStatus": "UNCERTAIN",
            "examType": null,
            "majorSubjects": null,
            "totalQuestions": null,
            "ncsAreas": []
          }
        }
      ]
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 47,
  "totalPages": 3
}
```

**중요 — 두 가지 규칙**

**① 미검수 데이터는 응답에 포함하지 않는다.** `verifyStatus`가 `VERIFIED`가 아니면 시험 정보 필드는 반드시 `null`로 응답한다. 서버에 값이 있더라도 마찬가지다. 이를 검증하는 테스트를 작성한다.

**② 미확인은 필드 단위로 표현한다.** 전체를 숨기지 않고, 확보한 필드는 값을, 못 얻은 필드는 `null`을 준다. 클라이언트는 `null`을 **"공고문 직접 확인 필요"**로 렌더링하고 `noticeFileUrl` 링크를 함께 표시한다.

```json
"examPlan": {
  "verifyStatus": "VERIFIED",
  "hasWrittenExam": true,
  "writtenExamDate": "2026-09-12",
  "examType": "NCS_PLUS_MAJOR",
  "majorSubjects": "직무수행능력평가, 철도법령",
  "totalQuestions": 70,
  "timeLimitMinutes": 70,
  "ncsAreasConfirmed": false,
  "ncsAreas": [],
  "rawPositionName": "사무영업",
  "noticeFileUrl": "https://opendata.alio.go.kr/recruit/downloadAtchFile?recrutAtchFileNo=..."
}
```

위 예시에서 `ncsAreasConfirmed: false`이므로 클라이언트는 NCS 영역 칸에 **"공고문 직접 확인 필요"**와 `noticeFileUrl` 링크를 표시한다. 빈 배열을 "출제 영역 없음"으로 오해하지 않도록 반드시 이 플래그를 함께 본다.

`hasWrittenExam`이 `false`면 "필기전형 없음"을, `null`이면 "공고문 직접 확인 필요"를 표시한다. **이 둘을 같게 취급하지 않는다.**

### `GET /api/v1/recruitments/{id}`

단건 상세. 목록과 동일한 구조에 `attachments` 배열이 추가된다.

---

## 2. 캘린더

### `GET /api/v1/calendar`

**Query Parameters**

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `year` | int | ✓ | |
| `month` | int | ✓ | 1–12 |
| `category` | enum[] | | 직렬 필터 |
| `examType` | enum[] | | 시험 유형 필터 |
| `eventType` | enum[] | | `APPLY_START`, `APPLY_END`, `WRITTEN_EXAM` |

**200 Response**

```json
{
  "year": 2026,
  "month": 8,
  "events": [
    {
      "recruitmentId": 1024,
      "institutionName": "한국전력공사",
      "eventType": "APPLY_END",
      "date": "2026-08-21",
      "categories": ["IT", "ADMIN"],
      "sourceUrl": "https://recruit.kepco.co.kr/..."
    }
  ]
}
```

### `GET /api/v1/calendar.ics`

iCalendar 구독 피드. 사용자가 구글/애플 캘린더에 URL로 구독한다. **계정도 개인정보도 필요 없는 알림 수단.**

**Query Parameters**: `category`, `examType` (조회 API와 동일)

**Response**: `Content-Type: text/calendar; charset=utf-8`

```
BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//gonggomoa//KO
X-WR-CALNAME:공기업 채용 일정 (전산)
BEGIN:VEVENT
UID:1024-apply-end@gonggomoa
DTSTART;VALUE=DATE:20260821
SUMMARY:[마감] 한국전력공사 신입사원 채용
DESCRIPTION:전산직 30명 / NCS+전공\n원문: https://recruit.kepco.co.kr/...
URL:https://recruit.kepco.co.kr/...
BEGIN:VALARM
TRIGGER:-P3D
ACTION:DISPLAY
DESCRIPTION:접수 마감 3일 전
END:VALARM
END:VEVENT
END:VCALENDAR
```

구현 노트
- 향후 6개월 범위의 이벤트만 포함한다. 전체를 넣으면 피드가 비대해진다.
- 마감일에는 3일 전 `VALARM`을 기본으로 넣는다. 이게 사실상의 알림 기능이다.
- 캘린더 클라이언트는 보통 수 시간~하루 간격으로 갱신하므로 실시간성은 기대하지 않는다.

---

### `GET /api/v1/recruitments.csv`

필터 조건이 반영된 CSV 내보내기. **노션·엑셀 임포트용.** 계정도 외부 연동도 없이 사용자가 자기 도구로 데이터를 가져가게 한다.

**Query Parameters**: 목록 조회 API와 동일 (`category`, `examType`, `ncsArea`, `status`)

**Response**
- `Content-Type: text/csv; charset=utf-8`
- `Content-Disposition: attachment; filename="gonggomoa-2026-08-19.csv"`

```csv
기관명,공고제목,고용유형,접수시작,접수마감,필기시험일,D-day,직렬,모집단위,시험유형,전공과목,NCS영역,검수상태,원문링크
한국철도공사,2026년 하반기 신입사원 채용,정규직·채용형인턴,2026-08-18,2026-08-21,2026-09-12,-1,전산,전기통신(공개경쟁채용),NCS+전공,직무수행능력평가·철도법령,공고문 직접 확인 필요,검수완료,https://info.korail.com
```

구현 노트
- **BOM(`\uFEFF`)을 선두에 붙인다.** 없으면 엑셀에서 한글이 깨진다.
- 미확인 필드는 빈 값이 아니라 **"공고문 직접 확인 필요"** 문자열로 채운다. 빈 칸은 "해당 없음"으로 오해된다.
- 미검수 공고의 시험 정보는 JSON 응답과 동일하게 마스킹한다.
- 행 수 상한 500. 초과 시 마감 임박순으로 절단한다.

---

## 3. OG 메타태그

공고 상세 페이지(`/recruitments/{id}`)는 링크 공유 시 카드 프리뷰가 표시되도록 OG 태그를 제공한다. 노션·카카오톡·슬랙·디스코드에 모두 적용된다.

```html
<meta property="og:title" content="한국철도공사 · 2026년 하반기 신입사원 채용">
<meta property="og:description" content="필기 9/12(토) · NCS+전공 · 전산/행정 · 마감 D-14">
<meta property="og:url" content="https://{host}/recruitments/1024">
<meta name="twitter:card" content="summary_large_image">
```

- `og:description`은 **필기시험일 · 시험유형 · D-day** 순으로 구성한다. 공유받은 사람이 클릭 전에 판단할 수 있게 하는 것이 목적이다.
- 미검수 공고는 시험 정보를 넣지 않고 기관명·접수기간만 표기한다. 화면 노출 정책과 동일하다.
- React SPA는 크롤러가 메타태그를 읽지 못하므로, 봇 요청에 한해 서버가 정적 HTML을 반환하거나 프리렌더를 적용한다.

---

## 4. 메타 · 상태

### `GET /api/v1/meta/filters`
필터 UI 구성용. 직렬·시험유형·NCS영역 목록과 각각의 현재 공고 수를 함께 반환한다.

```json
{
  "categories": [
    { "code": "IT", "label": "전산", "openCount": 23 },
    { "code": "ADMIN", "label": "행정", "openCount": 41 }
  ],
  "examTypes": [
    { "code": "NCS_ONLY", "label": "NCS만", "openCount": 18 },
    { "code": "NCS_PLUS_MAJOR", "label": "NCS+전공", "openCount": 29 }
  ],
  "employmentTypes": [
    { "code": "FULL_TIME", "label": "정규직", "openCount": 22 },
    { "code": "INTERN_HIRE", "label": "채용형 인턴", "openCount": 8 }
  ],
  "ncsAreas": [
    { "code": "COMMUNICATION", "label": "의사소통능력", "openCount": 44 },
    { "code": "INFORMATION", "label": "정보능력", "openCount": 21 }
  ]
}
```

### `GET /api/v1/meta/health`

화면 상단에 "최근 업데이트: 2시간 전"으로 노출한다. 신뢰 신호이자 운영자 본인의 감시 장치.

```json
{
  "lastCollectedAt": "2026-08-05T14:00:00+09:00",
  "totalOpenRecruitments": 64,
  "verifiedRatio": 0.78
}
```

### `POST /api/v1/recruitments/{id}/click`

원문 링크 클릭 집계. 익명 카운터만 증가시키고 개인 식별 정보는 저장하지 않는다.

**Request**: `{ "eventType": "SOURCE_LINK" }`
**Response**: `204 No Content`

---

## 5. 관리자 API

전부 인증 필요. `/api/admin/**`

### `GET /api/admin/extractions`
검수 대기 목록.

| 이름 | 타입 | 설명 |
|---|---|---|
| `status` | enum | `AUTO`(대기), `UNCERTAIN`, `VERIFIED` |
| `page`, `size` | int | |

### `GET /api/admin/extractions/{examPlanId}`

검수 화면용. 원문 텍스트와 추출 결과를 함께 반환해 한 화면에서 대조할 수 있게 한다.

```json
{
  "examPlanId": 3312,
  "recruitment": { "id": 1024, "title": "...", "sourceUrl": "..." },
  "position": { "jobCategory": "IT", "headcount": 30 },
  "extracted": {
    "examType": "NCS_PLUS_MAJOR",
    "majorSubjects": "전산학",
    "totalQuestions": 55,
    "ncsAreas": [ { "area": "INFORMATION", "questionCount": 10 } ]
  },
  "confidence": 0.62,
  "sourceText": "…첨부파일에서 추출한 원문 텍스트 전문…",
  "highlightHints": ["필기전형", "직업기초능력", "전공"]
}
```

`highlightHints`는 원문에서 하이라이트할 키워드다. 검수 시간을 줄이는 장치이므로 반드시 구현한다.

### `PUT /api/admin/extractions/{examPlanId}`

수정 후 승인. **어떤 필드가 수정됐는지 서버가 비교해 `EXTRACTION_LOG.corrected_field_count`에 기록한다.** 이 값이 파싱 정확도 지표의 원천이다.

```json
{
  "examType": "NCS_PLUS_MAJOR",
  "majorSubjects": "전산학(자료구조, DB, 네트워크)",
  "totalQuestions": 55,
  "ncsAreas": [
    { "area": "COMMUNICATION", "questionCount": 10 },
    { "area": "INFORMATION", "questionCount": 10 }
  ]
}
```
**Response**: `200 OK`, `verifyStatus: VERIFIED`

### `POST /api/admin/extractions/{examPlanId}/reject`
`UNCERTAIN`으로 전환. `{ "reason": "첨부파일에 시험 정보 없음" }`

### `POST /api/admin/collect`
수집 배치 수동 실행. `{ "institutionId": null }` (null이면 전체)

### `POST /api/admin/extractions/{examPlanId}/retry`
LLM 추출 재시도.

---

## 6. 공통 규약

**에러 응답**
```json
{
  "code": "RECRUITMENT_NOT_FOUND",
  "message": "공고를 찾을 수 없습니다.",
  "timestamp": "2026-08-05T14:22:31+09:00"
}
```

| HTTP | 상황 |
|---|---|
| 400 | 잘못된 파라미터 (필터 값 오류, size 초과) |
| 401 | 관리자 인증 실패 |
| 404 | 리소스 없음 |
| 500 | 서버 오류 |
| 503 | 수집 배치 진행 중 일시 불가 |

**날짜 포맷**: 날짜는 `yyyy-MM-dd`, 일시는 ISO-8601 + `+09:00`.
**페이징**: 0-based. `size` 최대 50.
