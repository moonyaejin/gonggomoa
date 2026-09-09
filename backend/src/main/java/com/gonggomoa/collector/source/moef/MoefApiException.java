package com.gonggomoa.collector.source.moef;

public class MoefApiException extends RuntimeException {

	public MoefApiException(String message) {
		super(message);
	}

	public MoefApiException(String message, Throwable cause) {
		super(message, cause);
	}
}
