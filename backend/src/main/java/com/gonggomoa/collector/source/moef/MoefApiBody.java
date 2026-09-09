package com.gonggomoa.collector.source.moef;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MoefApiBody(List<MoefItem> items, Integer numOfRows, Integer pageNo, Integer totalCount) {
}
