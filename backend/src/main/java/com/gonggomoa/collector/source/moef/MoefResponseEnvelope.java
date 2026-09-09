package com.gonggomoa.collector.source.moef;

/**
 * 공공데이터포털 표준 봉투가 아니라 이 API 고유의 평평한 응답 구조 — {@code resultCode}가
 * 최상위에 숫자(200)로 온다 (docs/08-external-api.md 참고).
 */
public interface MoefResponseEnvelope {

	int resultCode();

	String resultMsg();

	default boolean isSuccess() {
		return resultCode() == 200;
	}
}
