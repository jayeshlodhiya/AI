
package com.retailai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.retailai.repo")
public class AiRetailAssistantApp {
  public static void main(String[] args) {
    SpringApplication.run(AiRetailAssistantApp.class, args);
  }
}
