// src/main/java/com/example/docsign/crypto/VerificationService.java
package com.example.docsign.services;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.asn1.cms.Time;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.util.Store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.stream.Collectors;

import static com.example.docsign.util.Digests.digest;
import static com.example.docsign.util.Digests.hex;

@Service
public class VerificationService {
  private static final Logger log = LoggerFactory.getLogger(VerificationService.class);

  // ===== Frontend DTOs =====
  public record PdfVerifyResult(
      boolean valid,
      boolean byteRangeOK,
      int signerCount,
      List<Signer> signers,
      List<String> errors
  ) {
    public record Signer(
        String subject,
        String issuer,
        String serialHex,
        String notBefore,
        String notAfter,
        String signingTime,          // ISO string or null
        Alg alg,
        List<String> chainSubjects   // optional, may be empty
    ) {}
    public record Alg(String hash, String sig) {}
  }

  // ===== PDF verification (PDFBox 3.0.3) =====
  public PdfVerifyResult verifyPdf(byte[] pdf) {
    var errors = new ArrayList<String>();
    try (var doc = Loader.loadPDF(pdf)) {
      var sigs = doc.getSignatureDictionaries();
      if (sigs.isEmpty()) {
        errors.add("No signature found in PDF.");
        return new PdfVerifyResult(false, false, 0, List.of(), errors);
      }

      PDSignature sig = sigs.get(0);
      boolean byteRangeOK = sig.getByteRange() != null && sig.getByteRange().length == 4;

      // Extract CMS (/Contents) as stored (handles hex padding)
      byte[] cmsBytes;
      try (var in = new ByteArrayInputStream(pdf)) {
        cmsBytes = sig.getContents(in);
      }
      if (cmsBytes == null || cmsBytes.length == 0) {
        errors.add("Empty CMS /Contents.");
        return new PdfVerifyResult(false, byteRangeOK, 0, List.of(), errors);
      }

      // Extract exact signed bytes (concatenation of /ByteRange)
      byte[] signedContent;
      try (var in = new ByteArrayInputStream(pdf)) {
        signedContent = sig.getSignedContent(in);
      }

      // Build CMSSignedData with DETACHED content
      CMSSignedData cms = new CMSSignedData(new CMSProcessableByteArray(signedContent), cmsBytes);

      // DEBUG: compare computed digest vs CMS message-digest (first signer)
      var firstSi = cms.getSignerInfos().getSigners().iterator().next();
      String digestOid = firstSi.getDigestAlgOID();
      String jcaName = oidToJca(digestOid);
      byte[] calcDigest = digest(jcaName, signedContent);
      log.info("PDF-VERIFY ByteRange digest ({} / {}): {}", digestOid, jcaName, hex(calcDigest));
      var mdAttr = firstSi.getSignedAttributes() != null
          ? firstSi.getSignedAttributes().get(CMSAttributes.messageDigest) : null;
      if (mdAttr != null) {
        byte[] msgDigest = ((DEROctetString) mdAttr.getAttrValues().getObjectAt(0).toASN1Primitive()).getOctets();
        log.info("PDF-VERIFY CMS attribute message-digest: {}", hex(msgDigest));
        log.info("PDF-VERIFY Digest match? {}", Arrays.equals(calcDigest, msgDigest));
        if (!Arrays.equals(calcDigest, msgDigest)) errors.add("ByteRange digest mismatch with CMS message-digest.");
      } else {
        errors.add("Missing CMS message-digest attribute.");
      }

      // Crypto verification of each signer
      boolean allValid = verifyAllSigners(cms, errors);

      // Build signer list for UI
      List<PdfVerifyResult.Signer> signers = buildSignerDtos(cms);

      return new PdfVerifyResult(
          allValid && errors.isEmpty(),
          byteRangeOK,
          signers.size(),
          signers,
          errors.isEmpty() ? null : errors
      );
    } catch (Exception e) {
      errors.add("Verify failed: " + e.getMessage());
      log.warn("PDF verify error", e);
      return new PdfVerifyResult(false, false, 0, List.of(), errors);
    }
  }

