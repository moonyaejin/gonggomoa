package com.gonggomoa.recruitment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ExamPlanTest {

	@Test
	void confirmNoWrittenExam_marksVerifiedInsteadOfLeavingAutoForReview() {
		Position position = Position.builder()
				.jobCategory(JobCategory.ADMIN)
				.rawPositionName("행정(공개경쟁채용)")
				.build();
		ExamPlan examPlan = position.getExamPlan();
		assertThat(examPlan.getVerifyStatus()).isEqualTo(VerifyStatus.AUTO);

		examPlan.confirmNoWrittenExam();

		assertThat(examPlan.getHasWrittenExam()).isFalse();
		assertThat(examPlan.isNcsAreasConfirmed()).isFalse();
		assertThat(examPlan.getVerifyStatus()).isEqualTo(VerifyStatus.VERIFIED);
		assertThat(examPlan.getVerifiedAt()).isNotNull();
	}
}
