package com.gonggomoa.collector.source;

import java.util.List;

public record SourceRecruitmentDetail(
		String externalId,
		List<SourcePosition> positions) {
}
