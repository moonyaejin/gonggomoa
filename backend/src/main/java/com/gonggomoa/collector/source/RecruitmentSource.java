package com.gonggomoa.collector.source;

import java.time.LocalDate;
import java.util.List;

import com.gonggomoa.recruitment.SourceType;

/**
 * 채용정보 수집 소스 추상화. 각 구현체는 자기 API의 응답 구조를 이 인터페이스의 Source* DTO로
 * 변환한다 — 벤더별 원본 필드가 도메인 엔티티에 직접 새어 들어오지 않게 하기 위함이다.
 */
public interface RecruitmentSource {

	List<SourceRecruitment> fetchList(LocalDate from, LocalDate to, int page);

	SourceRecruitmentDetail fetchDetail(String externalId);

	List<SourceAttachment> fetchAttachments(String externalId);

	SourceType getSourceType();
}
