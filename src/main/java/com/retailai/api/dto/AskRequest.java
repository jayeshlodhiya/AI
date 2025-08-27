package com.retailai.api.dto;

public class AskRequest {
    private String q;           // optional
    private String message;     // optional
    private String tenant_id;   // optional

    public String getQ() { return q; }
    public void setQ(String q) { this.q = q; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getTenant_id() { return tenant_id; }
    public void setTenant_id(String tenant_id) { this.tenant_id = tenant_id; }
}
