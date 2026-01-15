// src/main/java/com/example/docsign/config/CryptoConfig.java
package com.example.docsign.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;

@Configuration
public class CryptoConfig {

  @Bean
  public SecureRandom secureRandom() {
    // Good default; blocks only while seeding on first use
    return new SecureRandom();

    // If you specifically want a strong instance (may block on some OSes):
    // return SecureRandom.getInstanceStrong();
  }
}
