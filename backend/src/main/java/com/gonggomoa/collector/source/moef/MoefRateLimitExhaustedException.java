package com.gonggomoa.collector.source.moef;

/**
 * {@code X-RateLimit-Remaining}이 임계값 아래로 떨어졌을 때 던진다. 상위 Collector는 이
 * 예외를 잡아 남은 {@code /detail} 호출을 다음 배치로 미뤄야 한다.
 */
public class MoefRateLimitExhaustedException extends MoefApiException {

	public MoefRateLimitExhaustedException(int remaining, int threshold) {
		super("MOEF API 잔여 호출량 부족 (remaining=%d, threshold=%d)".formatted(remaining, threshold));
	}
}
