package com.example.docsign.web;

import com.example.docsign.model.*;
import com.example.docsign.services.CaService;
import com.example.docsign.services.CsrService;
import com.example.docsign.services.KeyPairService;
import com.example.docsign.web.dto.CsrRequest;
import com.example.docsign.web.dto.KeyPairResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/crypto")
public class CryptoController {

  private final KeyPairService keySvc;
  private final KeyPairRecordRepository keyRepo;
  private final CsrService csrService;
  private final CsrRecordRepository csrRepo;
  private final CaService caService;
  private final CertificateRecordRepository certRepo;

  private static final String[] KU_NAMES = {
      "digitalSignature", "nonRepudiation", "keyEncipherment", "dataEncipherment",
      "keyAgreement", "keyCertSign", "cRLSign", "encipherOnly", "decipherOnly"
  };

  private static final java.util.Map<String, String> EKU_NAMES = java.util.Map.ofEntries(
      java.util.Map.entry("2.5.29.37.0", "anyExtendedKeyUsage"),
      java.util.Map.entry("1.3.6.1.5.5.7.3.1", "serverAuth"),
      java.util.Map.entry("1.3.6.1.5.5.7.3.2", "clientAuth"),
      java.util.Map.entry("1.3.6.1.5.5.7.3.3", "codeSigning"),
      java.util.Map.entry("1.3.6.1.5.5.7.3.4", "emailProtection"),
      java.util.Map.entry("1.3.6.1.5.5.7.3.8", "timeStamping"),
      java.util.Map.entry("1.3.6.1.5.5.7.3.9", "OCSPSigning"));

  // Helper to format Date as UTC ISO-8601
  private static String utc(java.util.Date d) {
    return java.time.format.DateTimeFormatter.ISO_INSTANT.format(d.toInstant());
  }

  public CryptoController(
      KeyPairService keySvc,
      KeyPairRecordRepository keyRepo,
      CsrService csrService,
      CsrRecordRepository csrRepo,
      CaService caService,
      CertificateRecordRepository certRepo) {
    this.keySvc = keySvc;
    this.keyRepo = keyRepo;
    this.csrService = csrService;
    this.csrRepo = csrRepo;
    this.caService = caService;
    this.certRepo = certRepo;
  }

  // ---- Keys

  @PostMapping("/generate-keypair")
  @ResponseStatus(HttpStatus.CREATED)
  public KeyPairResponse generate(@RequestParam(defaultValue = "2048") int bits) throws Exception {
    KeyPairRecord rec = keySvc.generateRsaAndStore(bits);
    return toDto(rec);
  }

  @GetMapping("/keys")
  public List<KeyPairResponse> list() {
    return keyRepo.findBySystemKeyFalse().stream().map(CryptoController::toDto).toList();
  }

  @GetMapping("/keys/{id}")
  public KeyPairResponse get(@PathVariable long id) {
    return toDto(keySvc.get(id));
  }

  private static KeyPairResponse toDto(KeyPairRecord r) {
    return KeyPairResponse.builder()
        .id(r.getId())
        .algorithm(r.getAlgorithm())
        .keySize(r.getKeySize())
        .createdAt(r.getCreatedAt())
        .publicKeyFingerprint(r.getPublicKeyFingerprint())
        .publicKeyPath(r.getPublicKeyPath())
        .build();
  }

  // ---- CSR

  @PostMapping("/csr")
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, Object> createCsr(@Valid @RequestBody CsrRequest req) throws Exception {
    var created = csrService.createCsr(req.getKeyId(), req.getSubjectDn());
    return req.isIncludePem()
        ? Map.of(
            "id", created.csrId(),
            "keyId", req.getKeyId(),
            "subjectDn", req.getSubjectDn(),
            "signatureAlgorithm", "SHA256withRSA",
            "csrPath", created.path(),
            "pem", created.pem())
        : Map.of(
            "id", created.csrId(),
            "keyId", req.getKeyId(),
            "subjectDn", req.getSubjectDn(),
            "signatureAlgorithm", "SHA256withRSA",
            "csrPath", created.path());
  }

  @GetMapping("/csr")
  public List<CsrDto> listAllCsrs() {
    return csrRepo.findAllByOrderByIdDesc().stream()
        .map(c -> new CsrDto(c.getId(), c.getKeyId(), c.getSubjectDn(), c.getCreatedAt()))
        .toList();
  }

