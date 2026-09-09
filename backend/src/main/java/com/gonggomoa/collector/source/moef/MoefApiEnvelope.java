package com.gonggomoa.collector.source.moef;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 공공데이터포털 표준 응답 봉투. {@code response.header.resultCode}가 "00"이면 정상이다.
 * 실제 필드명은 W0에서 유효한 서비스키로 실호출해 확정한 것이 아니라, 공공데이터포털의
 * 통상적인 규격을 따른 것이므로 실제 응답과 다르면 이 파일만 고치면 된다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MoefApiEnvelope(MoefApiResponse response) {
}
