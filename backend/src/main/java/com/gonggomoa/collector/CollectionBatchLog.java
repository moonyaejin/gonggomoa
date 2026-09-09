package com.gonggomoa.collector;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "collection_batch_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CollectionBatchLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "started_at", nullable = false)
	private LocalDateTime startedAt;

	@Column(name = "duration_ms")
	private Integer durationMs;

	@Column(name = "fetched_count")
	private Integer fetchedCount;

	@Column(name = "new_count")
	private Integer newCount;

	@Column(name = "updated_count")
	private Integer updatedCount;

	@Column(name = "success", nullable = false)
	private boolean success;

	@Column(name = "error_message", length = 1000)
	private String errorMessage;

	@Builder
	private CollectionBatchLog(LocalDateTime startedAt, Integer durationMs, Integer fetchedCount, Integer newCount,
			Integer updatedCount, boolean success, String errorMessage) {
		this.startedAt = startedAt;
		this.durationMs = durationMs;
		this.fetchedCount = fetchedCount;
		this.newCount = newCount;
		this.updatedCount = updatedCount;
		this.success = success;
		this.errorMessage = errorMessage;
	}
}
