package com.example.docsign.services;

import com.example.docsign.model.*;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;


@Service
public class CaService {

  private final KeyPairRecordRepository keyRepo;
  private final CertificateRecordRepository certRepo;
  private final CsrRecordRepository csrRepo;
  private final Path baseDir;
  private final SecureRandom rng = new SecureRandom();

  private final long rootKeyId;
  private final long interKeyId;

  private final Path interKeyPath;
  private final Path interCertPath;
  private final Path rootKeyPath;   
  private final Path rootCertPath;  

  public CaService(
      KeyPairRecordRepository keyRepo,
      CertificateRecordRepository certRepo,
      CsrRecordRepository csrRepo,
      @Value("${app.storage.base-path}") String basePath,
      @Value("${app.ca.root.key-id}") long rootKeyId,
      @Value("${app.ca.intermediate.key-id}") long interKeyId,
      @Value("${app.ca.intermediate.key-path}") String interKeyPathStr,
      @Value("${app.ca.intermediate.cert-path}") String interCertPathStr,
      @Value("${app.ca.root.key-path}") String rootKeyPathStr,
      @Value("${app.ca.root.cert-path}") String rootCertPathStr
  ) throws IOException {
    this.keyRepo = keyRepo;
    this.certRepo = certRepo;
    this.csrRepo = csrRepo;

    this.baseDir = Paths.get(basePath).toAbsolutePath().normalize();
    Files.createDirectories(this.baseDir);

    if (Security.getProvider("BC") == null) {
      Security.addProvider(new BouncyCastleProvider());
    }

    this.rootKeyId = rootKeyId;
    this.interKeyId = interKeyId;

    this.interKeyPath  = resolvePath(interKeyPathStr);
    this.interCertPath = resolvePath(interCertPathStr);
    this.rootKeyPath   = resolvePath(rootKeyPathStr);
    this.rootCertPath  = resolvePath(rootCertPathStr);

    if (!Files.exists(this.interKeyPath))
      throw new FileNotFoundException("Intermediate key not found: " + this.interKeyPath);
    if (!Files.exists(this.interCertPath))
      throw new FileNotFoundException("Intermediate cert not found: " + this.interCertPath);
  }

  public CertificateRecord getCertificate(long id) {
    return certRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Cert not found: " + id));
  }

  public CertificateRecord issueFromCsrUsingConfiguredIntermediate(long csrId, int days) throws Exception {
    return issueFromCsr(csrId, days);
  }

  public CertificateRecord createRootCaConfigured(String subjectDn, int days) throws Exception {
    return createRootCa(this.rootKeyId, subjectDn, days);
  }

  public CertificateRecord createIntermediateCaConfigured(long issuerCertId, String subjectDn, int days) throws Exception {
    return createIntermediateCa(issuerCertId, this.interKeyId, subjectDn, days);
  }

  public CertificateRecord createRootCa(long keyId, String subjectDn, int days) throws Exception {
    var keyRec = mustKey(keyId);
    var pair = loadKeyPair(keyRec);
    var subject = new X500Name(subjectDn);

    var now = Instant.now();
    var notBefore = java.util.Date.from(now.minus(1, ChronoUnit.MINUTES));
    var notAfter  = java.util.Date.from(now.plus(days, ChronoUnit.DAYS));
    var serial = new BigInteger(160, rng).abs();

    var builder = new JcaX509v3CertificateBuilder(
        subject, serial, notBefore, notAfter, subject, pair.getPublic());

    builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
    builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
    builder.addExtension(Extension.subjectKeyIdentifier, false, createSubjectKeyId(pair.getPublic()));
    builder.addExtension(Extension.authorityKeyIdentifier, false, createAuthorityKeyId(pair.getPublic()));

    var holder = builder.build(signer("SHA256withRSA", pair.getPrivate()));
    var cert = toX509(holder);

    return persistCert(keyRec, subject, subject, cert, true, null);
  }

