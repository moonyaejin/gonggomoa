package com.gonggomoa.recruitment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ncs_area_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NcsAreaItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "exam_plan_id", nullable = false)
	private ExamPlan examPlan;

	@Enumerated(EnumType.STRING)
	@Column(name = "ncs_area", nullable = false, length = 30)
	private NcsArea ncsArea;

	@Column(name = "question_count")
	private Integer questionCount;

	@Builder
	private NcsAreaItem(ExamPlan examPlan, NcsArea ncsArea, Integer questionCount) {
		this.examPlan = examPlan;
		this.ncsArea = ncsArea;
		this.questionCount = questionCount;
	}
}
