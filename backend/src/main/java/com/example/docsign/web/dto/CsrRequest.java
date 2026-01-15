package com.example.docsign.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CsrRequest {
  @Min(1)
  private long keyId;

  /** RFC4514 string, e.g., "CN=Alice,O=DocSeal,L=Hyderabad,ST=TS,C=IN" */
  @NotBlank
  private String subjectDn;

  /** Optional: return the PEM inline (default true). If false, returns only path/id. */
  private boolean includePem = true;
}

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
class CsrResponse {
  private long id;
  private long keyId;
  private String subjectDn;
  private String signatureAlgorithm;
  private String csrPath;
  private String pem; // null if not requested
}