  public CertificateRecord createIntermediateCa(long issuerCertId, long subjectKeyId, String subjectDn, int days) throws Exception {
    var issuer = mustCert(issuerCertId);
    var issuerKey = mustKey(issuer.getKeyPair().getId());
    var issuerPair = loadKeyPair(issuerKey);

    var subjectKey = mustKey(subjectKeyId);
    var subjectPair = loadKeyPair(subjectKey);
    var subject = new X500Name(subjectDn);

    var now = Instant.now();
    var notBefore = java.util.Date.from(now.minus(1, ChronoUnit.MINUTES));
    var notAfter  = java.util.Date.from(now.plus(days, ChronoUnit.DAYS));
    var serial = new BigInteger(160, rng).abs();

    var issuerX500 = new X500Name(issuer.getSubjectDn());
    var builder = new JcaX509v3CertificateBuilder(
        issuerX500, serial, notBefore, notAfter, subject, subjectPair.getPublic());

    builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
    builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
    builder.addExtension(Extension.subjectKeyIdentifier, false, createSubjectKeyId(subjectPair.getPublic()));
    builder.addExtension(Extension.authorityKeyIdentifier, false, createAuthorityKeyId(issuerPair.getPublic()));

    var holder = builder.build(signer("SHA256withRSA", issuerPair.getPrivate()));
    var cert = toX509(holder);

    return persistCert(subjectKey, subject, issuerX500, cert, true, issuer);
  }

  public CertificateRecord issueFromCsr(long csrId, int days) throws Exception {
    var csrRec = csrRepo.findById(csrId).orElseThrow(() -> new IllegalArgumentException("CSR not found: " + csrId));
    var csrHolder = readCsr(Paths.get(csrRec.getCsrPath()));
    var subject = csrHolder.getSubject();

    var pubConv = new JcaPEMKeyConverter().setProvider("BC");
    PublicKey publicKey = pubConv.getPublicKey(csrHolder.getSubjectPublicKeyInfo());

    PrivateKey interPriv = readPrivateKey(interKeyPath);
    X509Certificate interCert = readCert(interCertPath);

    var now = Instant.now();
    var notBefore = java.util.Date.from(now.minus(1, ChronoUnit.MINUTES));
    var notAfter  = java.util.Date.from(now.plus(days, ChronoUnit.DAYS));
    var serial = new BigInteger(160, rng).abs();

    var issuerX500 = new X500Name(interCert.getSubjectX500Principal().getName());
    var builder = new JcaX509v3CertificateBuilder(
        issuerX500, serial, notBefore, notAfter, subject, publicKey);

    builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
    int usageBits = KeyUsage.digitalSignature | KeyUsage.nonRepudiation;
    builder.addExtension(Extension.keyUsage, true, new KeyUsage(usageBits));
    builder.addExtension(Extension.extendedKeyUsage, false,
        new ExtendedKeyUsage(new KeyPurposeId[]{ KeyPurposeId.id_kp_codeSigning }));
    builder.addExtension(Extension.subjectKeyIdentifier, false, createSubjectKeyId(publicKey));
    builder.addExtension(Extension.authorityKeyIdentifier, false, createAuthorityKeyId(interCert.getPublicKey()));

    var holder = builder.build(signer("SHA256withRSA", interPriv));
    var cert = toX509(holder);

    return persistCert(csrRec.getKeyPair(), subject, issuerX500, cert, false, null);
  }

  private KeyPairRecord mustKey(long id) {
    return keyRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Key not found: " + id));
  }

  private CertificateRecord mustCert(long id) {
    return certRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Cert not found: " + id));
  }

  private KeyPair loadKeyPair(KeyPairRecord keyRec) throws Exception {
    var priv = readPrivateKey(resolvePath(keyRec.getPrivateKeyPath()));
    PublicKey pub = null;
    if (keyRec.getPublicKeyPath() != null && Files.exists(resolvePath(keyRec.getPublicKeyPath()))) {
      pub = readPublicKey(resolvePath(keyRec.getPublicKeyPath()));
    } else if (priv instanceof RSAPrivateCrtKey rsa) {
      var kf = KeyFactory.getInstance("RSA");
      pub = kf.generatePublic(new java.security.spec.RSAPublicKeySpec(rsa.getModulus(), rsa.getPublicExponent()));
    }
    if (pub == null) throw new IllegalStateException("Missing public key for keyId=" + keyRec.getId());
    return new KeyPair(pub, priv);
  }

  private Path resolvePath(String stored) {
    Path p = Paths.get(stored);
    Path abs = p.isAbsolute() ? p : baseDir.resolve(p);
    abs = abs.normalize();
    if (!abs.startsWith(baseDir)) throw new SecurityException("Path escapes base dir: " + stored);
    return abs;
  }

  private ContentSigner signer(String alg, PrivateKey priv) throws Exception {
    return new JcaContentSignerBuilder(alg).setProvider("BC").build(priv);
  }

  private SubjectKeyIdentifier createSubjectKeyId(PublicKey pub) throws Exception {
    var info = SubjectPublicKeyInfo.getInstance(pub.getEncoded());
    return new JcaX509ExtensionUtils().createSubjectKeyIdentifier(info);
  }

