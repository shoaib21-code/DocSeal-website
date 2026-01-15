package com.example.docsign.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "key_pairs")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class KeyPairRecord {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** RSA / EC / ED25519*/
  @Column(nullable = false, length = 32)
  private String algorithm;

  /** For RSA: key size in bits (e.g., 2048, 4096). Null for EC/EdDSA. */
  private Integer keySize;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  /** Optional SHA-256 fingerprint of the public key (base64 or hex) */
  @Column(length = 128)
  private String publicKeyFingerprint;

  /** Filesystem paths where keys are stored*/
  @Column(length = 1024)
  private String publicKeyPath;

  @Column(length = 1024)
  private String privateKeyPath;
  
  @Column(nullable = false)
private boolean systemKey = false;
}
