package com.gonggomoa.admin;

/**
 * {@code POST /api/admin/collect}는 현재 전체 수집만 지원한다. 특정 기관으로 범위를
 * 좁히는 기능은 RecruitmentCollectorService에 아직 없어서, institutionId를 특정 값으로
 * 보내면 조용히 무시하는 대신 명확히 거부한다.
 */
public class UnsupportedCollectionFilterException extends RuntimeException {

	public UnsupportedCollectionFilterException() {
		super("institutionId로 범위를 좁힌 수집은 아직 지원하지 않습니다. null로 전체 수집만 가능합니다.");
	}
}