  private AuthorityKeyIdentifier createAuthorityKeyId(PublicKey pub) throws Exception {
    var info = SubjectPublicKeyInfo.getInstance(pub.getEncoded());
    return new JcaX509ExtensionUtils().createAuthorityKeyIdentifier(info);
  }

  private X509Certificate toX509(X509CertificateHolder holder) throws Exception {
    return new JcaX509CertificateConverter().setProvider("BC").getCertificate(holder);
  }

  private PKCS10CertificationRequest readCsr(Path path) throws Exception {
    try (var r = new BufferedReader(new FileReader(path.toFile(), StandardCharsets.UTF_8));
         var pem = new PEMParser(r)) {
      Object obj = pem.readObject();
      if (obj instanceof PKCS10CertificationRequest req) return req;
      throw new IllegalArgumentException("Not a PKCS#10 CSR: " + path);
    }
  }

  private X509Certificate readCert(Path path) throws Exception {
    try (var in = Files.newInputStream(path)) {
      var cf = CertificateFactory.getInstance("X.509");
      return (X509Certificate) cf.generateCertificate(in);
    }
  }

  private PrivateKey readPrivateKey(Path pkPath) throws Exception {
    try (var reader = new BufferedReader(new FileReader(pkPath.toFile(), StandardCharsets.UTF_8));
         var pemParser = new PEMParser(reader)) {
      Object obj = pemParser.readObject();
      var converter = new JcaPEMKeyConverter().setProvider("BC");
      if (obj instanceof org.bouncycastle.openssl.PEMKeyPair kp) {
        return converter.getKeyPair(kp).getPrivate();
      } else if (obj instanceof org.bouncycastle.asn1.pkcs.PrivateKeyInfo pki) {
        return converter.getPrivateKey(pki);
      } else {
        String all = Files.readString(pkPath);
        String b64 = all.replaceAll("-----.*?-----", "").replaceAll("\\s+", "");
        byte[] der = Base64.getDecoder().decode(b64);
        var spec = new PKCS8EncodedKeySpec(der);
        try {
          return KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (Exception e) {
          return KeyFactory.getInstance("EC").generatePrivate(spec);
        }
      }
    }
  }

  private PublicKey readPublicKey(Path pubPath) throws Exception {
    try (var reader = new BufferedReader(new FileReader(pubPath.toFile(), StandardCharsets.UTF_8));
         var pemParser = new PEMParser(reader)) {
      Object obj = pemParser.readObject();
      var converter = new JcaPEMKeyConverter().setProvider("BC");
      if (obj instanceof org.bouncycastle.asn1.x509.SubjectPublicKeyInfo spi) {
        return converter.getPublicKey(spi);
      } else {
        String all = Files.readString(pubPath);
        String b64 = all.replaceAll("-----.*?-----", "").replaceAll("\\s+", "");
        byte[] der = Base64.getDecoder().decode(b64);
        try {
          return KeyFactory.getInstance("RSA")
              .generatePublic(new java.security.spec.X509EncodedKeySpec(der));
        } catch (Exception e) {
          return KeyFactory.getInstance("EC")
              .generatePublic(new java.security.spec.X509EncodedKeySpec(der));
        }
      }
    }
  }

  private CertificateRecord persistCert(
      KeyPairRecord key, X500Name subject, X500Name issuer,
      X509Certificate cert, boolean isCa, CertificateRecord issuerRec) throws Exception {

    Path certDir = baseDir.resolve(Paths.get("certs"));
    Files.createDirectories(certDir);
    String filename = "cert-" + cert.getSerialNumber().toString(16) + ".pem";
    Path certPath = certDir.resolve(filename);
    String pem = toPem("CERTIFICATE", cert.getEncoded());
    Files.writeString(certPath, pem, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);

    var rec = CertificateRecord.builder()
        .keyPair(key)
        .serialNumber(cert.getSerialNumber().toString(16))
        .subjectDn(subject.toString())
        .issuerDn(issuer.toString())
        .notBefore(cert.getNotBefore().toInstant())
        .notAfter(cert.getNotAfter().toInstant())
        .certificatePath(certPath.toString())
        .sha256Fingerprint(sha256Hex(cert.getEncoded()))
        .ca(isCa)
        .build();

    return certRepo.save(rec);
  }

  private String toPem(String type, byte[] der) {
    String b64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(der);
    return "-----BEGIN " + type + "-----\n" + b64 + "\n-----END " + type + "-----\n";
  }

  private String sha256Hex(byte[] data) throws Exception {
    var md = MessageDigest.getInstance("SHA-256");
    var d = md.digest(data);
    var sb = new StringBuilder(d.length * 2);
    for (byte b : d) sb.append(String.format("%02x", b));
    return sb.toString();
  }
}
