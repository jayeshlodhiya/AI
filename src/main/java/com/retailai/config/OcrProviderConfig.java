// src/main/java/com/retailai/config/OcrProviderConfig.java
package com.retailai.config;

import com.retailai.service.OcrService;
import com.retailai.service.ocr.OcrSpaceOcrService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class OcrProviderConfig {

  @Bean
  public WebClient.Builder webClientBuilder() {
    return WebClient.builder();
  }

  @Bean
  public OcrService ocrService(WebClient.Builder builder) {
    return new OcrSpaceOcrService(builder);
  }
}
