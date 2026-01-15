// src/main/java/com/example/docsign/web/RestExceptionHandler.java
package com.example.docsign.web;

import jakarta.validation.ConstraintViolationException;
import org.bouncycastle.cms.CMSSignerDigestMismatchException;
import org.hibernate.LazyInitializationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.*;

@ControllerAdvice
public class RestExceptionHandler {

  /* ---------- Common response shape ---------- */

  public record ErrorPayload(
      String error,
      String hint,
      String code,
      Instant timestamp,
      Map<String, Object> meta
  ) {
    static ErrorPayload of(String error, String hint, String code) {
      return new ErrorPayload(error, hint, code, Instant.now(), null);
    }
    static ErrorPayload of(String error, String hint, String code, Map<String, Object> meta) {
      return new ErrorPayload(error, hint, code, Instant.now(), meta);
    }
  }

  private ResponseEntity<ErrorPayload> resp(HttpStatus status, String error, String hint, String code) {
    return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON)
        .body(ErrorPayload.of(error, hint, code));
  }
  private ResponseEntity<ErrorPayload> resp(HttpStatus status, String error, String hint, String code, Map<String,Object> meta) {
    return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON)
        .body(ErrorPayload.of(error, hint, code, meta));
  }

  /* ---------- Specific mappings ---------- */

  @ExceptionHandler(IllegalStateException.class)
public ResponseEntity<ErrorPayload> illegalState(IllegalStateException ex) {
  String raw   = Optional.ofNullable(ex.getMessage()).orElse("");
  String lower = raw.toLowerCase();

  // Phase-1 block: PDF already signed
  if (lower.contains("already signed")) {
    return resp(HttpStatus.BAD_REQUEST,
        "Multiple signatures are not allowed.",
        "Please upload an unsigned PDF (no existing /Sig).",
        "PDF_ALREADY_SIGNED");
  }

  // NEW: No leaf certificate for the selected key
  if (lower.contains("no leaf certificate for keyid")) {
    // extract keyId if present
    Long keyId = null;
    var m = java.util.regex.Pattern.compile("keyid\\s*=\\s*(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE)
        .matcher(raw);
    if (m.find()) keyId = Long.valueOf(m.group(1));

    Map<String,Object> meta = new LinkedHashMap<>();
    if (keyId != null) meta.put("keyId", keyId);

    return resp(HttpStatus.BAD_REQUEST,
        "No certificate is available for the selected key.",
        "Issue a certificate for this key and try again.",
        "LEAF_CERT_MISSING",
        meta);
  }

  // Fallback
  return resp(HttpStatus.BAD_REQUEST,
      "Request cannot be processed.",
      raw.isBlank() ? null : raw,
      "ILLEGAL_STATE");
}

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorPayload> illegalArgument(IllegalArgumentException ex) {
    String raw = Optional.ofNullable(ex.getMessage()).orElse("");
    String lower = raw.toLowerCase();
    if (lower.contains("key not found"))
      return resp(HttpStatus.NOT_FOUND, "Key not found.", "Pick a valid key or create one.", "KEY_NOT_FOUND");
    if (lower.contains("cert not found"))
      return resp(HttpStatus.NOT_FOUND, "Certificate not found.", "Pick a valid certificate.", "CERT_NOT_FOUND");
    if (lower.contains("csr not found"))
      return resp(HttpStatus.NOT_FOUND, "CSR not found.", "Create a CSR first or choose a valid CSR.", "CSR_NOT_FOUND");
    return resp(HttpStatus.BAD_REQUEST, "Invalid input.", raw.isBlank() ? null : raw, "BAD_INPUT");
  }

  @ExceptionHandler(MissingServletRequestPartException.class)
  public ResponseEntity<ErrorPayload> missingPart(MissingServletRequestPartException ex) {
    String name = ex.getRequestPartName();
    return resp(HttpStatus.BAD_REQUEST,
        "Required upload part is missing.",
        "Missing part: " + name + ". Ensure the form includes it.",
        "MISSING_PART",
        Map.of("part", name));
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ErrorPayload> badMedia(HttpMediaTypeNotSupportedException ex) {
    return resp(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
        "Unsupported content type.",
        "Use 'multipart/form-data' for file uploads.",
        "UNSUPPORTED_MEDIA");
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ErrorPayload> maxUpload(MaxUploadSizeExceededException ex) {
    return resp(HttpStatus.PAYLOAD_TOO_LARGE,
        "File too large.",
        "Try a smaller PDF or raise 'spring.servlet.multipart.max-file-size'.",
        "FILE_TOO_LARGE");
  }

  @ExceptionHandler(MultipartException.class)
  public ResponseEntity<ErrorPayload> multipart(MultipartException ex) {
    return resp(HttpStatus.BAD_REQUEST,
        "Failed to process uploaded file.",
        "Re-upload the PDF. If the error persists, check server logs.",
        "MULTIPART_ERROR");
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorPayload> invalid(MethodArgumentNotValidException ex) {
    Map<String, Object> meta = new LinkedHashMap<>();
    List<Map<String, String>> fields = new ArrayList<>();
    for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
      fields.add(Map.of("field", fe.getField(), "message", fe.getDefaultMessage()));
    }
    meta.put("fields", fields);
    return resp(HttpStatus.BAD_REQUEST,
        "Validation failed.",
        "Fix highlighted fields and try again.",
        "VALIDATION_ERROR",
        meta);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorPayload> constraint(jakarta.validation.ConstraintViolationException ex) {
    return resp(HttpStatus.BAD_REQUEST,
        "Validation failed.",
        ex.getMessage(),
        "CONSTRAINT_VIOLATION");
  }

  @ExceptionHandler(FileNotFoundException.class)
  public ResponseEntity<ErrorPayload> notFoundFs(FileNotFoundException ex) {
    return resp(HttpStatus.NOT_FOUND,
        "Required file is missing on server.",
        "Re-create the item or contact admin if it should exist.",
        "FILE_NOT_FOUND");
  }

  @ExceptionHandler(IOException.class)
  public ResponseEntity<ErrorPayload> io(IOException ex) {
    return resp(HttpStatus.INTERNAL_SERVER_ERROR,
        "I/O error while processing your file.",
        "Try again. If it persists, check server disk and permissions.",
        "IO_ERROR");
  }

  @ExceptionHandler(CMSSignerDigestMismatchException.class)
  public ResponseEntity<ErrorPayload> cmsDigestMismatch(CMSSignerDigestMismatchException ex) {
    return resp(HttpStatus.BAD_REQUEST,
        "Signature is invalid for the current PDF content.",
        "The PDF may have been modified after signing.",
        "CMS_DIGEST_MISMATCH");
  }

  @ExceptionHandler(GeneralSecurityException.class)
  public ResponseEntity<ErrorPayload> security(GeneralSecurityException ex) {
    return resp(HttpStatus.BAD_REQUEST,
        "Cryptographic operation failed.",
        "Check algorithms & key/cert compatibility.",
        "CRYPTO_ERROR");
  }

  @ExceptionHandler(LazyInitializationException.class)
  public ResponseEntity<ErrorPayload> lazy(LazyInitializationException ex) {
    return resp(HttpStatus.INTERNAL_SERVER_ERROR,
        "Server couldn’t complete the request.",
        "Please retry. If it persists, contact admin.",
        "SERVER_LAZY_LOADING");
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorPayload> unknown(Exception ex) {
    return resp(HttpStatus.INTERNAL_SERVER_ERROR,
        "Unexpected server error.",
        "Please retry in a moment.",
        "UNEXPECTED_ERROR");
  }
}
