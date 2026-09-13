package com.gonggomoa.common.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.gonggomoa.admin.UnsupportedCollectionFilterException;
import com.gonggomoa.collector.CollectionInProgressException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(CollectionInProgressException.class)
	public ResponseEntity<ErrorResponse> handleCollectionInProgress(CollectionInProgressException e) {
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(ErrorResponse.of("COLLECTION_IN_PROGRESS", e.getMessage()));
	}

	@ExceptionHandler(UnsupportedCollectionFilterException.class)
	public ResponseEntity<ErrorResponse> handleUnsupportedCollectionFilter(UnsupportedCollectionFilterException e) {
		return ResponseEntity.badRequest()
				.body(ErrorResponse.of("UNSUPPORTED_COLLECTION_FILTER", e.getMessage()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
		log.error("처리되지 않은 예외", e);
		return ResponseEntity.internalServerError()
				.body(ErrorResponse.of("INTERNAL_ERROR", "서버 오류가 발생했습니다."));
	}
}
