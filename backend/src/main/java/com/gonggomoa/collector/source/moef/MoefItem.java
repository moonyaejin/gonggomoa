package com.gonggomoa.collector.source.moef;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * {@code /list}, {@code /detail}이 공유하는 채용공고 항목 원본 구조. {@code /list}에서는
 * {@code files}, {@code steps}가 항상 빈 배열로 온다 (docs/02-domain-model.md 참고).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MoefItem(
		String recrutPblntSn,
		String pblntInstCd,
		String instNm,
		String recrutPbancTtl,
		String pbancBgngYmd,
		String pbancEndYmd,
		String srcUrl,
		String ncsCdLst,
		String hireTypeLst,
		String workRgnNmLst,
		Integer recrutNope,
		String scrnprcdrMthdExpln,
		String ongoingYn,
		List<MoefFile> files,
		List<MoefStep> steps) {
}
