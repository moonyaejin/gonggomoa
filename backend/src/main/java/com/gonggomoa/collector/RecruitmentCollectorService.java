package com.gonggomoa.collector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.gonggomoa.collector.source.RecruitmentSource;
import com.gonggomoa.collector.source.SourceRecruitment;

/**
 * 수집 배치의 진입점. {@link RecruitmentSource} 목록을 순회하며 목록을 페이지 단위로
 * 끝까지 가져오고, 공고별 저장은 {@link RecruitmentPersistenceService}에 위임한다.
 * 건별로 트랜잭션을 분리하기 위해 저장 로직을 별도 빈으로 뒀다 — 이 클래스 안에서
 * {@code @Transactional} 메서드를 self-invocation하면 프록시를 타지 않아 적용되지
 * 않는다.
 */
@Service
public class RecruitmentCollectorService {

	private static final Logger log = LoggerFactory.getLogger(RecruitmentCollectorService.class);

	private final List<RecruitmentSource> sources;
	private final RecruitmentPersistenceService persistenceService;
	private final CollectionBatchLogRepository batchLogRepository;
	private final CacheInvalidator cacheInvalidator;
	private final BatchFailureAlertService batchFailureAlertService;
	private final AtomicBoolean running = new AtomicBoolean(false);

	public RecruitmentCollectorService(List<RecruitmentSource> sources,
			RecruitmentPersistenceService persistenceService, CollectionBatchLogRepository batchLogRepository,
			CacheInvalidator cacheInvalidator, BatchFailureAlertService batchFailureAlertService) {
		this.sources = sources;
		this.persistenceService = persistenceService;
		this.batchLogRepository = batchLogRepository;
		this.cacheInvalidator = cacheInvalidator;
		this.batchFailureAlertService = batchFailureAlertService;
	}

	/**
	 * 매시 정각 스케줄러와 관리자 수동 실행(POST /api/admin/collect)이 겹치지 않도록
	 * 같은 JVM 안에서는 항상 이 메서드를 통해서만 배치를 돈다. 스케줄러는 ShedLock으로
	 * 여러 인스턴스 간 중복 실행을 막고, 이 플래그는 같은 인스턴스 안에서의 동시 실행을
	 * 막는다.
	 */
	public CollectionResult collect() {
		if (!running.compareAndSet(false, true)) {
			throw new CollectionInProgressException();
		}
		try {
			return runCollection();
		} finally {
			running.set(false);
		}
	}

	private CollectionResult runCollection() {
		CollectionBatchLog batchLog = CollectionBatchLog.start();
		batchLogRepository.save(batchLog);

		int fetched = 0;
		int created = 0;
		int updated = 0;
		try {
			for (RecruitmentSource source : sources) {
				for (SourceRecruitment sourceRecruitment : fetchAllPages(source)) {
					fetched++;
					switch (processOneSafely(source, sourceRecruitment)) {
						case CREATED -> created++;
						case UPDATED -> updated++;
						case UNCHANGED, FAILED -> {
						}
					}
				}
			}
			batchLog.complete(fetched, created, updated);
			log.info("수집 배치 완료 — 조회 {}건, 신규 {}건, 갱신 {}건", fetched, created, updated);
			return new CollectionResult(fetched, created, updated, true, null);
		} catch (Exception e) {
			log.error("수집 배치 실패", e);
			String errorMessage = truncate(e.getMessage());
			batchLog.fail(fetched, created, updated, errorMessage);
			return new CollectionResult(fetched, created, updated, false, errorMessage);
		} finally {
			batchLogRepository.save(batchLog);
			cacheInvalidator.invalidateRecruitmentCaches();
			if (!batchLog.isSuccess()) {
				batchFailureAlertService.alertIfConsecutiveFailureThresholdReached(batchLog);
			}
		}
	}

	private List<SourceRecruitment> fetchAllPages(RecruitmentSource source) {
		List<SourceRecruitment> all = new ArrayList<>();
		int page = 1;
		while (true) {
			List<SourceRecruitment> pageItems = source.fetchList(null, null, page);
			if (pageItems.isEmpty()) {
				break;
			}
			all.addAll(pageItems);
			page++;
		}
		return all;
	}

	private RecruitmentPersistenceService.Outcome processOneSafely(RecruitmentSource source,
			SourceRecruitment sourceRecruitment) {
		try {
			return persistenceService.processOne(source, sourceRecruitment);
		} catch (Exception e) {
			log.warn("공고 처리 실패 (externalId={}): {}", sourceRecruitment.externalId(), e.getMessage(), e);
			return RecruitmentPersistenceService.Outcome.FAILED;
		}
	}

	private String truncate(String message) {
		if (message == null) {
			return null;
		}
		return message.length() > 1000 ? message.substring(0, 1000) : message;
	}
}
