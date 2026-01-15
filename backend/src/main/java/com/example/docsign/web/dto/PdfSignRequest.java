package com.example.docsign.web.dto;

import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.constraints.NotNull;

public record PdfSignRequest(
    @NotNull MultipartFile pdfFile,
    String keyRef,
    String certRef,
    String hashAlgo,   // default SHA-256
    String sigAlgo     // default RSA-PSS | RSA
) {}
