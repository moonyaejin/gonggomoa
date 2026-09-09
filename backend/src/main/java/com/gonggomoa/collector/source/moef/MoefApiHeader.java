package com.gonggomoa.collector.source.moef;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MoefApiHeader(String resultCode, String resultMsg) {

	public boolean isSuccess() {
		return "00".equals(resultCode);
	}
}
