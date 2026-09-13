package com.gonggomoa.collector;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 정정공고 감지용 해시. docs/02-domain-model.md "설계상 주의점"에 따라 공고 제목 +
 * 접수기간 + 첨부파일 메타(파일명, 크기)만 합친다 — 첨부파일 본문은 포함하지 않는다
 * (매번 다운로드해야 해서 의미가 없다).
 */
public final class ContentHashCalculator {

	private ContentHashCalculator() {
	}

	public static String calculate(String title, LocalDate applyStartAt, LocalDate applyEndAt,
			List<AttachmentMeta> attachments) {
		String attachmentPart = attachments.stream()
				.sorted(Comparator.comparing(AttachmentMeta::fileName))
				.map(a -> a.fileName() + ":" + a.sizeBytes())
				.collect(Collectors.joining(","));

		String base = String.join("|",
				nullToEmpty(title),
				nullToEmpty(applyStartAt),
				nullToEmpty(applyEndAt),
				attachmentPart);

		return sha256Hex(base);
	}

	private static String nullToEmpty(Object value) {
		return value == null ? "" : value.toString();
	}

	private static String sha256Hex(String input) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다", e);
		}
	}
}
