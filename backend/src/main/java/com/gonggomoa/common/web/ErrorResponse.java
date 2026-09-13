package com.gonggomoa.common.web;

import java.time.OffsetDateTime;

/**
 * docs/03-api-spec.md "6. 공통 규약"의 에러 응답 형식.
 */
public record ErrorResponse(String code, String message, OffsetDateTime timestamp) {

	public static ErrorResponse of(String code, String message) {
		return new ErrorResponse(code, message, OffsetDateTime.now());
	}
}
