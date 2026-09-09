package com.gonggomoa.collector.source.moef;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 모집단위 x 전형단계로 펼쳐진 한 행. 같은 모집단위가 {@code sortNo}당 여러 행 반복되므로
 * {@code sortNo}로 그룹핑해 중복을 제거해야 한다 (docs/02-domain-model.md "steps[] 처리 규칙").
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MoefStep(Integer sortNo, String recrutPbancTtl, Integer recrutNope, String cmpttRt) {
}
