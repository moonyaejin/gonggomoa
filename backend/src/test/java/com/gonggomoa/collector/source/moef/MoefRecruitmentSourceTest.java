package com.gonggomoa.collector.source.moef;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.gonggomoa.collector.source.SourceAttachment;
import com.gonggomoa.collector.source.SourceRecruitment;
import com.gonggomoa.collector.source.SourceRecruitmentDetail;
import com.gonggomoa.institution.InstitutionType;
import com.gonggomoa.recruitment.AttachmentType;
import com.gonggomoa.recruitment.EmploymentType;
import com.gonggomoa.recruitment.JobCategory;

class MoefRecruitmentSourceTest {

	private static final String BASE_URL = "https://apis.data.go.kr/1051000/recruitment";

	private MoefProperties properties() {
		return new MoefProperties(BASE_URL, "test-key", "R600020,R600002", "R1010,R1070", 100, 1000, 1000, 3, 1, 100);
	}

	private record Fixture(MockRestServiceServer server, MoefRecruitmentSource source) {
	}

	private Fixture fixture() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
		MoefRecruitmentSource source = new MoefRecruitmentSource(builder.build(), properties());
		return new Fixture(server, source);
	}

	private String listJson(String... items) {
		return """
				{"response":{"header":{"resultCode":"00","resultMsg":"OK"},
				"body":{"items":[%s],"numOfRows":100,"pageNo":1,"totalCount":%d}}}
				""".formatted(String.join(",", items), items.length);
	}

	private String item(String sn, String instNm, String title, String ncsCdLst, String hireTypeLst) {
		return """
				{"recrutPblntSn":"%s","pblntInstCd":"COD1","instNm":"%s","recrutPbancTtl":"%s",
				"pbancBgngYmd":"2026-09-01","pbancEndYmd":"2026-09-30","srcUrl":"https://example.org/%s",
				"ncsCdLst":"%s","hireTypeLst":"%s","workRgnNmLst":"서울","recrutNope":5,
				"scrnprcdrMthdExpln":"필기시험 실시","files":[],"steps":[]}
				""".formatted(sn, instNm, title, sn, ncsCdLst, hireTypeLst);
	}

	@Test
	void fetchList_splitsInstTypeIntoFourCallsAndMergesResults() {
		Fixture fixture = fixture();

		fixture.server().expect(requestTo(containsString("instType=A2001")))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(listJson(item("1", "기관A", "전산직 채용", "R600020", "R1010")),
						MediaType.APPLICATION_JSON));
		fixture.server().expect(requestTo(containsString("instType=A2002")))
				.andRespond(withSuccess(listJson(), MediaType.APPLICATION_JSON));
		fixture.server().expect(requestTo(containsString("instType=A2003")))
				.andRespond(withSuccess(listJson(), MediaType.APPLICATION_JSON));
		fixture.server().expect(requestTo(containsString("instType=A2004")))
				.andRespond(withSuccess(listJson(item("2", "기관B", "행정직 채용", "R600002", "R1070")),
						MediaType.APPLICATION_JSON));

		var results = fixture.source().fetchList(null, null, 1);

		assertThat(results).hasSize(2);
		SourceRecruitment first = results.get(0);
		assertThat(first.externalId()).isEqualTo("1");
		assertThat(first.institutionType()).isEqualTo(InstitutionType.A2001);
		assertThat(first.jobCategories()).containsExactly(JobCategory.IT);
		assertThat(first.employmentType()).isEqualTo(EmploymentType.FULL_TIME);

		SourceRecruitment second = results.get(1);
		assertThat(second.institutionType()).isEqualTo(InstitutionType.A2004);
		assertThat(second.jobCategories()).containsExactly(JobCategory.ADMIN);
		assertThat(second.employmentType()).isEqualTo(EmploymentType.INTERN_HIRE);

		fixture.server().verify();
	}

	@Test
	void fetchList_undefinedCodesMapToUnknownInsteadOfThrowing() {
		Fixture fixture = fixture();
		fixture.server().expect(requestTo(containsString("instType=A2001")))
				.andRespond(withSuccess(listJson(item("9", "기관C", "미분류직", "R9999", "R9999")),
						MediaType.APPLICATION_JSON));
		fixture.server().expect(requestTo(containsString("instType=A2002")))
				.andRespond(withSuccess(listJson(), MediaType.APPLICATION_JSON));
		fixture.server().expect(requestTo(containsString("instType=A2003")))
				.andRespond(withSuccess(listJson(), MediaType.APPLICATION_JSON));
		fixture.server().expect(requestTo(containsString("instType=A2004")))
				.andRespond(withSuccess(listJson(), MediaType.APPLICATION_JSON));

		var results = fixture.source().fetchList(null, null, 1);

		assertThat(results).hasSize(1);
		assertThat(results.get(0).jobCategories()).containsExactly(JobCategory.UNKNOWN);
		assertThat(results.get(0).employmentType()).isEqualTo(EmploymentType.UNKNOWN);
	}

	@Test
	void fetchDetail_groupsStepsBySortNoRemovingDuplicateRows() {
		Fixture fixture = fixture();
		String detailJson = """
				{"response":{"header":{"resultCode":"00","resultMsg":"OK"},
				"body":{"items":[{"recrutPblntSn":"1","pblntInstCd":"COD1","instNm":"기관A",
				"recrutPbancTtl":"전산직 채용","pbancBgngYmd":"2026-09-01","pbancEndYmd":"2026-09-30",
				"srcUrl":"https://example.org/1","ncsCdLst":"R600020","hireTypeLst":"R1010",
				"workRgnNmLst":"서울","recrutNope":5,"scrnprcdrMthdExpln":"필기시험 실시","files":[],
				"steps":[
				{"sortNo":0,"recrutPbancTtl":"전산(공개경쟁채용)","recrutNope":null,"cmpttRt":null},
				{"sortNo":0,"recrutPbancTtl":"전산(공개경쟁채용)","recrutNope":null,"cmpttRt":null},
				{"sortNo":1,"recrutPbancTtl":"행정(공개경쟁채용)","recrutNope":null,"cmpttRt":null}
				]}],"numOfRows":1,"pageNo":1,"totalCount":1}}}
				""";
		fixture.server().expect(requestTo(containsString("/detail")))
				.andRespond(withSuccess(detailJson, MediaType.APPLICATION_JSON));

		SourceRecruitmentDetail detail = fixture.source().fetchDetail("1");

		assertThat(detail.positions()).hasSize(2);
		assertThat(detail.positions().get(0).rawPositionName()).isEqualTo("전산(공개경쟁채용)");
		assertThat(detail.positions().get(1).rawPositionName()).isEqualTo("행정(공개경쟁채용)");
	}

	@Test
	void fetchAttachments_keepsOnlyAnnouncementAndReference() {
		Fixture fixture = fixture();
		String detailJson = """
				{"response":{"header":{"resultCode":"00","resultMsg":"OK"},
				"body":{"items":[{"recrutPblntSn":"1","pblntInstCd":"COD1","instNm":"기관A",
				"recrutPbancTtl":"전산직 채용","pbancBgngYmd":"2026-09-01","pbancEndYmd":"2026-09-30",
				"srcUrl":"https://example.org/1","ncsCdLst":"R600020","hireTypeLst":"R1010",
				"workRgnNmLst":"서울","recrutNope":5,"scrnprcdrMthdExpln":"필기시험 실시",
				"files":[
				{"atchFileNm":"공고문.pdf","url":"https://x/1","atchFileType":"A"},
				{"atchFileNm":"입사지원서.hwp","url":"https://x/2","atchFileType":"B"},
				{"atchFileNm":"직무기술서.pdf","url":"https://x/3","atchFileType":"C"},
				{"atchFileNm":"참고자료.pdf","url":"https://x/4","atchFileType":"Z"}
				],"steps":[]}],"numOfRows":1,"pageNo":1,"totalCount":1}}}
				""";
		fixture.server().expect(requestTo(containsString("/detail")))
				.andRespond(withSuccess(detailJson, MediaType.APPLICATION_JSON));

		var attachments = fixture.source().fetchAttachments("1");

		assertThat(attachments).extracting(SourceAttachment::attachmentType)
				.containsExactlyInAnyOrder(AttachmentType.ANNOUNCEMENT, AttachmentType.REFERENCE);
	}

	@Test
	void fetchDetail_whenRateLimitBelowThreshold_throwsWithoutCallingServer() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		RestClient restClient = builder.build();
		MoefRecruitmentSource source = new MoefRecruitmentSource(restClient, properties());

		// remainingCalls 초기값은 Integer.MAX_VALUE이므로, 헤더 응답으로 한 번 낮춰준다.
		server.expect(requestTo(containsString("instType=A2001")))
				.andRespond(withSuccess(listJson(), MediaType.APPLICATION_JSON)
						.headers(headersWithRemaining(5)));
		server.expect(requestTo(containsString("instType=")))
				.andRespond(withSuccess(listJson(), MediaType.APPLICATION_JSON));
		server.expect(requestTo(containsString("instType=")))
				.andRespond(withSuccess(listJson(), MediaType.APPLICATION_JSON));
		server.expect(requestTo(containsString("instType=")))
				.andRespond(withSuccess(listJson(), MediaType.APPLICATION_JSON));
		source.fetchList(null, null, 1);

		assertThatThrownBy(() -> source.fetchDetail("1"))
				.isInstanceOf(MoefRateLimitExhaustedException.class);

		server.verify();
	}

	private org.springframework.http.HttpHeaders headersWithRemaining(int remaining) {
		org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
		headers.add("X-RateLimit-Remaining", String.valueOf(remaining));
		return headers;
	}
}
