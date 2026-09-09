# 외부 API — 재정경제부_공공기관 채용정보 (알리오)

`spike/api-explore.http`로 실호출해 확인한 실제 응답 구조. `docs/05-roadmap.md`의
필드 의미(어떤 도메인 필드에 대응하는지)는 그대로 유효하고, 이 문서는 **응답의 실제
JSON 형태**만 다룬다. `MoefRecruitmentSource`(`backend/.../collector/source/moef/`)의
DTO는 이 구조를 그대로 반영한다.

## 0. 공공데이터포털 표준 봉투가 아니다

이 API는 공공데이터포털의 통상적인 `response.header` / `response.body.items` 봉투를
쓰지 않는다. **평평한 구조**이고, `resultCode`가 문자열 `"00"`이 아니라 **최상위의
숫자 `200`**이다. 다른 data.go.kr API와 헷갈리지 않도록 주의한다.

## 1. `GET /list`

```json
{
  "resultCode": 200,
  "resultMsg": "성공했습니다.",
  "totalCount": 30,
  "result": [
    {
      "recrutPblntSn": "303642",
      "pblntInstCd": "C0079",
      "instNm": "한국철도공사",
      "recrutPbancTtl": "2026년 신입사원(일반공채) 채용공고",
      "pbancBgngYmd": "20260807",
      "pbancEndYmd": "20260821",
      "srcUrl": "https://info.korail.com/...",
      "ncsCdLst": "R600020",
      "ncsCdNmLst": "정보통신",
      "hireTypeLst": "R1010",
      "hireTypeNmLst": "정규직",
      "workRgnNmLst": "전국",
      "recrutNope": 600,
      "scrnprcdrMthdExpln": "○ 2차(필기시험) : 2배수 선발, 직업기초능력평가(30문항), ...",
      "ongoingYn": "Y",
      "files": [],
      "steps": []
    }
  ]
}
```

- `result`는 **항상 배열**이다.
- `files`, `steps`는 **항상 빈 배열**이다 — `/detail`을 호출해야 채워진다.
- `ongoingYn`은 `/list`에서만 값이 채워진다(`Y`/`N`).

## 2. `GET /detail?sn={recrutPblntSn}`

```json
{
  "resultCode": 200,
  "resultMsg": "성공했습니다.",
  "result": {
    "recrutPblntSn": "303642",
    "pblntInstCd": "C0079",
    "instNm": "한국철도공사",
    "recrutPbancTtl": "2026년 신입사원(일반공채) 채용공고",
    "pbancBgngYmd": "20260807",
    "pbancEndYmd": "20260821",
    "srcUrl": "https://info.korail.com/...",
    "ncsCdLst": "R600020",
    "hireTypeLst": "R1010",
    "workRgnNmLst": "전국",
    "recrutNope": 600,
    "scrnprcdrMthdExpln": "○ 2차(필기시험) : ...",
    "ongoingYn": null,
    "files": [
      { "atchFileNm": "채용공고문.pdf", "url": "https://opendata.alio.go.kr/recruit/downloadAtchFile?recrutAtchFileNo=3066893", "atchFileType": "A" },
      { "atchFileNm": "채용 공고 참고자료.pdf", "url": "https://opendata.alio.go.kr/recruit/downloadAtchFile?recrutAtchFileNo=3066896", "atchFileType": "Z" }
    ],
    "steps": [
      { "sortNo": 0, "recrutPbancTtl": "토목(공개경쟁채용)", "recrutNope": null, "cmpttRt": null },
      { "sortNo": 0, "recrutPbancTtl": "토목(공개경쟁채용)", "recrutNope": null, "cmpttRt": null },
      { "sortNo": 1, "recrutPbancTtl": "전기통신(공개경쟁채용)", "recrutNope": null, "cmpttRt": null }
    ]
  }
}
```

- **`result`는 배열이 아니라 단일 객체다.** `/list`와의 핵심 차이. 여기서 실수하면
  Jackson이 `List<MoefItem>`으로 역직렬화를 시도하다 예외를 던진다.
- `ongoingYn`은 `/detail`에서 항상 `null`이다. 접수 상태 판단은 `/list` 값이나
  `pbancEndYmd` 기준으로 한다.
- `files`, `steps`가 여기서는 실제로 채워진다.
- `steps[]`는 **모집단위 × 전형단계**로 펼쳐져 있어 같은 모집단위가 여러 행 반복된다.
  `sortNo`로 그룹핑해 중복을 제거해야 한다 (`docs/02-domain-model.md` "steps[] 처리
  규칙" 참고). `MoefRecruitmentSource.groupSteps()`가 이 처리를 담당한다.

## 3. 날짜 형식 — 요청과 응답이 다르다

| 구분 | 형식 | 예 |
|---|---|---|
| 요청 파라미터 (`pbancBgngYmd`, `pbancEndYmd` 쿼리) | `yyyy-MM-dd` | `2026-08-01` |
| 응답 필드 (`pbancBgngYmd`, `pbancEndYmd` 값) | `yyyyMMdd` | `20260807` |

같은 필드명인데 요청 때 넣는 형식과 응답으로 받는 형식이 다르다. `MoefRecruitmentSource`는
`REQUEST_DATE_FORMAT`(`yyyy-MM-dd`)과 `RESPONSE_DATE_FORMAT`(`yyyyMMdd`)을 분리해서
쓴다 — 하나로 통일하면 안 된다.

## 4. `resultCode`

숫자 `200`이 성공이다. 그 외 값은 실패로 간주하고 재시도 대상으로 처리한다
(`MoefResponseEnvelope.isSuccess()`).

## 5. 필드 → 도메인 매핑

상세한 필드 매핑표는 `docs/02-domain-model.md`의 "필드 매핑" 절과 "steps[] 처리 규칙"을
참고한다. 이 문서는 JSON 형태 자체만 다룬다.

## 6. 검증 상태

이 문서의 구조는 `spike/api-explore.http`로 실호출해 확인한 실제 응답을 기준으로 한다.
다만 **모든 필드 조합을 다 실호출로 검증한 것은 아니다** — 특히 에러 응답(`resultCode`가
200이 아닌 경우)의 실제 형태와 `steps[]`가 아예 없는(비어 있는) 상세 응답의 형태는
아직 실물로 확인하지 못했다. 이런 케이스를 실제로 마주치면 이 문서와
`MoefListEnvelope`/`MoefDetailEnvelope`/`MoefItem`을 함께 갱신한다.
