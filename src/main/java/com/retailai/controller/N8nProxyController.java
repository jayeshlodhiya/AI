package com.retailai.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import com.retailai.model.PromoResponse;
import com.retailai.service.AirtableService;
import com.retailai.util.JsonParser;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/n8n/promotions")
public class N8nProxyController {
   private final AirtableService airtableService;
   private String promoId ;
    private final WebClient web = WebClient.builder().baseUrl("https://jayeshlodhiya.app.n8n.cloud").build();

    public N8nProxyController(AirtableService airtableService) {
        this.airtableService = airtableService;
    }

    //https://jayeshlodhiya.app.n8n.cloud/webhook/5ad43414-12cd-42d1-bec0-953132fe4dfc
@PostMapping(value = "/start", consumes = MediaType.APPLICATION_JSON_VALUE)
public ResponseEntity<String> startJson(@RequestBody Map<String, Object> payload) {
    Random r = new Random(System.currentTimeMillis() );
    int id = 10000 + r.nextInt(20000);
    promoId = String.valueOf(id);
    payload.put("id",id);
    ResponseEntity<String> response = web.post()
      .uri("https://jayeshlodhiya.app.n8n.cloud/webhook/5ad43414-12cd-42d1-bec0-953132fe4dfc")
      .contentType(MediaType.APPLICATION_JSON)
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(payload)
      .retrieve()
      .toEntity(String.class)
      .block();

    System.out.println("Response from n8n : "+response.getBody());

    return ResponseEntity.ok().body(promoId);
}

    @GetMapping("/status/{id}")
    public ResponseEntity<String> status(@PathVariable String id) throws IOException {

//https://jayeshlodhiya.app.n8n.cloud/webhook/80309d9b-ceca-4e67-b418-3ae7f3eb25ed
      ResponseEntity<String> response = web.get()
        .uri(uriBuilder -> uriBuilder
          .scheme("https")
          .host("jayeshlodhiya.app.n8n.cloud")
          .path("/webhook/80309d9b-ceca-4e67-b418-3ae7f3eb25ed")
          .queryParam("promoId", id)
          .build())
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .toEntity(String.class)
        .block();
      PromoResponse promoResponse = JsonParser.parsePromoResponse(response.getBody());

      System.out.println("Response from n8n : "+promoResponse.getUrl());

      return ResponseEntity.ok().body(promoResponse.getUrl());
    }
}
