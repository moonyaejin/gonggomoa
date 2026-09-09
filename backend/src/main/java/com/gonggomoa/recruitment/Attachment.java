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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "attachment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Attachment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "recruitment_id", nullable = false)
	private Recruitment recruitment;

	@Column(name = "file_name", nullable = false, length = 255)
	private String fileName;

	@Column(name = "file_url", nullable = false, length = 1000)
	private String fileUrl;

	@Enumerated(EnumType.STRING)
	@Column(name = "attachment_type", nullable = false, length = 30)
	private AttachmentType attachmentType;

	@Column(name = "mime_type", length = 100)
	private String mimeType;

	@Lob
	@Column(name = "extracted_text", length = Integer.MAX_VALUE)
	private String extractedText;

	@Enumerated(EnumType.STRING)
	@Column(name = "extract_status", nullable = false, length = 30)
	private ExtractStatus extractStatus;

	@Builder
	private Attachment(Recruitment recruitment, String fileName, String fileUrl, AttachmentType attachmentType,
			String mimeType) {
		this.recruitment = recruitment;
		this.fileName = fileName;
		this.fileUrl = fileUrl;
		this.attachmentType = attachmentType;
		this.mimeType = mimeType;
		this.extractStatus = ExtractStatus.PENDING;
	}

	public void markExtracted(String extractedText) {
		this.extractedText = extractedText;
		this.extractStatus = ExtractStatus.SUCCEEDED;
	}

	public void markExtractionFailed() {
		this.extractStatus = ExtractStatus.FAILED;
	}
}
