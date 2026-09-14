package com.gonggomoa.collector;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 배치가 실패로 저장된 직후 {@link RecruitmentCollectorService}가 호출한다. 매 실패마다
 * 알리면 일시적 오류에도 스팸이 되므로, {@code COLLECTION_BATCH_LOG}를 조회해 연속
 * 실패 횟수가 임계값에 "막 도달한" 시점에만 1회 발송한다 (ADR-0005).
 *
 * <p>알림은 부가 기능이다. 이 서비스가 던지는 예외는 배치 자체를 실패시키면 안 되므로
 * {@link #alertIfConsecutiveFailureThresholdReached}는 어떤 예외도 밖으로 전파하지 않는다.
 * 또한 웹훅 URL 자체가 시크릿이므로 어떤 경로로도 로그에 남기지 않는다.
 */
@Service
public class BatchFailureAlertService {

	private static final Logger log = LoggerFactory.getLogger(BatchFailureAlertService.class);

	private final RestClient discordAlertRestClient;
	private final DiscordAlertProperties properties;
	private final CollectionBatchLogRepository batchLogRepository;

	public BatchFailureAlertService(RestClient discordAlertRestClient, DiscordAlertProperties properties,
			CollectionBatchLogRepository batchLogRepository) {
		this.discordAlertRestClient = discordAlertRestClient;
		this.properties = properties;
		this.batchLogRepository = batchLogRepository;
	}

	public void alertIfConsecutiveFailureThresholdReached(CollectionBatchLog failedLog) {
		try {
			doAlertIfNeeded(failedLog);
		} catch (Exception e) {
			log.warn("배치 실패 알림 처리 중 오류 발생 ({}). 배치 결과에는 영향 없음.", e.getClass().getSimpleName());
		}
	}

	private void doAlertIfNeeded(CollectionBatchLog failedLog) {
		if (properties.webhookUrl() == null || properties.webhookUrl().isBlank()) {
			log.debug("DISCORD_WEBHOOK_URL 미설정 — 배치 실패 알림 비활성");
			return;
		}
		int threshold = properties.failureThreshold();
		List<CollectionBatchLog> recentDesc = batchLogRepository.findAllByOrderByIdDesc(PageRequest.of(0, threshold + 1));
		int consecutiveFailures = countLeadingFailures(recentDesc);
		if (consecutiveFailures != threshold) {
			// threshold 미만이면 아직 알릴 시점이 아니고, threshold를 넘어서면 이미 지난 실패에서 알렸다.
			return;
		}
		send(buildMessage(failedLog, consecutiveFailures));
	}

	private int countLeadingFailures(List<CollectionBatchLog> logsDesc) {
		int count = 0;
		for (CollectionBatchLog batchLog : logsDesc) {
			if (batchLog.isSuccess()) {
				break;
			}
			count++;
		}
		return count;
	}

	private String buildMessage(CollectionBatchLog failedLog, int consecutiveFailures) {
		String error = failedLog.getErrorMessage();
		String displayError = (error == null || error.isBlank()) ? "(오류 메시지 없음)"
				: error.length() > 500 ? error.substring(0, 500) + "..." : error;
		return """
				🔴 **공고모아 수집 배치 연속 %d회 실패**
				마지막 실행: %s
				오류: `%s`""".formatted(consecutiveFailures, failedLog.getStartedAt(), escapeForInlineCode(displayError));
	}

	/**
	 * "/list 호출 - 재시도 소진" 같은 오류 메시지를 인라인 코드블록 없이 그대로 보내면
	 * 디스코드가 "/list"를 서버에 등록된 슬래시 커맨드로 잘못 렌더링한다. 백틱 안에서는
	 * 슬래시 커맨드로 해석하지 않으므로 인라인 코드블록으로 감싼다. 메시지 안에 백틱이
	 * 섞여 있으면 코드블록이 중간에 끊기므로 먼저 치환해둔다.
	 */
	private String escapeForInlineCode(String text) {
		return text.replace('`', '\'');
	}

	private void send(String content) {
		RuntimeException lastError = null;
		for (int attempt = 1; attempt <= properties.maxRetries(); attempt++) {
			try {
				discordAlertRestClient.post()
						.uri(properties.webhookUrl())
						.contentType(MediaType.APPLICATION_JSON)
						.body(Map.of("content", content))
						.retrieve()
						.toBodilessEntity();
				return;
			} catch (RestClientException e) {
				lastError = e;
				if (attempt < properties.maxRetries()) {
					sleep(properties.retryBackoffMs() * attempt);
				}
			}
		}
		throw new DiscordAlertException("디스코드 웹훅 호출 재시도 소진", lastError);
	}

	private void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
