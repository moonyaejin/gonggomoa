package com.gonggomoa.collector.source.moef;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.gonggomoa.collector.source.RecruitmentSource;
import com.gonggomoa.collector.source.SourceAttachment;
import com.gonggomoa.collector.source.SourcePosition;
import com.gonggomoa.collector.source.SourceRecruitment;
import com.gonggomoa.collector.source.SourceRecruitmentDetail;
import com.gonggomoa.institution.InstitutionType;
import com.gonggomoa.recruitment.AttachmentType;
import com.gonggomoa.recruitment.EmploymentType;
import com.gonggomoa.recruitment.JobCategory;
import com.gonggomoa.recruitment.SourceType;

/**
 * 재정경제부_공공기관 채용정보(잡알리오) API 어댑터.
 *
 * <p>{@code instType}은 콤마 다중값을 지원하지 않으므로 MVP 대상인 A2001~A2004를 각각
 * 단일값으로 호출해 병합한다 (CLAUDE.md 절대 규칙). {@code ncsCdLst}·{@code hireTypeLst}·
 * {@code ongoingYn} 필터는 항상 적용한다 — 필터 없이 조회하면 112,655건이 반환되어
 * 무의미하다 (docs/05-roadmap.md "구현 시 주의사항").
 */
public class MoefRecruitmentSource implements RecruitmentSource {

	private static final Logger log = LoggerFactory.getLogger(MoefRecruitmentSource.class);

	private static final List<InstitutionType> MVP_INSTITUTION_TYPES = List.of(
			InstitutionType.A2001, InstitutionType.A2002, InstitutionType.A2003, InstitutionType.A2004);

	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

	private static final Pattern SERVICE_KEY_PATTERN = Pattern.compile("serviceKey=[^&]*");

	private final RestClient restClient;
	private final MoefProperties properties;
	private final AtomicInteger remainingCalls = new AtomicInteger(Integer.MAX_VALUE);

	public MoefRecruitmentSource(RestClient restClient, MoefProperties properties) {
		this.restClient = restClient;
		this.properties = properties;
	}

	@Override
	public List<SourceRecruitment> fetchList(LocalDate from, LocalDate to, int page) {
		List<SourceRecruitment> merged = new ArrayList<>();
		for (InstitutionType instType : MVP_INSTITUTION_TYPES) {
			MoefApiBody body = requestList(from, to, page, instType);
			for (MoefItem item : safeList(body == null ? null : body.items())) {
				merged.add(toSourceRecruitment(item, instType));
			}
		}
		return merged;
	}

	@Override
	public SourceRecruitmentDetail fetchDetail(String externalId) {
		MoefItem item = fetchDetailItem(externalId);
		return new SourceRecruitmentDetail(externalId, groupSteps(item.steps()));
	}

	@Override
	public List<SourceAttachment> fetchAttachments(String externalId) {
		MoefItem item = fetchDetailItem(externalId);
		return safeList(item.files()).stream()
				.map(this::toSourceAttachment)
				.filter(Objects::nonNull)
				.toList();
	}

	@Override
	public SourceType getSourceType() {
		return SourceType.MOEF;
	}

	private MoefApiBody requestList(LocalDate from, LocalDate to, int page, InstitutionType instType) {
		Map<String, String> params = new LinkedHashMap<>();
		params.put("resultType", "json");
		params.put("numOfRows", String.valueOf(properties.numOfRows()));
		params.put("pageNo", String.valueOf(page));
		params.put("ongoingYn", "Y");
		params.put("ncsCdLst", properties.ncsCdLst());
		params.put("hireTypeLst", properties.hireTypeLst());
		params.put("instType", instType.name());
		if (from != null) {
			params.put("pbancBgngYmd", DATE_FORMAT.format(from));
		}
		if (to != null) {
			params.put("pbancEndYmd", DATE_FORMAT.format(to));
		}
		return fetchBody("/list", params);
	}

	private MoefItem fetchDetailItem(String externalId) {
		ensureRateLimitAvailable();
		Map<String, String> params = new LinkedHashMap<>();
		params.put("resultType", "json");
		params.put("sn", externalId);
		MoefApiBody body = fetchBody("/detail", params);
		List<MoefItem> items = safeList(body == null ? null : body.items());
		if (items.isEmpty()) {
			throw new MoefApiException("MOEF 상세 조회 결과 없음 (externalId=%s)".formatted(externalId));
		}
		return items.get(0);
	}

