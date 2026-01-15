package com.example.docsign.services;

import com.example.docsign.model.KeyPairRecord;
import com.example.docsign.model.KeyPairRecordRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

@Service
public class KeyPairService {

  private final KeyPairRecordRepository repo;
  private final Path baseDir;

  public KeyPairService(KeyPairRecordRepository repo,
                        @Value("${app.storage.base-path}") String basePath) throws IOException {
    this.repo = repo;
    this.baseDir = Paths.get(basePath).toAbsolutePath().normalize();
    Files.createDirectories(this.baseDir);
  }

  /** Generate an RSA key pair and persist it; returns the saved record */
  public KeyPairRecord generateRsaAndStore(int bits) throws Exception {
    if (bits < 2048) throw new IllegalArgumentException("Key size must be >= 2048");

    // 1) Generate RSA keypair (standard JCA)
    KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
    kpg.initialize(bits);
    KeyPair kp = kpg.generateKeyPair();

    // 2) Save a DB row first to get an id for the folder name
    KeyPairRecord rec = KeyPairRecord.builder()
        .algorithm("RSA")
        .keySize(bits)
        .createdAt(Instant.now())
        .build();
    rec = repo.save(rec);

    // 3) Write PEM files to ./data/keys/{id}/
    Path keyDir = baseDir.resolve(Paths.get("keys", String.valueOf(rec.getId())));
    Files.createDirectories(keyDir);

    String pubPem  = toPem("PUBLIC KEY",  kp.getPublic().getEncoded());
    String privPem = toPem("PRIVATE KEY", kp.getPrivate().getEncoded());

    Path pubPath  = keyDir.resolve("public.pem");
    Path privPath = keyDir.resolve("private.pem");

    Files.writeString(pubPath, pubPem, StandardCharsets.UTF_8,
        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);


    try { Files.writeString(privPath, privPem, StandardCharsets.UTF_8,
        StandardOpenOption.CREATE_NEW); }
    catch (FileAlreadyExistsException e) {
      Files.writeString(privPath, privPem, StandardCharsets.UTF_8,
          StandardOpenOption.TRUNCATE_EXISTING);
    }

    // 4) Compute a simple SHA-256 fingerprint of the public key DER
    String fp = sha256Hex(kp.getPublic().getEncoded());

    rec.setPublicKeyPath(pubPath.toString());
    rec.setPrivateKeyPath(privPath.toString());
    rec.setPublicKeyFingerprint(fp);

    return repo.save(rec);
  }

  public KeyPairRecord get(long id) {
    return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Key not found: " + id));
  }

  private static String toPem(String type, byte[] der) {
    String b64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(der);
    return "-----BEGIN " + type + "-----\n" + b64 + "\n-----END " + type + "-----\n";
  }

  private static String sha256Hex(byte[] data) throws Exception {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    byte[] d = md.digest(data);
    StringBuilder sb = new StringBuilder(d.length * 2);
    for (byte b : d) sb.append(String.format("%02x", b));
    return sb.toString();
  }
}

