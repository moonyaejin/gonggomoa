package com.gonggomoa.collector.source;

import java.time.LocalDate;
import java.util.Set;

import com.gonggomoa.institution.InstitutionType;
import com.gonggomoa.recruitment.EmploymentType;
import com.gonggomoa.recruitment.JobCategory;

/**
 * {@code /list} 응답 한 건을 표현하는 소스-중립 DTO. {@code jobCategories}가 복수인 경우
 * 이 공고 안에 서로 다른 NCS 대분류가 섞여 있다는 뜻이며, 어떤 직렬(POSITION)에 어떤 분류를
 * 붙일지는 API가 알려주지 않으므로 이 어댑터가 아니라 상위 Collector가 판단한다.
 */
public record SourceRecruitment(
		String externalId,
		String institutionExternalCode,
		String institutionName,
		InstitutionType institutionType,
		String title,
		LocalDate applyStartAt,
		LocalDate applyEndAt,
		String sourceUrl,
		Set<JobCategory> jobCategories,
		EmploymentType employmentType,
		String workRegion,
		Integer headcount,
		String screeningProcedureText) {
}