  // ===== Helpers =====

  private boolean verifyAllSigners(CMSSignedData cms, List<String> errors) throws Exception {
    var signers = cms.getSignerInfos();
    var certs   = cms.getCertificates();
    var verifierBuilder = new JcaSimpleSignerInfoVerifierBuilder().setProvider("BC");

    for (SignerInformation s : signers.getSigners()) {
      var holder = findSignerCert(certs, s);
      var cert   = toX509(holder);
      boolean ok = s.verify(verifierBuilder.build(cert));
      if (!ok) errors.add("Signature verification failed for signer: " + cert.getSubjectX500Principal().getName());
      if (!ok) return false;
    }
    return true;
  }

  private List<PdfVerifyResult.Signer> buildSignerDtos(CMSSignedData cms) throws Exception {
    var out = new ArrayList<PdfVerifyResult.Signer>();
    Store<X509CertificateHolder> certs = cms.getCertificates();
    SignerInformationStore sis = cms.getSignerInfos();

    // optional: a simple list of all subjects present in CMS (not a built chain)
    List<String> allSubjects = certs.getMatches(null).stream()
        .map(h -> h.getSubject().toString())
        .collect(Collectors.toList());

    for (SignerInformation si : sis.getSigners()) {
      var holder = findSignerCert(certs, si);
      var cert   = toX509(holder);

      // per-signer signingTime if present
      String signingTime = null;
      AttributeTable attrs = si.getSignedAttributes();
      if (attrs != null) {
        Attribute st = attrs.get(CMSAttributes.signingTime);
        if (st != null) {
          ASN1Encodable val = st.getAttrValues().getObjectAt(0);
          Time t = Time.getInstance(val);
          signingTime = t.getDate().toInstant().toString();
        }
      }

      // algorithms
      var alg = new PdfVerifyResult.Alg(
          oidToJca(si.getDigestAlgOID()),
          oidToSigName(si.getEncryptionAlgOID())
      );

      out.add(new PdfVerifyResult.Signer(
          cert.getSubjectX500Principal().getName(),
          cert.getIssuerX500Principal().getName(),
          cert.getSerialNumber().toString(16),
          cert.getNotBefore().toInstant().toString(),
          cert.getNotAfter().toInstant().toString(),
          signingTime,
          alg,
          allSubjects   // simple list
      ));
    }
    return out;
  }

  private static X509CertificateHolder findSignerCert(
      Store<X509CertificateHolder> certs,
      SignerInformation s
  ) {
    for (X509CertificateHolder h : certs.getMatches(null)) {
      if (s.getSID().match(h)) return h;
    }
    throw new IllegalStateException("Signer certificate not found for signer ID");
  }

  private static X509Certificate toX509(X509CertificateHolder h) throws Exception {
    var cf = CertificateFactory.getInstance("X.509");
    return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(h.getEncoded()));
  }

  // Digest OID → JCA name
  private static String oidToJca(String oid) {
    return switch (oid) {
      case "2.16.840.1.101.3.4.2.1" -> "SHA-256";
      case "2.16.840.1.101.3.4.2.2" -> "SHA-384";
      case "2.16.840.1.101.3.4.2.3" -> "SHA-512";
      case "1.3.14.3.2.26"          -> "SHA-1";
      default -> oid; // fallback to OID string
    };
  }

  // Signature (encryption) OID → friendly name
  private static String oidToSigName(String oid) {
    return switch (oid) {
      case "1.2.840.113549.1.1.1"   -> "RSA";       // rsaEncryption (PKCS#1 v1.5)
      case "1.2.840.113549.1.1.10"  -> "RSASSA-PSS";
      default -> oid; // fallback to OID string
    };
  }
}
