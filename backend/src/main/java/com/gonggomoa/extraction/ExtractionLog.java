package com.gonggomoa.extraction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.gonggomoa.recruitment.Recruitment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "extraction_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExtractionLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "recruitment_id", nullable = false)
	private Recruitment recruitment;

	@Column(name = "attempted_at", nullable = false)
	private LocalDateTime attemptedAt;

	@Column(name = "duration_ms")
	private Integer durationMs;

	@Column(name = "confidence", precision = 3, scale = 2)
	private BigDecimal confidence;

	@Column(name = "auto_approved", nullable = false)
	private boolean autoApproved;

	@Column(name = "corrected_field_count")
	private Integer correctedFieldCount;

	@Column(name = "model_id", length = 100)
	private String modelId;

	@Column(name = "failure_reason", length = 500)
	private String failureReason;

	@Builder
	private ExtractionLog(Recruitment recruitment, LocalDateTime attemptedAt, Integer durationMs,
			BigDecimal confidence, boolean autoApproved, Integer correctedFieldCount, String modelId,
			String failureReason) {
		this.recruitment = recruitment;
		this.attemptedAt = attemptedAt;
		this.durationMs = durationMs;
		this.confidence = confidence;
		this.autoApproved = autoApproved;
		this.correctedFieldCount = correctedFieldCount;
		this.modelId = modelId;
		this.failureReason = failureReason;
	}
}
