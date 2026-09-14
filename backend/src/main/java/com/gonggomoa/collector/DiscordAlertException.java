package com.gonggomoa.collector;

/**
 * 디스코드 웹훅 호출이 재시도를 소진하고도 실패했을 때 던진다.
 * {@link BatchFailureAlertService#alertIfConsecutiveFailureThresholdReached}가
 * 항상 잡아서 삼키므로 배치 실행에는 영향을 주지 않는다.
 */
public class DiscordAlertException extends RuntimeException {

	public DiscordAlertException(String message, Throwable cause) {
		super(message, cause);
	}
}