  @GetMapping(value = "/csr/{id}/pem", produces = "text/plain; charset=UTF-8")
  public String getCsrPem(@PathVariable long id) throws IOException {
    var csr = csrRepo.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CSR not found"));
    Path pemPath = Path.of(csr.getCsrPath());
    if (!Files.exists(pemPath))
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "CSR PEM missing");
    return Files.readString(pemPath);
  }

  @GetMapping(value = "/csr/{id}/text", produces = "text/plain; charset=UTF-8")
  public String getCsrText(@PathVariable long id) throws Exception {
    String pem = getCsrPem(id);
    try (var rdr = new StringReader(pem); var pp = new org.bouncycastle.openssl.PEMParser(rdr)) {
      Object obj = pp.readObject();
      if (!(obj instanceof org.bouncycastle.pkcs.PKCS10CertificationRequest req))
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a PKCS#10 CSR");

      var sb = new StringBuilder();
      sb.append("Certificate Request:\n");
      sb.append("  Subject: ").append(req.getSubject()).append("\n");
      sb.append("  Signature Algorithm: ")
          .append(req.getSignatureAlgorithm().getAlgorithm().getId()).append("\n");

      var spki = req.getSubjectPublicKeyInfo();
      sb.append("  Public Key Algorithm: ")
          .append(spki.getAlgorithm().getAlgorithm().getId()).append("\n");
      return sb.toString();
    }
  }

  // ---- CA creation & issuance

  public record RootCaRequest(String subjectDn, int days) {
  }

  public record InterCaRequest(long issuerCertId, String subjectDn, int days) {
  }

  public record IssueRequest(long issuerCertId, long csrId, int days, boolean serverAuth, boolean clientAuth) {
  }

  @PostMapping("/ca/root")
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, Object> createRoot(@RequestBody RootCaRequest req) throws Exception {
    var rec = caService.createRootCaConfigured(req.subjectDn(), req.days());
    return Map.of(
        "id", rec.getId(),
        "serial", rec.getSerialNumber(),
        "subject", rec.getSubjectDn(),
        "issuer", rec.getIssuerDn(),
        "path", rec.getCertificatePath());
  }

  @PostMapping("/ca/intermediate")
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, Object> createIntermediate(@RequestBody InterCaRequest req) throws Exception {
    var rec = caService.createIntermediateCaConfigured(req.issuerCertId(), req.subjectDn(), req.days());
    return Map.of(
        "id", rec.getId(),
        "serial", rec.getSerialNumber(),
        "subject", rec.getSubjectDn(),
        "issuer", rec.getIssuerDn(),
        "path", rec.getCertificatePath());
  }

  @PostMapping("/issue")
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, Object> issue(@RequestBody IssueRequest req) throws Exception {
    var rec = caService.issueFromCsrUsingConfiguredIntermediate(req.csrId(), req.days());
    return Map.of(
        "id", rec.getId(),
        "serial", rec.getSerialNumber(),
        "subject", rec.getSubjectDn(),
        "issuer", rec.getIssuerDn(),
        "path", rec.getCertificatePath());
  }

  // ---- Certificate fetches (no chain)

  @GetMapping(value = "/certificates/{id}/pem", produces = "text/plain; charset=utf-8")
  public String downloadCertPem(@PathVariable long id) throws Exception {
    var rec = caService.getCertificate(id);
    return Files.readString(Path.of(rec.getCertificatePath()));
  }

  @GetMapping("/certificates")
  public List<CertDto> listCertificates(@RequestParam(value = "leaf", required = false) Boolean leaf) {
    List<CertificateRecord> list = Boolean.TRUE.equals(leaf)
        ? certRepo.findByCaFalseOrderByIdDesc()
        : certRepo.findAllByOrderByIdDesc();

    return list.stream()
        .map(c -> new CertDto(
            c.getId(),
            c.getSubjectDn(),
            c.getKeyId(),
            c.getNotBefore(),
            c.getNotAfter(),
            c.getCreatedAt()))
        .toList();
  }

  @GetMapping(value = "/certificates/{id}/text", produces = "text/plain; charset=UTF-8")
  public String certificateText(@PathVariable long id) throws Exception {
    String pem = downloadCertPem(id);
    var cf = java.security.cert.CertificateFactory.getInstance("X.509");
    try (var is = new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8))) {
      var x = (java.security.cert.X509Certificate) cf.generateCertificate(is);

      var sb = new StringBuilder();
      sb.append("Certificate:\n");
      sb.append("  Subject: ").append(x.getSubjectX500Principal().getName()).append("\n");
      sb.append("  Issuer : ").append(x.getIssuerX500Principal().getName()).append("\n");
      sb.append("  Serial : ").append(x.getSerialNumber().toString(16)).append("\n");
      sb.append("  Validity (UTC):\n");
      sb.append("    Not Before: ").append(utc(x.getNotBefore())).append("\n");
      sb.append("    Not After : ").append(utc(x.getNotAfter())).append("\n");
      sb.append("  Signature Algorithm: ").append(x.getSigAlgName()).append("\n");
      sb.append("  Public Key Algorithm: ").append(x.getPublicKey().getAlgorithm()).append("\n");

      // Key Usage
      boolean[] ku = x.getKeyUsage();
      if (ku != null) {
        sb.append("  Key Usage: ");
        boolean first = true;
        for (int i = 0; i < ku.length && i < KU_NAMES.length; i++) {
          if (ku[i]) {
            if (!first)
              sb.append(", ");
            sb.append(KU_NAMES[i]);
            first = false;
          }
        }
        if (first)
          sb.append("-"); // none set
        sb.append("\n");
      }

      // Extended Key Usage
      try {
        var eku = x.getExtendedKeyUsage(); // List<String> of OIDs
        if (eku != null && !eku.isEmpty()) {
          sb.append("  Extended Key Usage:\n");
          for (var oid : eku) {
            sb.append("    - ").append(EKU_NAMES.getOrDefault(oid, oid)).append("\n");
          }
        }
      } catch (java.security.cert.CertificateParsingException ignore) {
        /* no EKU */ }

      return sb.toString();
    }
  }

  public record CsrDto(long id, long keyId, String subject, Instant createdAt) {
  }

  public record CertDto(long id, String subject, Long keyId, Instant notBefore, Instant notAfter, Instant createdAt) {
  }
}
