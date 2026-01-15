// src/main/java/com/example/docsign/crypto/PdfSignService.java
package com.example.docsign.services;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.ExternalSigningSupport;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.example.docsign.util.Digests.*;

@Service
public class PdfSignService {

  private final KeyMaterialProvider keyMat;
  private final String defHash;
  private final String defSig;
  private final long maxPdfBytes;

  private static final Logger log = LoggerFactory.getLogger(PdfSignService.class);

  public PdfSignService(
      KeyMaterialProvider keyMat,
      @Value("${docseal.sign.default-hash:SHA-256}") String defHash,
      @Value("${docseal.sign.default-sig:RSA-PSS}") String defSig,
      @Value("${docseal.sign.max-pdf-mb:10}") long maxPdfMb) {
    this.keyMat = keyMat;
    this.defHash = defHash;
    this.defSig = defSig;
    this.maxPdfBytes = maxPdfMb * 1024L * 1024L;
  }

  public byte[] signInvisible(byte[] pdf, String keyRef, String certRef, String hashAlgo, String sigAlgo)
      throws Exception {
    if (pdf.length > maxPdfBytes)
      throw new IllegalArgumentException("PDF too large");
    var km = (keyRef != null && !keyRef.isBlank()) ? keyMat.byKeyRef(keyRef) : keyMat.byCertRef(certRef);
    String jcaHash = (hashAlgo != null) ? hashAlgo : defHash;
    try (var doc = Loader.loadPDF(pdf);
        var baos = new java.io.ByteArrayOutputStream();
        var opts = new SignatureOptions()) {

      if (!doc.getSignatureDictionaries().isEmpty())
        throw new IllegalStateException("PDF already signed (Multiple signatures forbidded).");

      // reserve space for CMS (Crytographic message syntax)
      opts.setPreferredSignatureSize(64 * 1024);

      PDSignature sig = new PDSignature();
      sig.setType(COSName.SIG);
      sig.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
      sig.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
      sig.setSignDate(java.util.Calendar.getInstance());

      // attach signature dictionary to PDF (but still empty content)
      doc.addSignature(sig, opts);

      ExternalSigningSupport ext = doc.saveIncrementalForExternalSigning(baos);

      byte[] toBeSigned;
      try (var content = ext.getContent()) {
        toBeSigned = IOUtils.toByteArray(content);
      }

      // Our own digest of the data
      byte[] calcDigest = digest(jcaHash, toBeSigned);
      log.info("PDF-SIGN ByteRange digest ({}): {}", jcaHash, hex(calcDigest));

      // Build CMS SignedData
      byte[] cms = CmsSigner.createCms(
          toBeSigned,
          /* encapsulate */ false,
          km.privateKey(), km.leaf(), km.chain(),
          jcaHash, (sigAlgo != null) ? sigAlgo : defSig);

      // Debug: inspect CMS and log its messageDigest attribute
      {
        CMSSignedData cmsObj = new CMSSignedData(new CMSProcessableByteArray(toBeSigned), cms);
        var si = cmsObj.getSignerInfos().getSigners().iterator().next();
        Attribute mdAttr = si.getSignedAttributes().get(CMSAttributes.messageDigest);
        ASN1Encodable val = mdAttr.getAttrValues().getObjectAt(0);
        byte[] msgDigest = ((DEROctetString) val.toASN1Primitive()).getOctets();
        log.info("CMS attribute message-digest: {}", hex(msgDigest));
      }
      // hand the CMS back to PDFBox so it writes into /Contents
      ext.setSignature(cms);

      return baos.toByteArray();
    }
  }

public byte[] signVisibleText(
    byte[] pdf,
    String keyRef, String certRef,
    String hashAlgo, String sigAlgo,
    int pageIndex,
    float xFromTopLeft, float yFromTopLeft,
    float width, float height,
    String displayName 
) throws Exception {

  if (pdf.length > maxPdfBytes) throw new IllegalArgumentException("PDF too large");

  var km = (keyRef != null && !keyRef.isBlank()) ? keyMat.byKeyRef(keyRef) : keyMat.byCertRef(certRef);

  String jcaHash = (hashAlgo == null || hashAlgo.isBlank()) ? defHash : hashAlgo;
  String jcaSig  = (sigAlgo  == null || sigAlgo.isBlank())  ? defSig  : sigAlgo;

  try (var doc = Loader.loadPDF(pdf);
       var baos = new java.io.ByteArrayOutputStream();
       var opts = new SignatureOptions()) {

    if (!doc.getSignatureDictionaries().isEmpty())
      throw new IllegalStateException("PDF already signed (Multiple signatures forbidden).");

    // Reserve space for CMS
    opts.setPreferredSignatureSize(150 * 1024);

    PDSignature sig = new PDSignature();
    sig.setType(COSName.SIG);
    sig.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
    sig.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
    sig.setSignDate(java.util.Calendar.getInstance());

    // These show up in signature properties
    String name = (displayName == null || displayName.isBlank()) ? "DocSeal Signer" : displayName;
    sig.setName(name);
    sig.setReason("Digitally signed");

    // --- Visible signature mark (text-only) ---
    var humanRect = new java.awt.geom.Rectangle2D.Float(xFromTopLeft, yFromTopLeft, width, height);
    var rect = createSignatureRectangle(doc, pageIndex, humanRect);

    opts.setVisualSignature(
        createVisualSignatureTemplateTextOnly(doc, pageIndex, rect, sig)
    );
    opts.setPage(pageIndex);

    // Attach signature dictionary + appearance
    doc.addSignature(sig, opts);

    ExternalSigningSupport ext = doc.saveIncrementalForExternalSigning(baos);

    byte[] toBeSigned;
    try (var content = ext.getContent()) {
      toBeSigned = IOUtils.toByteArray(content);
    }

    // CMS
    byte[] cms = CmsSigner.createCms(
        toBeSigned,
        false,
        km.privateKey(), km.leaf(), km.chain(),
        jcaHash, jcaSig
    );

    ext.setSignature(cms);
    return baos.toByteArray();
  }
}

public byte[] signVisibleText(
    byte[] pdf,
    String keyRef, String certRef,
    String hashAlgo, String sigAlgo
) throws Exception {

  // Resolve cert once to get CN
  var km = (keyRef != null && !keyRef.isBlank())
      ? keyMat.byKeyRef(keyRef)
      : keyMat.byCertRef(certRef);

  String displayName = certCommonName(km.leaf());
  if (displayName == null || displayName.isBlank()) displayName = "DocSeal";

  int pageIndex = 0;

  try (var doc = Loader.loadPDF(pdf)) {
  var page = doc.getPage(0);
  float pageW = page.getCropBox().getWidth();
  float pageH = page.getCropBox().getHeight();

  float width  = 240f;
  float height = 65f;
  float margin = 24f;

  float xFromTopLeft = pageW - width  - margin;
  float yFromTopLeft = pageH - height - margin;

  return signVisibleText(pdf, keyRef, certRef, hashAlgo, sigAlgo,
      pageIndex, xFromTopLeft, yFromTopLeft, width, height, displayName);
}

}



private static String certCommonName(java.security.cert.X509Certificate cert) {
  try {
    var holder = new org.bouncycastle.cert.jcajce.JcaX509CertificateHolder(cert);
    var x500 = holder.getSubject();
    var rdns = x500.getRDNs(org.bouncycastle.asn1.x500.style.BCStyle.CN);
    if (rdns != null && rdns.length > 0) {
      var val = rdns[0].getFirst().getValue();
      return org.bouncycastle.asn1.x500.style.IETFUtils.valueToString(val);
    }
    return cert.getSubjectX500Principal().getName(); // fallback
  } catch (Exception e) {
    return cert.getSubjectX500Principal().getName(); // fallback
  }
}

private static org.apache.pdfbox.pdmodel.common.PDRectangle createSignatureRectangle(
    org.apache.pdfbox.pdmodel.PDDocument doc,
    int pageNum,
    java.awt.geom.Rectangle2D humanRect
) {
  float x = (float) humanRect.getX();
  float y = (float) humanRect.getY();
  float w = (float) humanRect.getWidth();
  float h = (float) humanRect.getHeight();

  var page = doc.getPage(pageNum);
  var pageRect = page.getCropBox();
  var rect = new org.apache.pdfbox.pdmodel.common.PDRectangle();

  switch (page.getRotation()) {
    case 90:
      rect.setLowerLeftY(x);
      rect.setUpperRightY(x + w);
      rect.setLowerLeftX(y);
      rect.setUpperRightX(y + h);
      break;
    case 180:
      rect.setUpperRightX(pageRect.getWidth() - x);
      rect.setLowerLeftX(pageRect.getWidth() - x - w);
      rect.setLowerLeftY(y);
      rect.setUpperRightY(y + h);
      break;
    case 270:
      rect.setLowerLeftY(pageRect.getHeight() - x - w);
      rect.setUpperRightY(pageRect.getHeight() - x);
      rect.setLowerLeftX(pageRect.getWidth() - y - h);
      rect.setUpperRightX(pageRect.getWidth() - y);
      break;
    case 0:
    default:
      rect.setLowerLeftX(x);
      rect.setUpperRightX(x + w);
      rect.setLowerLeftY(pageRect.getHeight() - y - h);
      rect.setUpperRightY(pageRect.getHeight() - y);
      break;
  }
  return rect;
}


private static java.io.InputStream createVisualSignatureTemplateTextOnly(
    org.apache.pdfbox.pdmodel.PDDocument srcDoc,
    int pageIndex,
    org.apache.pdfbox.pdmodel.common.PDRectangle rect,
    org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature signature
) throws java.io.IOException {

  try (var doc = new org.apache.pdfbox.pdmodel.PDDocument()) {

    var srcPage = srcDoc.getPage(pageIndex);
    var page = new org.apache.pdfbox.pdmodel.PDPage(srcPage.getMediaBox());
    doc.addPage(page);

    var acroForm = new org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm(doc);
    doc.getDocumentCatalog().setAcroForm(acroForm);

    // signature field + widget
    var sigField = new org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField(acroForm);
    sigField.setPartialName("Signature1");
    acroForm.getFields().add(sigField);

    var widget = sigField.getWidgets().get(0);
    widget.setRectangle(rect);
    widget.setPage(page);
    page.getAnnotations().add(widget);

    // appearance stream
    var appearanceStream = new org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream(doc);
    appearanceStream.setResources(new org.apache.pdfbox.pdmodel.PDResources());
    appearanceStream.setBBox(new org.apache.pdfbox.pdmodel.common.PDRectangle(rect.getWidth(), rect.getHeight()));

    var appearance = new org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary();
    appearance.setNormalAppearance(appearanceStream);
    widget.setAppearance(appearance);

    float W = rect.getWidth();
    float H = rect.getHeight();
    float pad = 4f;


    float leftW = W * 0.45f;     
    float rightX = leftW + pad;   
    float rightW = W - rightX - pad;

    var bold = new org.apache.pdfbox.pdmodel.font.PDType1Font(
        org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA_BOLD
    );
    var reg = new org.apache.pdfbox.pdmodel.font.PDType1Font(
        org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA
    );

    String cn = safe(signature.getName());
    if (cn.isBlank()) cn = "DocSeal Signer";

    // Split CN into 2 lines like the screenshot ("Casey" / "Crane")
    String lineA = cn;
    String lineB = "";
    {
      String[] parts = cn.split("\\s+");
      if (parts.length >= 2) {
        lineA = parts[0];
        lineB = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
      }
    }

    // Date formatting (nice + stable)
    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy.MM.dd HH:mm");
    sdf.setTimeZone(signature.getSignDate().getTimeZone());
    String dateStr = sdf.format(signature.getSignDate().getTime());

    try (var cs = new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, appearanceStream)) {

      // ---- Left: big CN (two lines) ----
      float maxNameW = leftW - 2 * pad;
      float fs1 = Math.min(34f, H * 0.55f);  // starting font size
      float fs2 = Math.min(34f, H * 0.55f);

      // shrink to fit each line
      fs1 = fitFontSize(bold, lineA, fs1, maxNameW);
      if (!lineB.isBlank()) fs2 = fitFontSize(bold, lineB, fs2, maxNameW);

      float yTop = H - pad - fs1;
      drawText(cs, bold, fs1, pad, yTop, trimToFit(lineA, bold, fs1, maxNameW));

      if (!lineB.isBlank()) {
        float gap = 4f;
        float y2 = yTop - fs2 - gap;
        drawText(cs, bold, fs2, pad, y2, trimToFit(lineB, bold, fs2, maxNameW));
      }

      // a “signature-like swoosh” 
      cs.setStrokingColor(0.75f);   // light gray
      cs.setLineWidth(1.2f);
      float sx = leftW - 20f;
      float sy = H * 0.25f;
      cs.moveTo(sx, sy);
      cs.curveTo(sx + 15, sy + 35, sx + 40, sy + 40, sx + 70, sy + 15);
      cs.curveTo(sx + 95, sy - 5, sx + 115, sy + 10, sx + 130, sy + 35);
      cs.stroke();

      // ---- Right: “Digitally signed by … Date … Signed using DocSeal” ----
      float ry = H - pad - 12f;

      drawText(cs, reg, 10f, rightX, ry, "Digitally signed");
      ry -= 13f;

      drawText(cs, reg, 10f, rightX, ry,
          trimToFit("by " + cn, reg, 10f, rightW));
      ry -= 13f;

      drawText(cs, reg, 10f, rightX, ry,
          trimToFit("Date: " + dateStr, reg, 10f, rightW));
      ry -= 13f;

      // app branding line
      drawText(cs, reg, 9f, rightX, ry,
          trimToFit("Signed using DocSeal", reg, 9f, rightW));
    }

    var baos = new java.io.ByteArrayOutputStream();
    doc.save(baos);
    return new java.io.ByteArrayInputStream(baos.toByteArray());
  }
}

