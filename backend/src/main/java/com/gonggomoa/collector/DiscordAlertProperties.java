package com.gonggomoa.collector;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code webhookUrl}이 비어 있으면 알림을 비활성 상태로 둔다 (로컬 개발 환경에서
 * 디스코드 서버 없이도 배치가 정상 동작해야 하므로).
 */
@ConfigurationProperties(prefix = "gonggomoa.collector.discord-alert")
public record DiscordAlertProperties(
		String webhookUrl,
		int failureThreshold,
		int connectTimeoutMs,
		int readTimeoutMs,
		int maxRetries,
		long retryBackoffMs) {
}
