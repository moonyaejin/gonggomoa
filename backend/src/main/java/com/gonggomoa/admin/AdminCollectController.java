package com.gonggomoa.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gonggomoa.collector.CollectionResult;
import com.gonggomoa.collector.RecruitmentCollectorService;

/**
 * 수집 배치 수동 실행 (docs/03-api-spec.md). 로컬 테스트용이라 인증이 없다 — W4에서
 * 관리자 인증(Basic Auth)을 붙일 때 이 컨트롤러도 그 아래로 들어가야 한다.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminCollectController {

	private final RecruitmentCollectorService collectorService;

	public AdminCollectController(RecruitmentCollectorService collectorService) {
		this.collectorService = collectorService;
	}

	@PostMapping("/collect")
	public ResponseEntity<CollectionResult> collect(@RequestBody(required = false) CollectRequest request) {
		if (request != null && request.institutionId() != null) {
			throw new UnsupportedCollectionFilterException();
		}
		return ResponseEntity.ok(collectorService.collect());
	}
}
