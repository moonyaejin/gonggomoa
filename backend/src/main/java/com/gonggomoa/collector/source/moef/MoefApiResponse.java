package com.gonggomoa.collector.source.moef;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MoefApiResponse(MoefApiHeader header, MoefApiBody body) {
}
