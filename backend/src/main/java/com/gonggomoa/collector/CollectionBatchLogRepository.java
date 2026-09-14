package com.gonggomoa.collector;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollectionBatchLogRepository extends JpaRepository<CollectionBatchLog, Long> {

	List<CollectionBatchLog> findAllByOrderByIdDesc(Pageable pageable);
}
