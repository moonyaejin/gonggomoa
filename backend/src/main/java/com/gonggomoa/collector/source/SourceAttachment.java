package com.gonggomoa.collector.source;

import com.gonggomoa.recruitment.AttachmentType;

public record SourceAttachment(
		String fileName,
		String fileUrl,
		AttachmentType attachmentType) {
}
