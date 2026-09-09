package com.gonggomoa.recruitment;

/**
 * API {@code files[].atchFileType} 코드와 대응한다: A=ANNOUNCEMENT, B=APPLICATION_FORM,
 * C=JOB_DESCRIPTION, Z=REFERENCE. 실제로 저장되는 것은 ANNOUNCEMENT·REFERENCE뿐이다 —
 * APPLICATION_FORM·JOB_DESCRIPTION은 수집 단계에서 필터링되어 저장되지 않는다.
 */
public enum AttachmentType {
	ANNOUNCEMENT,
	APPLICATION_FORM,
	JOB_DESCRIPTION,
	REFERENCE
}