	private void ensureRateLimitAvailable() {
		int remaining = remainingCalls.get();
		if (remaining < properties.rateLimitThreshold()) {
			throw new MoefRateLimitExhaustedException(remaining, properties.rateLimitThreshold());
		}
	}

	private MoefApiBody fetchBody(String path, Map<String, String> params) {
		return executeWithRetry(path + " 호출", () -> {
			URI uri = buildUri(path, params);
			log.debug("MOEF 요청: {}", maskServiceKey(uri.toString()));
			ResponseEntity<MoefApiEnvelope> response;
			try {
				response = restClient.get().uri(uri).retrieve().toEntity(MoefApiEnvelope.class);
			} catch (RestClientException e) {
				throw new MoefApiException("MOEF 호출 실패: %s".formatted(maskServiceKey(uri.toString())), e);
			}
			updateRateLimit(response.getHeaders());
			MoefApiEnvelope envelope = response.getBody();
			if (envelope == null || envelope.response() == null || envelope.response().header() == null) {
				throw new MoefApiException("MOEF 응답 형식이 올바르지 않습니다: %s".formatted(maskServiceKey(uri.toString())));
			}
			MoefApiHeader header = envelope.response().header();
			if (!header.isSuccess()) {
				throw new MoefApiException("MOEF API 오류 응답 (resultCode=%s, resultMsg=%s)"
						.formatted(header.resultCode(), header.resultMsg()));
			}
			return envelope.response().body();
		});
	}

	private <T> T executeWithRetry(String description, Supplier<T> action) {
		RuntimeException lastError = null;
		for (int attempt = 1; attempt <= properties.maxRetries(); attempt++) {
			try {
				return action.get();
			} catch (MoefApiException e) {
				lastError = e;
				log.warn("{} 실패 (시도 {}/{}): {}", description, attempt, properties.maxRetries(), e.getMessage());
				if (attempt < properties.maxRetries()) {
					sleep(properties.retryBackoffMs() * attempt);
				}
			}
		}
		throw new MoefApiException("%s - 재시도 소진".formatted(description), lastError);
	}

	private URI buildUri(String path, Map<String, String> params) {
		StringBuilder sb = new StringBuilder(properties.baseUrl())
				.append(path)
				.append("?serviceKey=")
				.append(properties.serviceKey());
		for (Map.Entry<String, String> entry : params.entrySet()) {
			sb.append('&')
					.append(entry.getKey())
					.append('=')
					.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
		}
		return URI.create(sb.toString());
	}

	private void updateRateLimit(HttpHeaders headers) {
		String remaining = headers.getFirst("X-RateLimit-Remaining");
		if (remaining == null) {
			return;
		}
		try {
			remainingCalls.set(Integer.parseInt(remaining));
		} catch (NumberFormatException e) {
			log.warn("X-RateLimit-Remaining 헤더 파싱 실패: {}", remaining);
		}
	}

	private List<SourcePosition> groupSteps(List<MoefStep> steps) {
		if (steps == null || steps.isEmpty()) {
			return List.of();
		}
		Map<Integer, MoefStep> firstBySortNo = new LinkedHashMap<>();
		for (MoefStep step : steps) {
			firstBySortNo.putIfAbsent(step.sortNo(), step);
		}
		return firstBySortNo.values().stream()
				.map(step -> new SourcePosition(step.sortNo(), step.recrutPbancTtl(), step.recrutNope(),
						step.cmpttRt()))
				.toList();
	}

	private SourceAttachment toSourceAttachment(MoefFile file) {
		AttachmentType type = mapAttachmentType(file.atchFileType());
		if (type != AttachmentType.ANNOUNCEMENT && type != AttachmentType.REFERENCE) {
			return null;
		}
		return new SourceAttachment(file.atchFileNm(), file.url(), type);
	}

