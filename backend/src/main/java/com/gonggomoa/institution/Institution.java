package com.gonggomoa.institution;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "institution")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Institution {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "name", nullable = false, length = 255)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = "inst_type", nullable = false, length = 30)
	private InstitutionType instType;

	@Column(name = "inst_clsf", length = 100)
	private String instClsf;

	@Column(name = "homepage_url", length = 500)
	private String homepageUrl;

	@Column(name = "external_code", nullable = false, length = 50, unique = true)
	private String externalCode;

	@Column(name = "synced_at", nullable = false)
	private LocalDateTime syncedAt;

	@Builder
	private Institution(String name, InstitutionType instType, String instClsf, String homepageUrl,
			String externalCode, LocalDateTime syncedAt) {
		this.name = name;
		this.instType = instType;
		this.instClsf = instClsf;
		this.homepageUrl = homepageUrl;
		this.externalCode = externalCode;
		this.syncedAt = syncedAt;
	}

	public void syncFrom(String name, InstitutionType instType, String instClsf, String homepageUrl,
			LocalDateTime syncedAt) {
		this.name = name;
		this.instType = instType;
		this.instClsf = instClsf;
		this.homepageUrl = homepageUrl;
		this.syncedAt = syncedAt;
	}
}
