package com.gonggomoa.collector.source.moef;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gonggomoa.collector.moef")
public record MoefProperties(
		String baseUrl,
		String serviceKey,
		String ncsCdLst,
		String hireTypeLst,
		int numOfRows,
		int connectTimeoutMs,
		int readTimeoutMs,
		int maxRetries,
		long retryBackoffMs,
		int rateLimitThreshold) {
}
