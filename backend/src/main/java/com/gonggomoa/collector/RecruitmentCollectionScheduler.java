package com.gonggomoa.collector;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

/**
 * 매시 정각 실행. 여러 인스턴스가 떠 있어도 ShedLock이 한 곳에서만 실행되게 막는다.
 */
@Component
public class RecruitmentCollectionScheduler {

	private final RecruitmentCollectorService collectorService;

	public RecruitmentCollectionScheduler(RecruitmentCollectorService collectorService) {
		this.collectorService = collectorService;
	}

	@Scheduled(cron = "0 0 * * * *")
	@SchedulerLock(name = "recruitment-collection", lockAtMostFor = "PT50M", lockAtLeastFor = "PT1M")
	public void run() {
		collectorService.collect();
	}
}
