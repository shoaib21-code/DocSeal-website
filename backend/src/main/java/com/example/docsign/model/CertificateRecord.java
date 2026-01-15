package com.example.docsign.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;


@Entity
@Table(name = "certificates",
       indexes = {
         @Index(name = "ix_cert_serial", columnList = "serialNumber"),
         @Index(name = "ix_cert_subject", columnList = "subjectDn")
       })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CertificateRecord {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "key_pair_id")
  private KeyPairRecord keyPair;

  @Column(length = 128)
  private String serialNumber;

  @Column(length = 512)
  private String subjectDn;

  @Column(length = 512)
  private String issuerDn;

  private Instant notBefore;
  private Instant notAfter;

  @Column(length = 1024)
  private String certificatePath;

  @Column(length = 128)
  private String sha256Fingerprint;

  @Column(name = "is_ca", nullable = false)
  private boolean ca;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @PrePersist
  void onCreate() {
    if (createdAt == null) createdAt = Instant.now();
  }

  @Transient
  public Long getKeyId() { return keyPair != null ? keyPair.getId() : null; }
}
