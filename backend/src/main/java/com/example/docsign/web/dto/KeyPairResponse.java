package com.example.docsign.web.dto;

import lombok.*;
import java.time.Instant;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KeyPairResponse {
  private Long id;
  private String algorithm;
  private Integer keySize;
  private Instant createdAt;
  private String publicKeyFingerprint;
  private String publicKeyPath; 
}
