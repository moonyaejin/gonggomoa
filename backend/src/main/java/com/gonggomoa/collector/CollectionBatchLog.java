package com.gonggomoa.collector;

import java.time.Duration;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
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

	private CollectionBatchLog(LocalDateTime startedAt) {
		this.startedAt = startedAt;
		this.success = false;
	}

	public static CollectionBatchLog start() {
		return new CollectionBatchLog(LocalDateTime.now());
	}

	public void complete(int fetchedCount, int newCount, int updatedCount) {
		this.durationMs = elapsedMs();
		this.fetchedCount = fetchedCount;
		this.newCount = newCount;
		this.updatedCount = updatedCount;
		this.success = true;
	}

	public void fail(int fetchedCount, int newCount, int updatedCount, String errorMessage) {
		this.durationMs = elapsedMs();
		this.fetchedCount = fetchedCount;
		this.newCount = newCount;
		this.updatedCount = updatedCount;
		this.success = false;
		this.errorMessage = errorMessage;
	}

	private int elapsedMs() {
		return (int) Duration.between(startedAt, LocalDateTime.now()).toMillis();
	}
}
