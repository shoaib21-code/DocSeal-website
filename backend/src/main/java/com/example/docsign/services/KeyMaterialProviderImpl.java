// src/main/java/com/example/docsign/crypto/KeyMaterialProviderImpl.java
package com.example.docsign.services;

import com.example.docsign.model.CertificateRecord;
import com.example.docsign.model.CertificateRecordRepository;
import com.example.docsign.model.KeyPairRecord;
import com.example.docsign.model.KeyPairRecordRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.file.*;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;

@Component
public class KeyMaterialProviderImpl implements KeyMaterialProvider {

    private final KeyPairRecordRepository keyRepo;
    private final CertificateRecordRepository certRepo;
    private final Path baseDir;
    private final Path interCertPath; // configured intermediate certificate

    public KeyMaterialProviderImpl(
            KeyPairRecordRepository keyRepo,
            CertificateRecordRepository certRepo,
            @Value("${app.storage.base-path}") String basePath,
            @Value("${app.ca.intermediate.cert-path}") String interCertPathStr) {
        this.keyRepo = keyRepo;
        this.certRepo = certRepo;
        this.baseDir = Paths.get(basePath).toAbsolutePath().normalize();
        this.interCertPath = resolve(interCertPathStr);
    }

    @Override
    public KeyMaterial byKeyRef(String keyRef) throws Exception {
        long id = Long.parseLong(keyRef);
        KeyPairRecord k = keyRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Key not found: " + id));

        PrivateKey priv = readPrivateKey(resolve(k.getPrivateKeyPath()));

        // pick the newest LEAF cert issued for this key
        X509Certificate leaf = findLatestLeafForKey(id)
                .orElseThrow(() -> new IllegalStateException("No leaf certificate for keyId=" + id));

        List<X509Certificate> chain = new ArrayList<>();
        chain.add(leaf);
        if (Files.exists(interCertPath))
            chain.addAll(readAllPemCerts(interCertPath));
        return new KeyMaterial(priv, leaf, chain);
    }

    @Override
    public KeyMaterial byCertRef(String certRef) throws Exception {
        long cid = Long.parseLong(certRef);
        CertificateRecord c = certRepo.findById(cid)
                .orElseThrow(() -> new IllegalArgumentException("Cert not found: " + cid));
        if (c.isCa())
            throw new IllegalArgumentException("CA certificate cannot be used as signer");

        Long keyId = c.getKeyId();
        if (keyId == null)
            throw new IllegalStateException("Signer certificate has no key reference");
        KeyPairRecord k = keyRepo.findById(keyId)
                .orElseThrow(() -> new IllegalStateException("Key for cert not found: " + keyId));
        PrivateKey priv = readPrivateKey(resolve(k.getPrivateKeyPath()));
        X509Certificate leaf = readCert(resolve(c.getCertificatePath()));

        List<X509Certificate> chain = new ArrayList<>();
        chain.add(leaf);
        if (Files.exists(interCertPath))
            chain.addAll(readAllPemCerts(interCertPath));
        return new KeyMaterial(priv, leaf, chain);
    }

    // ---------- helpers ----------
    private Path resolve(String stored) {
        Path p = Paths.get(stored);
        Path abs = p.isAbsolute() ? p : baseDir.resolve(p);
        return abs.normalize();
    }

    private Optional<X509Certificate> findLatestLeafForKey(long keyId) throws Exception {
        // brute-force from existing repo get all leafs and pick newest for keyId
        var list = certRepo.findByCaFalseOrderByIdDesc();
        for (var rec : list) {
            if (rec.getKeyId() != null && rec.getKeyId() == keyId) {
                return Optional.of(readCert(resolve(rec.getCertificatePath())));
            }
        }
        return Optional.empty();
    }

    private static PrivateKey readPrivateKey(Path pkPath) throws Exception {
        String pem = Files.readString(pkPath);
        String b64 = pem.replaceAll("-----\\w+ PRIVATE KEY-----", "").replaceAll("\\s+", "");
        byte[] der = Base64.getDecoder().decode(b64);
        try {
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception e) {
            return KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(der));
        }
    }

    private static X509Certificate readCert(Path path) throws Exception {
        var cf = CertificateFactory.getInstance("X.509");
        try (var in = Files.newInputStream(path)) {
            return (X509Certificate) cf.generateCertificate(in);
        }
    }

    private static List<X509Certificate> readAllPemCerts(Path path) throws Exception {
        String all = Files.readString(path);
        var blocks = all.split("-----END CERTIFICATE-----");
        var cf = CertificateFactory.getInstance("X.509");
        List<X509Certificate> list = new ArrayList<>();
        for (String b : blocks) {
            if (!b.contains("-----BEGIN CERTIFICATE-----"))
                continue;
            String chunk = b.substring(b.indexOf("-----BEGIN CERTIFICATE-----"))
                    + "-----END CERTIFICATE-----\n";
            try (var in = new ByteArrayInputStream(chunk.getBytes())) {
                list.add((X509Certificate) cf.generateCertificate(in));
            }
        }
        return list;
    }
}
