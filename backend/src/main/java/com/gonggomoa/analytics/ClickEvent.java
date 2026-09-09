package com.gonggomoa.analytics;

import java.time.LocalDate;

import com.gonggomoa.recruitment.Recruitment;

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

/**
 * 익명 클릭 카운터. 개인정보(IP, 세션 등)를 저장하지 않고 (공고, 이벤트 종류, 일자) 단위로 집계만 한다.
 */
@Entity
@Table(name = "click_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClickEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "recruitment_id", nullable = false)
	private Recruitment recruitment;

	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", nullable = false, length = 30)
	private ClickEventType eventType;

	@Column(name = "occurred_on", nullable = false)
	private LocalDate occurredOn;

	@Column(name = "count", nullable = false)
	private int count;

	@Builder
	private ClickEvent(Recruitment recruitment, ClickEventType eventType, LocalDate occurredOn) {
		this.recruitment = recruitment;
		this.eventType = eventType;
		this.occurredOn = occurredOn;
		this.count = 1;
	}

	public void increment() {
		this.count++;
	}
}
