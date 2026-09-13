package com.gonggomoa.collector;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class ContentHashCalculatorTest {

	@Test
	void sameInputsProduceSameHash() {
		List<AttachmentMeta> attachments = List.of(new AttachmentMeta("공고문.pdf", 1024L));
		String hash1 = ContentHashCalculator.calculate("제목", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30),
				attachments);
		String hash2 = ContentHashCalculator.calculate("제목", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30),
				attachments);

		assertThat(hash1).isEqualTo(hash2);
	}

	@Test
	void attachmentOrderDoesNotAffectHash() {
		List<AttachmentMeta> orderA = List.of(
				new AttachmentMeta("공고문.pdf", 1024L),
				new AttachmentMeta("참고자료.pdf", 2048L));
		List<AttachmentMeta> orderB = List.of(
				new AttachmentMeta("참고자료.pdf", 2048L),
				new AttachmentMeta("공고문.pdf", 1024L));

		String hashA = ContentHashCalculator.calculate("제목", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30),
				orderA);
		String hashB = ContentHashCalculator.calculate("제목", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30),
				orderB);

		assertThat(hashA).isEqualTo(hashB);
	}

	@Test
	void attachmentSizeChangeAloneProducesDifferentHash() {
		String before = ContentHashCalculator.calculate("제목", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30),
				List.of(new AttachmentMeta("참고자료.pdf", 2048L)));
		String after = ContentHashCalculator.calculate("제목", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30),
				List.of(new AttachmentMeta("참고자료.pdf", 4096L)));

		assertThat(before).isNotEqualTo(after);
	}

	@Test
	void titleChangeProducesDifferentHash() {
		List<AttachmentMeta> attachments = List.of();
		String before = ContentHashCalculator.calculate("제목1", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30),
				attachments);
		String after = ContentHashCalculator.calculate("제목2", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30),
				attachments);

		assertThat(before).isNotEqualTo(after);
	}
}
