package com.gonggomoa.collector.source.moef;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * {@code /detail} 응답. {@code result}는 배열이 아니라 단일 객체다 ({@code /list}와의
 * 차이 — docs/08-external-api.md 참고).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MoefDetailEnvelope(int resultCode, String resultMsg, MoefItem result) implements MoefResponseEnvelope {
}