	private AttachmentType mapAttachmentType(String code) {
		if (code == null) {
			return null;
		}
		return switch (code) {
			case "A" -> AttachmentType.ANNOUNCEMENT;
			case "B" -> AttachmentType.APPLICATION_FORM;
			case "C" -> AttachmentType.JOB_DESCRIPTION;
			case "Z" -> AttachmentType.REFERENCE;
			default -> {
				log.warn("정의되지 않은 첨부파일 유형 코드: {}", code);
				yield null;
			}
		};
	}

	private SourceRecruitment toSourceRecruitment(MoefItem item, InstitutionType instType) {
		return new SourceRecruitment(
				item.recrutPblntSn(),
				item.pblntInstCd(),
				item.instNm(),
				instType,
				item.recrutPbancTtl(),
				parseDate(item.pbancBgngYmd()),
				parseDate(item.pbancEndYmd()),
				item.srcUrl(),
				parseJobCategories(item.ncsCdLst()),
				mapEmploymentType(item.hireTypeLst()),
				item.workRgnNmLst(),
				item.recrutNope(),
				item.scrnprcdrMthdExpln());
	}

	private LocalDate parseDate(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		try {
			return LocalDate.parse(raw, DATE_FORMAT);
		} catch (DateTimeParseException e) {
			log.warn("날짜 파싱 실패: {}", raw);
			return null;
		}
	}

	private Set<JobCategory> parseJobCategories(String ncsCdLst) {
		if (ncsCdLst == null || ncsCdLst.isBlank()) {
			return Set.of();
		}
		Set<JobCategory> categories = new LinkedHashSet<>();
		for (String code : ncsCdLst.split(",")) {
			categories.add(mapJobCategory(code.trim()));
		}
		return categories;
	}

	private JobCategory mapJobCategory(String code) {
		return switch (code) {
			case "R600020" -> JobCategory.IT;
			case "R600002" -> JobCategory.ADMIN;
			case "R600015" -> JobCategory.MECHANICAL;
			case "R600019" -> JobCategory.ELECTRICAL;
			case "R600014" -> JobCategory.CIVIL;
			case "R600017" -> JobCategory.CHEMICAL;
			case "R600023" -> JobCategory.ENV_ENERGY;
			case "R600025" -> JobCategory.RESEARCH;
			default -> {
				log.warn("정의되지 않은 NCS 코드: {}", code);
				yield JobCategory.UNKNOWN;
			}
		};
	}

	/**
	 * {@code hireTypeLst}는 콤마 다중값이 올 수 있으나 RECRUITMENT.employment_type은 단일
	 * 컬럼이다. 여러 값이 섞인 경우 정규직 &gt; 채용형 인턴 순으로 우선한다 — 흔치 않은
	 * 케이스이며 원문은 {@code screeningProcedureText}로도 확인 가능하다.
	 */
	private EmploymentType mapEmploymentType(String hireTypeLst) {
		if (hireTypeLst == null || hireTypeLst.isBlank()) {
			return EmploymentType.UNKNOWN;
		}
		Set<String> codes = Set.of(hireTypeLst.split(","));
		if (codes.contains("R1010")) {
			return EmploymentType.FULL_TIME;
		}
		if (codes.contains("R1070")) {
			return EmploymentType.INTERN_HIRE;
		}
		if (codes.contains("R1020")) {
			return EmploymentType.CONTRACT;
		}
		if (codes.contains("R1030")) {
			return EmploymentType.PERMANENT_CONTRACT;
		}
		if (codes.contains("R1040")) {
			return EmploymentType.NON_REGULAR;
		}
		if (codes.contains("R1050")) {
			return EmploymentType.INTERN;
		}
		if (codes.contains("R1060")) {
			return EmploymentType.INTERN_EXP;
		}
		log.warn("정의되지 않은 고용유형 코드: {}", hireTypeLst);
		return EmploymentType.UNKNOWN;
	}

	private static <T> List<T> safeList(List<T> list) {
		return list == null ? List.of() : list;
	}

	private static String maskServiceKey(String url) {
		return SERVICE_KEY_PATTERN.matcher(url).replaceAll("serviceKey={SERVICE_KEY}");
	}

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new MoefApiException("재시도 대기 중 인터럽트 발생", e);
		}
	}
}
