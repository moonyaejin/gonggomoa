package com.gonggomoa.collector;

import org.springframework.stereotype.Component;

@Component
public class NoOpCacheInvalidator implements CacheInvalidator {

	@Override
	public void invalidateRecruitmentCaches() {
		// W5에서 Redis 캐시 무효화 구현체로 교체한다.
	}
}
