package com.example.docsign.util;

import java.security.MessageDigest;

public final class Digests {
  private Digests() {}

  public static byte[] digest(String jcaName, byte[] data) throws Exception {
    MessageDigest md = MessageDigest.getInstance(jcaName);
    return md.digest(data);
  }

  public static String hex(byte[] b) {
    StringBuilder sb = new StringBuilder(b.length * 2);
    for (byte x : b) sb.append(String.format("%02x", x));
    return sb.toString();
  }
}
