package com.gonggomoa.collector;

/**
 * 이미 실행 중인 수집 배치가 있을 때 던진다 (예: 매시 정각 배치와 관리자 수동 실행이
 * 겹치는 경우). docs/03-api-spec.md 공통 규약의 503 응답에 대응한다.
 */
public class CollectionInProgressException extends RuntimeException {

	public CollectionInProgressException() {
		super("이미 수집 배치가 실행 중입니다.");
	}
}
