package com.gonggomoa.collector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gonggomoa.collector.attachment.AttachmentSizeFetcher;
import com.gonggomoa.collector.source.RecruitmentSource;
import com.gonggomoa.collector.source.SourceAttachment;
import com.gonggomoa.collector.source.SourcePosition;
import com.gonggomoa.collector.source.SourceRecruitment;
import com.gonggomoa.collector.source.SourceRecruitmentDetail;
import com.gonggomoa.institution.Institution;
import com.gonggomoa.institution.InstitutionRepository;
import com.gonggomoa.institution.InstitutionType;
import com.gonggomoa.recruitment.AttachmentType;
import com.gonggomoa.recruitment.EmploymentType;
import com.gonggomoa.recruitment.ExamPlan;
import com.gonggomoa.recruitment.JobCategory;
import com.gonggomoa.recruitment.Position;
import com.gonggomoa.recruitment.Recruitment;
import com.gonggomoa.recruitment.RecruitmentRepository;
import com.gonggomoa.recruitment.RecruitmentStatus;
import com.gonggomoa.recruitment.SourceType;
import com.gonggomoa.recruitment.VerifyStatus;

@ExtendWith(MockitoExtension.class)
class RecruitmentPersistenceServiceTest {

	@Mock
	private RecruitmentRepository recruitmentRepository;

	@Mock
	private InstitutionRepository institutionRepository;

	@Mock
	private AttachmentSizeFetcher attachmentSizeFetcher;

	@Mock
	private RecruitmentSource source;

	private RecruitmentPersistenceService service() {
		return new RecruitmentPersistenceService(recruitmentRepository, institutionRepository,
				attachmentSizeFetcher);
	}

	private SourceRecruitment sourceRecruitment() {
		return new SourceRecruitment(
				"1", "COD1", "기관A", InstitutionType.A2001, "전산직 채용",
				LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), "https://example.org/1",
				Set.of(JobCategory.IT), EmploymentType.FULL_TIME, "서울", 5, "필기시험 실시");
	}

	@Test
	void processOne_createsNewRecruitmentWithPositionsAndAttachments() {
		when(recruitmentRepository.findByExternalId("1")).thenReturn(Optional.empty());
		when(institutionRepository.findByExternalCode("COD1")).thenReturn(Optional.empty());
		when(institutionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(recruitmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(source.fetchDetail("1")).thenReturn(new SourceRecruitmentDetail("1", List.of(
				new SourcePosition(0, "전산(공개경쟁채용)", null, null),
				new SourcePosition(1, "행정(공개경쟁채용)", null, null))));
		when(source.fetchAttachments("1")).thenReturn(List.of(
				new SourceAttachment("공고문.pdf", "https://x/1", AttachmentType.ANNOUNCEMENT)));
		when(attachmentSizeFetcher.fetchSize(any())).thenReturn(1024L);
		when(source.getSourceType()).thenReturn(SourceType.MOEF);

		RecruitmentPersistenceService.Outcome outcome = service().processOne(source, sourceRecruitment());

		assertThat(outcome).isEqualTo(RecruitmentPersistenceService.Outcome.CREATED);

		ArgumentCaptor<Recruitment> captor = ArgumentCaptor.forClass(Recruitment.class);
		verify(recruitmentRepository).save(captor.capture());
		Recruitment saved = captor.getValue();
		assertThat(saved.getPositions()).hasSize(2);
		assertThat(saved.getAttachments()).hasSize(1);
		assertThat(saved.getContentHash()).isNotBlank();
		assertThat(saved.getPositions().get(0).getExamPlan().getVerifyStatus()).isEqualTo(VerifyStatus.AUTO);
	}

	@Test
	void processOne_skipsWhenContentHashUnchanged() {
		String hash = ContentHashCalculator.calculate("전산직 채용", LocalDate.of(2026, 9, 1),
				LocalDate.of(2026, 9, 30), List.of(new AttachmentMeta("공고문.pdf", 1024L)));

		Recruitment existing = Recruitment.builder()
				.institution(institution())
				.title("전산직 채용")
				.applyStartAt(LocalDate.of(2026, 9, 1))
				.applyEndAt(LocalDate.of(2026, 9, 30))
				.sourceUrl("https://example.org/1")
				.externalId("1")
				.contentHash(hash)
				.employmentType(EmploymentType.FULL_TIME)
				.sourceType(SourceType.MOEF)
				.status(RecruitmentStatus.OPEN)
				.collectedAt(LocalDateTime.now())
				.build();

		when(recruitmentRepository.findByExternalId("1")).thenReturn(Optional.of(existing));
		when(source.fetchDetail("1")).thenReturn(new SourceRecruitmentDetail("1", List.of()));
		when(source.fetchAttachments("1")).thenReturn(List.of(
				new SourceAttachment("공고문.pdf", "https://x/1", AttachmentType.ANNOUNCEMENT)));
		when(attachmentSizeFetcher.fetchSize(any())).thenReturn(1024L);

		RecruitmentPersistenceService.Outcome outcome = service().processOne(source, sourceRecruitment());

		assertThat(outcome).isEqualTo(RecruitmentPersistenceService.Outcome.UNCHANGED);
		verify(recruitmentRepository, never()).save(any());
	}

	@Test
	void processOne_revertsVerifiedExamPlanToAutoWhenHashChanges() {
		Recruitment existing = Recruitment.builder()
				.institution(institution())
				.title("전산직 채용 (구버전)")
				.applyStartAt(LocalDate.of(2026, 9, 1))
				.applyEndAt(LocalDate.of(2026, 9, 30))
				.sourceUrl("https://example.org/1")
				.externalId("1")
				.contentHash("old-hash-that-will-not-match")
				.employmentType(EmploymentType.FULL_TIME)
				.sourceType(SourceType.MOEF)
				.status(RecruitmentStatus.OPEN)
				.collectedAt(LocalDateTime.now())
				.build();
		Position position = Position.builder()
				.recruitment(existing)
				.jobCategory(JobCategory.IT)
				.rawPositionName("전산(공개경쟁채용)")
				.build();
		existing.addPosition(position);
		ExamPlan examPlan = position.getExamPlan();
		examPlan.markVerified();

		when(recruitmentRepository.findByExternalId("1")).thenReturn(Optional.of(existing));
		when(recruitmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(source.fetchDetail("1")).thenReturn(new SourceRecruitmentDetail("1", List.of()));
		when(source.fetchAttachments("1")).thenReturn(List.of());

		RecruitmentPersistenceService.Outcome outcome = service().processOne(source, sourceRecruitment());

		assertThat(outcome).isEqualTo(RecruitmentPersistenceService.Outcome.UPDATED);
		assertThat(examPlan.getVerifyStatus()).isEqualTo(VerifyStatus.AUTO);
		assertThat(existing.getTitle()).isEqualTo("전산직 채용");
	}

	private Institution institution() {
		return Institution.builder()
				.name("기관A")
				.instType(InstitutionType.A2001)
				.externalCode("COD1")
				.syncedAt(LocalDateTime.now())
				.build();
	}
}
