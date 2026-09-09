package com.gonggomoa.recruitment;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "position")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Position {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "recruitment_id", nullable = false)
	private Recruitment recruitment;

	@Enumerated(EnumType.STRING)
	@Column(name = "job_category", nullable = false, length = 30)
	private JobCategory jobCategory;

	@Column(name = "headcount")
	private Integer headcount;

	@Column(name = "work_region", length = 255)
	private String workRegion;

	@OneToOne(mappedBy = "position", cascade = CascadeType.ALL, orphanRemoval = true)
	private ExamPlan examPlan;

	@Builder
	private Position(Recruitment recruitment, JobCategory jobCategory, Integer headcount, String workRegion,
			String rawPositionName) {
		this.recruitment = recruitment;
		this.jobCategory = jobCategory;
		this.headcount = headcount;
		this.workRegion = workRegion;
		this.examPlan = ExamPlan.createInitial(this, rawPositionName);
	}
}
