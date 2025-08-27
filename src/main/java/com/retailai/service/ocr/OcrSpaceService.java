package com.retailai.service.ocr;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.*;

@Service
public class OcrSpaceService {

  private final String API_KEY = "helloworld"; // Demo key, replace with yours
  private final String API_URL = "https://api.ocr.space/parse/image";

  public String extractText(byte[] fileBytes, String filename) {
    RestTemplate restTemplate = new RestTemplate();

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    headers.add("apikey", API_KEY);

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("file", new ByteArrayResource(fileBytes) {
      @Override
      public String getFilename() {
        return filename;
      }
    });
    body.add("language", "eng");

    HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

    ResponseEntity<String> response = restTemplate.postForEntity(API_URL, requestEntity, String.class);
    return response.getBody();
  }
}
