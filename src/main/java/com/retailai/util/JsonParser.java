package com.retailai.util;

import java.io.DataInput;
import java.io.IOException;

import org.apache.poi.ss.formula.functions.T;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.retailai.model.PromoResponse;

public class JsonParser {
  private static final ObjectMapper mapper = new ObjectMapper();
  public static PromoResponse parsePromoResponse(String json) {
    try {
      return mapper.readValue(json, PromoResponse.class);
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse JSON", e);
    }
  }

}
