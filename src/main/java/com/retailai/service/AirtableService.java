package com.retailai.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;

@Service
public class AirtableService {
  private final WebClient airtable;

  public AirtableService(WebClient airtableWebClient) {
    this.airtable = airtableWebClient;
  }

  /**
   * List records using POST /listRecords (recommended for long params).
   *
   * @param baseId          e.g. "appXXXXXXXXXXXXXX"
   * @param table           e.g. "Customers" or tblXXXXXXXXXXXX
   * @param filterByFormula e.g. "{Email} = 'alice@example.com'"
   * @param pageSize        1..100
   * @param offset          pass the returned offset to fetch next page; null for first page
   */
  public Mono<Map> listRecords(
    String baseId, String table, String filterByFormula, Integer pageSize, String offset) {

    var body = Map.of(
      "pageSize", pageSize == null ? 100 : pageSize,
      "filterByFormula", filterByFormula == null ? "" : filterByFormula,
      "sort", new Object[] { Map.of("field", "Created", "direction", "desc") },
      "offset", offset == null ? "" : offset
    );

    return airtable.post()
      .uri("https://api.airtable.com/v0/{base}/{table}/listRecords", baseId, table)
      .bodyValue(body)
      .retrieve()
      .bodyToMono(Map.class)
      // retry a few times on 429/5xx with backoff (rate limits)
      .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                   .filter(ex -> ex.getMessage().contains("429") || ex instanceof RuntimeException));
  }
  public Mono<Map> listBases() {
    return airtable.get()
      .uri("/meta/bases")
      .retrieve()
      .bodyToMono(Map.class);
  }
}
