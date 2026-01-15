// src/main/java/com/example/docsign/crypto/CmsSigner.java
package com.example.docsign.services;

import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.asn1.cms.Time;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.cert.jcajce.JcaCertStore;   

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.*;

public class CmsSigner {

  public static byte[] createCms(
      byte[] content, boolean encapsulate,
      PrivateKey privateKey, X509Certificate leaf, List<X509Certificate> chain,
      String hashAlgo, String sigAlgo
  ) throws Exception {

    String hash = (hashAlgo == null) ? "SHA-256" : hashAlgo;
    String sig  = (sigAlgo  == null) ? "RSA-PSS" : sigAlgo;
    String jcaSig = jcaSigName(sig, hash);

    CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
    ContentSigner cs = new JcaContentSignerBuilder(jcaSig).build(privateKey);
    DigestCalculatorProvider digProv = new JcaDigestCalculatorProviderBuilder().build();

    // Add signingTime + default set
    var signedAttrs = new org.bouncycastle.asn1.cms.AttributeTable(
        new org.bouncycastle.asn1.ASN1EncodableVector() {{
          add(new org.bouncycastle.asn1.cms.Attribute(
              CMSAttributes.signingTime, new org.bouncycastle.asn1.DERSet(new Time(new java.util.Date()))
          ));
        }}
    );

    var sigInfoBuilder = new JcaSignerInfoGeneratorBuilder(digProv)
        .setSignedAttributeGenerator(new DefaultSignedAttributeTableGenerator(signedAttrs));

    gen.addSignerInfoGenerator(sigInfoBuilder.build(cs, new JcaX509CertificateHolder(leaf)));

    var holders = new ArrayList<X509CertificateHolder>();
    for (var c : chain) holders.add(new JcaX509CertificateHolder(c));
    gen.addCertificates(new JcaCertStore(holders));

    CMSTypedData msg = new CMSProcessableByteArray(content);
    return gen.generate(msg, encapsulate).getEncoded();
  }

  private static String jcaSigName(String sigAlgo, String hash) {
    String h = hash.replace("-", "");
    if ("RSA-PSS".equalsIgnoreCase(sigAlgo)) return h + "withRSAandMGF1";
    if ("RSA".equalsIgnoreCase(sigAlgo) || "RSA1_5".equalsIgnoreCase(sigAlgo)) return h + "withRSA";
    throw new IllegalArgumentException("Unsupported sigAlgo: " + sigAlgo);
  }
}
