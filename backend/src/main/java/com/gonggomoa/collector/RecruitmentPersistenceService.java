package com.gonggomoa.collector;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gonggomoa.collector.attachment.AttachmentSizeFetcher;
import com.gonggomoa.collector.source.RecruitmentSource;
import com.gonggomoa.collector.source.SourceAttachment;
import com.gonggomoa.collector.source.SourcePosition;
import com.gonggomoa.collector.source.SourceRecruitment;
import com.gonggomoa.collector.source.SourceRecruitmentDetail;
import com.gonggomoa.institution.Institution;
import com.gonggomoa.institution.InstitutionRepository;
import com.gonggomoa.recruitment.Attachment;
import com.gonggomoa.recruitment.ExamPlan;
import com.gonggomoa.recruitment.JobCategory;
import com.gonggomoa.recruitment.Position;
import com.gonggomoa.recruitment.Recruitment;
import com.gonggomoa.recruitment.RecruitmentRepository;
import com.gonggomoa.recruitment.RecruitmentStatus;
import com.gonggomoa.recruitment.VerifyStatus;

/**
 * 공고 한 건을 실제로 비교·저장하는 계층. {@link RecruitmentCollectorService}가 페이지
 * 단위로 순회하며 이 클래스를 호출한다 — 한 건 실패가 배치 전체를 롤백하지 않도록
 * 별도 빈으로 분리해 {@code @Transactional}이 건별로 걸리게 했다.
 */
@Service
public class RecruitmentPersistenceService {

	private final RecruitmentRepository recruitmentRepository;
	private final InstitutionRepository institutionRepository;
	private final AttachmentSizeFetcher attachmentSizeFetcher;

	public RecruitmentPersistenceService(RecruitmentRepository recruitmentRepository,
			InstitutionRepository institutionRepository, AttachmentSizeFetcher attachmentSizeFetcher) {
		this.recruitmentRepository = recruitmentRepository;
		this.institutionRepository = institutionRepository;
		this.attachmentSizeFetcher = attachmentSizeFetcher;
	}

	public enum Outcome {
		CREATED, UPDATED, UNCHANGED, FAILED
	}

	@Transactional
	public Outcome processOne(RecruitmentSource source, SourceRecruitment sr) {
		SourceRecruitmentDetail detail = source.fetchDetail(sr.externalId());
		List<SourceAttachment> sourceAttachments = source.fetchAttachments(sr.externalId());

		List<AttachmentMeta> attachmentMetas = sourceAttachments.stream()
				.map(a -> new AttachmentMeta(a.fileName(), attachmentSizeFetcher.fetchSize(a.fileUrl())))
				.toList();
		String contentHash = ContentHashCalculator.calculate(sr.title(), sr.applyStartAt(), sr.applyEndAt(),
				attachmentMetas);

		return recruitmentRepository.findByExternalId(sr.externalId())
				.map(existing -> updateExisting(existing, sr, contentHash))
				.orElseGet(() -> createNew(source, sr, detail, sourceAttachments, contentHash));
	}

	private Outcome updateExisting(Recruitment existing, SourceRecruitment sr, String contentHash) {
		if (existing.getContentHash().equals(contentHash)) {
			return Outcome.UNCHANGED;
		}

		// 정정공고: 같은 external_id(=같은 source_url)인데 content_hash가 다르다.
		// 이미 VERIFIED된 시험 정보는 재검수 큐(verify_status=AUTO)로 되돌린다.
		// Position/Attachment 자체는 이번 패스에서 재동기화하지 않는다 — steps[]를
		// 안정적으로 재매칭할 키(sortNo 등)가 스키마에 없어서, 잘못 매칭해 데이터를
		// 덮어쓰는 것보다 사람이 재검수 화면에서 판단하게 두는 편이 안전하다.
		existing.updateFromReCollection(sr.title(), sr.applyStartAt(), sr.applyEndAt(),
				sr.screeningProcedureText(), contentHash, LocalDateTime.now());
		for (Position position : existing.getPositions()) {
			ExamPlan examPlan = position.getExamPlan();
			if (examPlan.getVerifyStatus() == VerifyStatus.VERIFIED) {
				examPlan.revertToAuto();
			}
		}
		recruitmentRepository.save(existing);
		return Outcome.UPDATED;
	}

