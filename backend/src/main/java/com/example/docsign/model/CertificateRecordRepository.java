
package com.example.docsign.model;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CertificateRecordRepository extends JpaRepository<CertificateRecord, Long> {
  List<CertificateRecord> findAllByOrderByIdDesc();
  List<CertificateRecord> findByCaFalseOrderByIdDesc(); // leaf only
}
