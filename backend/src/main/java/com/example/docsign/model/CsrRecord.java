package com.example.docsign.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "csrs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CsrRecord {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "key_pair_id", nullable = false)
  private KeyPairRecord keyPair;

  @Column(nullable = false, length = 512)
  private String subjectDn; // RFC4514 string: CN=...,O=...,C=...

  @Column(nullable = false, length = 128)
  private String signatureAlgorithm; // e.g., SHA256withRSA

  @Column(nullable = false)
  private Instant createdAt;

  @Column(length = 1024, nullable = false)
  private String csrPath; // path to PEM file on disk

  @Transient
  public long getKeyId() {
    return (keyPair != null) ? keyPair.getId() : 0L;
  }

  @PrePersist
  public void prePersist() {
    if (createdAt == null)
      createdAt = Instant.now();
  }

}
