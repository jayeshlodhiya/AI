package com.retailai.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PromoResponse {
  private String id;
  private String createdTime;
  private String status;
  private String url;
  private int promoId;

  // Getters & Setters
  public String getId() { return id; }
  public void setId(String id) { this.id = id; }

  public String getCreatedTime() { return createdTime; }
  public void setCreatedTime(String createdTime) { this.createdTime = createdTime; }

  @JsonProperty("Status")   // maps "Status" (capitalized) → status
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }

  @JsonProperty("Url")      // maps "Url" → url
  public String getUrl() { return url; }
  public void setUrl(String url) { this.url = url; }

  public int getPromoId() { return promoId; }
  public void setPromoId(int promoId) { this.promoId = promoId; }
}

