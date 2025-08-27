package com.retailai.api.dto;

public class RagSource {
    private Long id;
    private String tenant_id;
    private String doc_type;
    private String doc_id;
    private String text;
    private Double score;

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenant_id() { return tenant_id; }
    public void setTenant_id(String tenant_id) { this.tenant_id = tenant_id; }
    public String getDoc_type() { return doc_type; }
    public void setDoc_type(String doc_type) { this.doc_type = doc_type; }
    public String getDoc_id() { return doc_id; }
    public void setDoc_id(String doc_id) { this.doc_id = doc_id; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
}
