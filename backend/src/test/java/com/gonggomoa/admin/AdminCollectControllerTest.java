package com.gonggomoa.admin;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.gonggomoa.collector.CollectionInProgressException;
import com.gonggomoa.collector.CollectionResult;
import com.gonggomoa.collector.RecruitmentCollectorService;

@WebMvcTest(AdminCollectController.class)
class AdminCollectControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private RecruitmentCollectorService collectorService;

	@Test
	void collect_withNoBody_returnsCollectionResult() throws Exception {
		when(collectorService.collect()).thenReturn(new CollectionResult(30, 2, 1, true, null));

		mockMvc.perform(post("/api/admin/collect"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fetchedCount").value(30))
				.andExpect(jsonPath("$.newCount").value(2))
				.andExpect(jsonPath("$.updatedCount").value(1))
				.andExpect(jsonPath("$.success").value(true));
	}

	@Test
	void collect_withNullInstitutionId_returnsCollectionResult() throws Exception {
		when(collectorService.collect()).thenReturn(new CollectionResult(0, 0, 0, true, null));

		mockMvc.perform(post("/api/admin/collect")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"institutionId\": null}"))
				.andExpect(status().isOk());
	}

	@Test
	void collect_withInstitutionIdSpecified_returns400() throws Exception {
		mockMvc.perform(post("/api/admin/collect")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"institutionId\": 5}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("UNSUPPORTED_COLLECTION_FILTER"));
	}

	@Test
	void collect_whenAlreadyRunning_returns503() throws Exception {
		when(collectorService.collect()).thenThrow(new CollectionInProgressException());

		mockMvc.perform(post("/api/admin/collect"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.code").value("COLLECTION_IN_PROGRESS"));
	}
}
