package com.example.docsign.services;

import com.example.docsign.model.*;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;

@Service
public class CsrService {

  private final KeyPairRecordRepository keyRepo;
  private final CsrRecordRepository csrRepo;
  private final Path baseDir;

  public CsrService(KeyPairRecordRepository keyRepo,
                    CsrRecordRepository csrRepo,
                    @Value("${app.storage.base-path}") String basePath) throws IOException {
    this.keyRepo = keyRepo;
    this.csrRepo = csrRepo;
    this.baseDir = Paths.get(basePath).toAbsolutePath().normalize();
    Files.createDirectories(this.baseDir);

    // Ensure BC provider (harmless if already present)
    if (Security.getProvider("BC") == null) {
      Security.addProvider(new BouncyCastleProvider());
    }
  }

  public record CsrResult(Long csrId, String pem, String path) {}

  /**
   * Create a PKCS#10 CSR for a stored keyPair id and subject DN.
   */
  public CsrResult createCsr(long keyId, String subjectDn) throws Exception {
    var keyRec = keyRepo.findById(keyId)
        .orElseThrow(() -> new IllegalArgumentException("Key not found: " + keyId));

    // Load PRIVATE KEY (PKCS#8 PEM) from disk
    var privateKey = readPrivateKey(Paths.get(keyRec.getPrivateKeyPath()));

    // Derive public key: prefer the saved public.pem; otherwise from private CRT fields
    PublicKey publicKey = null;
    if (keyRec.getPublicKeyPath() != null) {
      try { publicKey = readPublicKey(Paths.get(keyRec.getPublicKeyPath())); } catch (Exception ignore) {}
    }
    if (publicKey == null && privateKey instanceof RSAPrivateCrtKey rsa) {
      // Rebuild public key from private key's modulus/exponent (fallback)
      var kf = KeyFactory.getInstance("RSA");
      publicKey = kf.generatePublic(new java.security.spec.RSAPublicKeySpec(rsa.getModulus(), rsa.getPublicExponent()));
    }

    if (publicKey == null) {
      throw new IllegalStateException("Unable to load/derive public key for key id " + keyId);
    }

    // Build CSR
    String sigAlg = "SHA256withRSA";
    var x500 = new X500Name(subjectDn);
    var csrBuilder = new JcaPKCS10CertificationRequestBuilder(x500, publicKey);
    ContentSigner signer = new JcaContentSignerBuilder(sigAlg)
        .setProvider("BC")
        .build(privateKey);
    PKCS10CertificationRequest csr = csrBuilder.build(signer);

    // Write PEM to disk: ./data/csr/{keyId}/<timestamp>.csr.pem
    Path csrDir = baseDir.resolve(Paths.get("csr", String.valueOf(keyId)));
    Files.createDirectories(csrDir);
    String pem = toPem("CERTIFICATE REQUEST", csr.getEncoded());
    String filename = "req-" + Instant.now().toEpochMilli() + ".csr.pem";
    Path csrPath = csrDir.resolve(filename);
    Files.writeString(csrPath, pem, StandardCharsets.UTF_8,
        StandardOpenOption.CREATE_NEW);

    // Persist CSR record
    var rec = CsrRecord.builder()
        .keyPair(keyRec)
        .subjectDn(subjectDn)
        .signatureAlgorithm(sigAlg)
        .createdAt(Instant.now())
        .csrPath(csrPath.toString())
        .build();
    rec = csrRepo.save(rec);

    return new CsrResult(rec.getId(), pem, csrPath.toString());
  }

  // --- Helpers ---

  private static PrivateKey readPrivateKey(Path pkPath) throws Exception {
    try (var reader = new BufferedReader(new FileReader(pkPath.toFile(), StandardCharsets.UTF_8))) {
      try (var pemParser = new PEMParser(reader)) {
        Object obj = pemParser.readObject();
        var converter = new JcaPEMKeyConverter().setProvider("BC");
        if (obj instanceof PEMKeyPair kp) {
          return converter.getKeyPair(kp).getPrivate();
        } else if (obj instanceof org.bouncycastle.asn1.pkcs.PrivateKeyInfo pki) {
          return converter.getPrivateKey(pki);
        } else {
          // If file contains raw base64 without labels
          String all = Files.readString(pkPath);
          String b64 = all.replaceAll("-----.*?-----", "").replaceAll("\\s+", "");
          byte[] der = Base64.getDecoder().decode(b64);
          var spec = new PKCS8EncodedKeySpec(der);
          try { return KeyFactory.getInstance("RSA").generatePrivate(spec); }
          catch (Exception e) { return KeyFactory.getInstance("EC").generatePrivate(spec); }
        }
      }
    }
  }

  private static PublicKey readPublicKey(Path pubPath) throws Exception {
    try (var reader = new BufferedReader(new FileReader(pubPath.toFile(), StandardCharsets.UTF_8))) {
      try (var pemParser = new PEMParser(reader)) {
        Object obj = pemParser.readObject();
        var converter = new JcaPEMKeyConverter().setProvider("BC");
        if (obj instanceof org.bouncycastle.asn1.x509.SubjectPublicKeyInfo spi) {
          return converter.getPublicKey(spi);
        } else {
          String all = Files.readString(pubPath);
          String b64 = all.replaceAll("-----.*?-----", "").replaceAll("\\s+", "");
          byte[] der = Base64.getDecoder().decode(b64);
          try { return KeyFactory.getInstance("RSA").generatePublic(new java.security.spec.X509EncodedKeySpec(der)); }
          catch (Exception e) { return KeyFactory.getInstance("EC").generatePublic(new java.security.spec.X509EncodedKeySpec(der)); }
        }
      }
    }
  }

  private static String toPem(String type, byte[] der) {
    String b64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(der);
    return "-----BEGIN " + type + "-----\n" + b64 + "\n-----END " + type + "-----\n";
  }
}
