package com.example.docsign.model;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CsrRecordRepository extends JpaRepository<CsrRecord, Long> {
  List<CsrRecord> findByKeyPairId(Long keyPairId);
  List<CsrRecord> findAllByOrderByIdDesc();
}