	private Outcome createNew(RecruitmentSource source, SourceRecruitment sr, SourceRecruitmentDetail detail,
			List<SourceAttachment> sourceAttachments, String contentHash) {
		Institution institution = upsertInstitution(sr);

		Recruitment recruitment = Recruitment.builder()
				.institution(institution)
				.title(sr.title())
				.applyStartAt(sr.applyStartAt())
				.applyEndAt(sr.applyEndAt())
				.sourceUrl(sr.sourceUrl())
				.externalId(sr.externalId())
				.contentHash(contentHash)
				.employmentType(sr.employmentType())
				.screeningProcedureText(sr.screeningProcedureText())
				.sourceType(source.getSourceType())
				.status(determineStatus(sr.applyStartAt(), sr.applyEndAt()))
				.collectedAt(LocalDateTime.now())
				.build();

		JobCategory jobCategory = resolvePrimaryJobCategory(sr.jobCategories());
		for (SourcePosition sourcePosition : detail.positions()) {
			Position position = Position.builder()
					.recruitment(recruitment)
					.jobCategory(jobCategory)
					.headcount(sourcePosition.headcount())
					.workRegion(sr.workRegion())
					.rawPositionName(sourcePosition.rawPositionName())
					.build();
			recruitment.addPosition(position);
		}

		for (SourceAttachment sourceAttachment : sourceAttachments) {
			Attachment attachment = Attachment.builder()
					.recruitment(recruitment)
					.fileName(sourceAttachment.fileName())
					.fileUrl(sourceAttachment.fileUrl())
					.attachmentType(sourceAttachment.attachmentType())
					.mimeType(null)
					.build();
			recruitment.addAttachment(attachment);
		}

		recruitmentRepository.save(recruitment);
		return Outcome.CREATED;
	}

	private Institution upsertInstitution(SourceRecruitment sr) {
		return institutionRepository.findByExternalCode(sr.institutionExternalCode())
				.map(existing -> {
					existing.syncFrom(sr.institutionName(), sr.institutionType(), existing.getInstClsf(),
							existing.getHomepageUrl(), LocalDateTime.now());
					return institutionRepository.save(existing);
				})
				.orElseGet(() -> institutionRepository.save(Institution.builder()
						.name(sr.institutionName())
						.instType(sr.institutionType())
						.externalCode(sr.institutionExternalCode())
						.syncedAt(LocalDateTime.now())
						.build()));
	}

	private RecruitmentStatus determineStatus(LocalDate applyStartAt, LocalDate applyEndAt) {
		LocalDate today = LocalDate.now();
		if (applyStartAt != null && today.isBefore(applyStartAt)) {
			return RecruitmentStatus.UPCOMING;
		}
		if (applyEndAt != null && today.isAfter(applyEndAt)) {
			return RecruitmentStatus.CLOSED;
		}
		return RecruitmentStatus.OPEN;
	}

	/**
	 * steps[]에는 NCS 코드가 없어 직렬별 job_category를 API에서 직접 얻을 수 없다
	 * (docs/02-domain-model.md "steps[] 처리 규칙"). 공고 전체의 ncsCdLst 중 하나를
	 * 대표값으로 골라 모든 Position에 임시로 붙이고, raw_position_name을 함께 노출해
	 * 사용자가 직접 판단하게 한다.
	 */
	private JobCategory resolvePrimaryJobCategory(Set<JobCategory> jobCategories) {
		if (jobCategories.contains(JobCategory.IT)) {
			return JobCategory.IT;
		}
		if (jobCategories.contains(JobCategory.ADMIN)) {
			return JobCategory.ADMIN;
		}
		return jobCategories.stream().findFirst().orElse(JobCategory.UNKNOWN);
	}
}
