package com.gonggomoa.collector.source;

/**
 * {@code /detail}의 {@code steps[]}를 {@code sortNo} 기준으로 그룹핑해 중복을 제거한
 * 모집단위 하나. {@code rawPositionName}은 직렬명+전형구분이 함께 들어있는 원문이다.
 */
public record SourcePosition(
		int sortNo,
		String rawPositionName,
		Integer headcount,
		String competitionRate) {
}
