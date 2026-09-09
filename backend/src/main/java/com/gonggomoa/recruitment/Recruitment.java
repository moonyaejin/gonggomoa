package com.gonggomoa.recruitment;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.gonggomoa.institution.Institution;

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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "recruitment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recruitment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "institution_id", nullable = false)
	private Institution institution;

	@Column(name = "title", nullable = false, length = 500)
	private String title;

	@Column(name = "posted_at")
	private LocalDateTime postedAt;

	@Column(name = "apply_start_at")
	private LocalDate applyStartAt;

	@Column(name = "apply_end_at")
	private LocalDate applyEndAt;

	@Column(name = "written_exam_at")
	private LocalDate writtenExamAt;

	@Column(name = "source_url", nullable = false, length = 1000)
	private String sourceUrl;

	@Column(name = "external_id", nullable = false, length = 100, unique = true)
	private String externalId;

	@Column(name = "content_hash", nullable = false, length = 64, unique = true)
	private String contentHash;

	@Enumerated(EnumType.STRING)
	@Column(name = "employment_type", nullable = false, length = 30)
	private EmploymentType employmentType;

	@Lob
	@Column(name = "screening_procedure_text", length = Integer.MAX_VALUE)
	private String screeningProcedureText;

	@Enumerated(EnumType.STRING)
	@Column(name = "source_type", nullable = false, length = 30)
	private SourceType sourceType;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private RecruitmentStatus status;

	@Column(name = "collected_at", nullable = false)
	private LocalDateTime collectedAt;

	@OneToMany(mappedBy = "recruitment", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Position> positions = new ArrayList<>();

	@OneToMany(mappedBy = "recruitment", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Attachment> attachments = new ArrayList<>();

	@Builder
	private Recruitment(Institution institution, String title, LocalDateTime postedAt, LocalDate applyStartAt,
			LocalDate applyEndAt, LocalDate writtenExamAt, String sourceUrl, String externalId, String contentHash,
			EmploymentType employmentType, String screeningProcedureText, SourceType sourceType,
			RecruitmentStatus status, LocalDateTime collectedAt) {
		this.institution = institution;
		this.title = title;
		this.postedAt = postedAt;
		this.applyStartAt = applyStartAt;
		this.applyEndAt = applyEndAt;
		this.writtenExamAt = writtenExamAt;
		this.sourceUrl = sourceUrl;
		this.externalId = externalId;
		this.contentHash = contentHash;
		this.employmentType = employmentType;
		this.screeningProcedureText = screeningProcedureText;
		this.sourceType = sourceType;
		this.status = status;
		this.collectedAt = collectedAt;
	}

	public void open() {
		this.status = RecruitmentStatus.OPEN;
	}

	public void close() {
		this.status = RecruitmentStatus.CLOSED;
	}

	public void cancel() {
		this.status = RecruitmentStatus.CANCELED;
	}

	public void updateFromReCollection(String title, LocalDate applyStartAt, LocalDate applyEndAt,
			LocalDate writtenExamAt, String screeningProcedureText, String contentHash, LocalDateTime collectedAt) {
		this.title = title;
		this.applyStartAt = applyStartAt;
		this.applyEndAt = applyEndAt;
		this.writtenExamAt = writtenExamAt;
		this.screeningProcedureText = screeningProcedureText;
		this.contentHash = contentHash;
		this.collectedAt = collectedAt;
	}
}
