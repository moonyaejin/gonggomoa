package com.gonggomoa.collector.attachment;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ExternalUrlValidatorTest {

	// RFC 5737 TEST-NET-3 — 문서용 공인 대역이라 실제 네트워크 조회 없이 안전하게 쓸 수 있다.
	private static final String SAFE_PUBLIC_HOST = "203.0.113.5";

	@Test
	void allowsWhitelistedHostWithPublicAddress() {
		URI uri = URI.create("https://%s/file.pdf".formatted(SAFE_PUBLIC_HOST));

		assertThat(ExternalUrlValidator.isSafe(uri, Set.of(SAFE_PUBLIC_HOST))).isTrue();
	}

	@Test
	void rejectsHostNotInWhitelist() {
		URI uri = URI.create("https://%s/file.pdf".formatted(SAFE_PUBLIC_HOST));

		assertThat(ExternalUrlValidator.isSafe(uri, Set.of("opendata.alio.go.kr"))).isFalse();
	}

	@Test
	void rejectsLoopbackAddress() {
		URI uri = URI.create("https://127.0.0.1/file.pdf");

		assertThat(ExternalUrlValidator.isSafe(uri, Set.of("127.0.0.1"))).isFalse();
	}

	@Test
	void rejectsPrivateNetworkAddress() {
		URI uri = URI.create("https://192.168.1.1/file.pdf");

		assertThat(ExternalUrlValidator.isSafe(uri, Set.of("192.168.1.1"))).isFalse();
	}

	@Test
	void rejectsLinkLocalMetadataAddress() {
		URI uri = URI.create("https://169.254.169.254/latest/meta-data");

		assertThat(ExternalUrlValidator.isSafe(uri, Set.of("169.254.169.254"))).isFalse();
	}

	@Test
	void rejectsDisallowedScheme() {
		URI uri = URI.create("ftp://%s/file.pdf".formatted(SAFE_PUBLIC_HOST));

		assertThat(ExternalUrlValidator.isSafe(uri, Set.of(SAFE_PUBLIC_HOST))).isFalse();
	}
}
