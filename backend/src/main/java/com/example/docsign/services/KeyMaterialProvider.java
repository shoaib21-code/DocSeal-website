// src/main/java/com/example/docsign/crypto/KeyMaterialProvider.java
package com.example.docsign.services;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;

public interface KeyMaterialProvider {
  record KeyMaterial(PrivateKey privateKey, X509Certificate leaf, List<X509Certificate> chain) {}
  KeyMaterial byKeyRef(String keyRef) throws Exception;   // keyRef = key id
  KeyMaterial byCertRef(String certRef) throws Exception; // certRef = cert id
}
