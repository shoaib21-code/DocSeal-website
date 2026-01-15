// src/main/java/com/example/docsign/web/VerifyController.java
package com.example.docsign.web;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.docsign.services.VerificationService;
import com.example.docsign.services.VerificationService.PdfVerifyResult;

@RestController
@RequestMapping("/api/verify")
public class VerifyController {

  private final VerificationService verifier;

  public VerifyController(VerificationService verifier) {
    this.verifier = verifier;
  }

  @PostMapping(
      value = "/pdf",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public PdfVerifyResult verifyPdf(@RequestPart("file") MultipartFile file) throws Exception {
    return verifier.verifyPdf(file.getBytes());
  }
}
