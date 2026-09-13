package com.gonggomoa.institution;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InstitutionRepository extends JpaRepository<Institution, Long> {

	Optional<Institution> findByExternalCode(String externalCode);
}
