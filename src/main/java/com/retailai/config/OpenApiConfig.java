
package com.retailai.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class OpenApiConfig {
  @Primary
  @Bean
  public OpenAPI retailAssistantOpenAPI() {
    return new OpenAPI()
      .info(new Info()
        .title("AI Retail Assistant API")
        .description("APIs for inventory, ingest, chat, and RAG")
        .version("v1.0.0")
        .contact(new Contact().name("Retail AI").email("support@example.com"))
        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0.html")))
      .externalDocs(new ExternalDocumentation().description("Health").url("/api/health"));
  }
}
