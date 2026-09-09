package com.gonggomoa.collector.source.moef;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MoefFile(String atchFileNm, String url, String atchFileType) {
}
