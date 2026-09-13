package com.gonggomoa.recruitment;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RecruitmentRepository extends JpaRepository<Recruitment, Long> {

	Optional<Recruitment> findByExternalId(String externalId);
}
