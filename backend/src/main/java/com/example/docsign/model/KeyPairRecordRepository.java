package com.example.docsign.model;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface KeyPairRecordRepository extends JpaRepository<KeyPairRecord, Long> {
    List<KeyPairRecord> findBySystemKeyFalse();

    // (optional) generic filter
    List<KeyPairRecord> findBySystemKey(boolean systemKey);
}
