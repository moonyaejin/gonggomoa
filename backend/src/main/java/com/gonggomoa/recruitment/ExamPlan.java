package com.gonggomoa.recruitment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "exam_plan")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExamPlan {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "position_id", nullable = false, unique = true)
	private Position position;

	@Column(name = "has_written_exam")
	private Boolean hasWrittenExam;

	@Column(name = "written_exam_date")
	private LocalDate writtenExamDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "exam_type", length = 30)
	private ExamType examType;

	@Column(name = "major_subjects", length = 500)
	private String majorSubjects;

	@Column(name = "raw_position_name", length = 255)
	private String rawPositionName;

	@Column(name = "total_questions")
	private Integer totalQuestions;

	@Column(name = "time_limit_minutes")
	private Integer timeLimitMinutes;

	@Column(name = "ncs_areas_confirmed", nullable = false)
	private boolean ncsAreasConfirmed;

	@Enumerated(EnumType.STRING)
	@Column(name = "verify_status", nullable = false, length = 30)
	private VerifyStatus verifyStatus;

	@Column(name = "confidence", precision = 3, scale = 2)
	private BigDecimal confidence;

	@Column(name = "verified_at")
	private LocalDateTime verifiedAt;

	@OneToMany(mappedBy = "examPlan", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<NcsAreaItem> ncsAreaItems = new ArrayList<>();

	private ExamPlan(Position position, String rawPositionName) {
		this.position = position;
		this.rawPositionName = rawPositionName;
		this.ncsAreasConfirmed = false;
		this.verifyStatus = VerifyStatus.AUTO;
	}

	static ExamPlan createInitial(Position position, String rawPositionName) {
		return new ExamPlan(position, rawPositionName);
	}

	public void applyExtractionResult(Boolean hasWrittenExam, LocalDate writtenExamDate, ExamType examType,
			String majorSubjects, Integer totalQuestions, Integer timeLimitMinutes, boolean ncsAreasConfirmed,
			BigDecimal confidence) {
		this.hasWrittenExam = hasWrittenExam;
		this.writtenExamDate = writtenExamDate;
		this.examType = examType;
		this.majorSubjects = majorSubjects;
		this.totalQuestions = totalQuestions;
		this.timeLimitMinutes = timeLimitMinutes;
		this.ncsAreasConfirmed = ncsAreasConfirmed;
		this.confidence = confidence;
	}

	/**
	 * screening_procedure_text 1차 판정만으로 필기전형이 없다고 확정된 경우 사용한다.
	 * LLM 추출 없이도 확실한 사실이므로 검수 대기(AUTO)로 남기지 않고 즉시 VERIFIED
	 * 처리한다 — 그렇지 않으면 필기 없는 공고까지 검수 큐에 쌓여 "하루 5분" 전제가 깨진다.
	 */
	public void confirmNoWrittenExam() {
		this.hasWrittenExam = false;
		this.ncsAreasConfirmed = false;
		this.verifyStatus = VerifyStatus.VERIFIED;
		this.verifiedAt = LocalDateTime.now();
	}

	public void markVerified() {
		this.verifyStatus = VerifyStatus.VERIFIED;
		this.verifiedAt = LocalDateTime.now();
	}

	public void markUncertain() {
		this.verifyStatus = VerifyStatus.UNCERTAIN;
		this.verifiedAt = null;
	}

	public void revertToAuto() {
		this.verifyStatus = VerifyStatus.AUTO;
		this.verifiedAt = null;
	}
}
