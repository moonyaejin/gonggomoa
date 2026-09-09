package com.gonggomoa.collector.source.moef;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * {@code /list} 응답. {@code result}는 항상 배열이다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MoefListEnvelope(int resultCode, String resultMsg, Integer totalCount, List<MoefItem> result)
		implements
			MoefResponseEnvelope {
}
