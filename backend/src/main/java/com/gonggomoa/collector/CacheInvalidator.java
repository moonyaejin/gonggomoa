package com.gonggomoa.collector;

/**
 * 수집 배치가 끝났을 때 목록·캘린더·ics 캐시를 무효화하는 자리. Redis 캐싱은 W5에서
 * 붙이므로 지금은 {@link NoOpCacheInvalidator}만 있다.
 */
public interface CacheInvalidator {

	void invalidateRecruitmentCaches();
}