private static void drawText(org.apache.pdfbox.pdmodel.PDPageContentStream cs,
                             org.apache.pdfbox.pdmodel.font.PDFont font,
                             float fontSize,
                             float x, float y,
                             String text) throws java.io.IOException {
  cs.beginText();
  cs.setFont(font, fontSize);
  cs.setNonStrokingColor(0f);
  cs.newLineAtOffset(x, y);
  cs.showText(text == null ? "" : text);
  cs.endText();
}

private static float fitFontSize(org.apache.pdfbox.pdmodel.font.PDFont font,
                                 String text,
                                 float startSize,
                                 float maxWidth) throws java.io.IOException {
  float size = startSize;
  String t = (text == null) ? "" : text;
  while (size > 8f) {
    float w = font.getStringWidth(t) / 1000f * size;
    if (w <= maxWidth) return size;
    size -= 1f;
  }
  return 8f;
}

private static String safe(String s) {
  return (s == null) ? "" : s.replaceAll("[\\r\\n\\t]+", " ").trim();
}

private static String trimToFit(String text,
                                org.apache.pdfbox.pdmodel.font.PDFont font,
                                float fontSize,
                                float maxWidth) throws java.io.IOException {
  if (text == null) return "";
  String t = text;
  while (!t.isEmpty()) {
    float w = font.getStringWidth(t) / 1000f * fontSize;
    if (w <= maxWidth) return t;
    t = t.substring(0, t.length() - 1);
  }
  return "";
}

}
