package com.gonggomoa.collector;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
class BatchFailureAlertServiceTest {

	private static final String WEBHOOK_URL = "https://discord.com/api/webhooks/1/test-token";

	@Mock
	private CollectionBatchLogRepository batchLogRepository;

	private DiscordAlertProperties properties(String webhookUrl, int threshold) {
		return new DiscordAlertProperties(webhookUrl, threshold, 1000, 1000, 1, 1);
	}

	private record Fixture(MockRestServiceServer server, BatchFailureAlertService service) {
	}

	private Fixture fixture(DiscordAlertProperties properties) {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		BatchFailureAlertService service = new BatchFailureAlertService(builder.build(), properties,
				batchLogRepository);
		return new Fixture(server, service);
	}

	private CollectionBatchLog failedLog() {
		return failedLog("MOEF 호출 실패");
	}

	private CollectionBatchLog failedLog(String errorMessage) {
		CollectionBatchLog batchLog = CollectionBatchLog.start();
		batchLog.fail(0, 0, 0, errorMessage);
		return batchLog;
	}

	@Test
	void whenWebhookUrlBlank_doesNotQueryOrCallServer() {
		Fixture fixture = fixture(properties("", 3));

		fixture.service().alertIfConsecutiveFailureThresholdReached(failedLog());

		verify(batchLogRepository, never()).findAllByOrderByIdDesc(any());
		fixture.server().verify();
	}

	@Test
	void whenConsecutiveFailuresBelowThreshold_doesNotCallServer() {
		Fixture fixture = fixture(properties(WEBHOOK_URL, 3));
		when(batchLogRepository.findAllByOrderByIdDesc(PageRequest.of(0, 4)))
				.thenReturn(List.of(failedLog(), failedLog()));

		fixture.service().alertIfConsecutiveFailureThresholdReached(failedLog());

		fixture.server().verify();
	}

	@Test
	void whenConsecutiveFailuresReachThresholdExactly_sendsDiscordMessage() {
		Fixture fixture = fixture(properties(WEBHOOK_URL, 3));
		when(batchLogRepository.findAllByOrderByIdDesc(PageRequest.of(0, 4)))
				.thenReturn(List.of(failedLog(), failedLog(), failedLog()));
		fixture.server().expect(requestTo(WEBHOOK_URL))
				.andExpect(method(HttpMethod.POST))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("연속 3회 실패")))
				.andRespond(withSuccess());

		fixture.service().alertIfConsecutiveFailureThresholdReached(failedLog());

		fixture.server().verify();
	}

	@Test
	void wrapsErrorMessageInInlineCodeSoDiscordDoesNotRenderSlashCommand() {
		Fixture fixture = fixture(properties(WEBHOOK_URL, 3));
		when(batchLogRepository.findAllByOrderByIdDesc(PageRequest.of(0, 4)))
				.thenReturn(List.of(failedLog(), failedLog(), failedLog()));
		fixture.server().expect(requestTo(WEBHOOK_URL))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("`/list 호출 - 재시도 소진`")))
				.andRespond(withSuccess());

		fixture.service().alertIfConsecutiveFailureThresholdReached(failedLog("/list 호출 - 재시도 소진"));

		fixture.server().verify();
	}

	@Test
	void whenConsecutiveFailuresAlreadyPastThreshold_doesNotResend() {
		Fixture fixture = fixture(properties(WEBHOOK_URL, 3));
		when(batchLogRepository.findAllByOrderByIdDesc(PageRequest.of(0, 4)))
				.thenReturn(List.of(failedLog(), failedLog(), failedLog(), failedLog()));

		fixture.service().alertIfConsecutiveFailureThresholdReached(failedLog());

		fixture.server().verify();
	}

	@Test
	void whenDiscordCallFails_swallowsExceptionWithoutPropagating() {
		Fixture fixture = fixture(properties(WEBHOOK_URL, 3));
		when(batchLogRepository.findAllByOrderByIdDesc(PageRequest.of(0, 4)))
				.thenReturn(List.of(failedLog(), failedLog(), failedLog()));
		fixture.server().expect(requestTo(WEBHOOK_URL)).andRespond(withServerError());

		assertThatCode(() -> fixture.service().alertIfConsecutiveFailureThresholdReached(failedLog()))
				.doesNotThrowAnyException();

		fixture.server().verify();
	}

	@Test
	void whenLatestBatchSucceeded_streakResetsSoOlderFailuresDontCount() {
		Fixture fixture = fixture(properties(WEBHOOK_URL, 3));
		CollectionBatchLog success = CollectionBatchLog.start();
		success.complete(1, 1, 0);
		when(batchLogRepository.findAllByOrderByIdDesc(PageRequest.of(0, 4)))
				.thenReturn(List.of(failedLog(), success, failedLog(), failedLog()));

		fixture.service().alertIfConsecutiveFailureThresholdReached(failedLog());

		fixture.server().verify();
	}
}
