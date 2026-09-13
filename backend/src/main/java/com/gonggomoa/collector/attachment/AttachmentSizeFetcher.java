package com.gonggomoa.collector.attachment;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * content_hash 계산에 쓸 첨부파일 크기만 가볍게 조회한다 (HEAD 요청, 본문 다운로드 없음).
 * 실제 첨부파일 다운로드·텍스트 추출은 W3의 몫이다. SSRF 방어(docs/07-security.md §2)를
 * 위해 도메인 화이트리스트 확인, 사설 IP 차단, 리다이렉트 미추적을 모두 적용한다.
 *
 * <p>화이트리스트는 현재 알려진 유일한 첨부파일 호스트(opendata.alio.go.kr)로 좁혀뒀다.
 * 다른 소스가 추가되면 넓혀야 한다.
 */
@Component
public class AttachmentSizeFetcher {

	private static final Logger log = LoggerFactory.getLogger(AttachmentSizeFetcher.class);

	private static final Set<String> ALLOWED_HOSTS = Set.of("opendata.alio.go.kr");
	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
	private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

	private final HttpClient httpClient = HttpClient.newBuilder()
			.connectTimeout(CONNECT_TIMEOUT)
			.followRedirects(HttpClient.Redirect.NEVER)
			.build();

	public long fetchSize(String fileUrl) {
		URI uri;
		try {
			uri = URI.create(fileUrl);
		} catch (IllegalArgumentException e) {
			log.warn("첨부파일 URL 형식 오류: {}", fileUrl);
			return 0L;
		}
		if (!ExternalUrlValidator.isSafe(uri, ALLOWED_HOSTS)) {
			log.warn("첨부파일 URL이 허용 목록을 통과하지 못함: {}", fileUrl);
			return 0L;
		}
		try {
			HttpRequest request = HttpRequest.newBuilder(uri)
					.method("HEAD", HttpRequest.BodyPublishers.noBody())
					.timeout(READ_TIMEOUT)
					.build();
			HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
			if (response.statusCode() / 100 != 2) {
				log.warn("첨부파일 크기 확인 실패 (status={}): {}", response.statusCode(), fileUrl);
				return 0L;
			}
			return response.headers().firstValueAsLong("Content-Length").orElse(0L);
		} catch (IOException e) {
			log.warn("첨부파일 크기 확인 실패: {} ({})", fileUrl, e.getMessage());
			return 0L;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("첨부파일 크기 확인 중 인터럽트 발생", e);
		}
	}
}
