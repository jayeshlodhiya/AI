package com.retailai.model;

import jakarta.persistence.*;

@Entity
@Table(name = "rag_chunk_new", indexes = {
        @Index(name = "idx_rag_tenant_doc", columnList = "tenantId, docId"),
        @Index(name = "idx_rag_tenant_type", columnList = "tenantId, type")
})
public class RagChunkNew {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tenantId;
    private String docId;
    private String type;
    @Column(name = "doc_type")      // maps snake_case column
    private String docType;
    private Integer chunkIndex;

    @Lob
    @Column(columnDefinition = "CLOB",unique = true)
    private String content;

    @Column(name="phones_norm", length=2000)
    private String phonesNorm;   // comma-separated

    @Column(name="emails_norm", length=2000)
    private String emailsNorm;   // comma-separated

    @Column(name="invoice_ids", length=2000)
    private String invoiceIds;   // comma-separated: INV-*, #1234 etc.

    @Column(name="search_text")
    private String searchText;   // normalized copy for FTS/LIKE

    public String getPhonesNorm() {
        return phonesNorm;
    }

    public void setPhonesNorm(String phonesNorm) {
        this.phonesNorm = phonesNorm;
    }

    public String getEmailsNorm() {
        return emailsNorm;
    }

    public void setEmailsNorm(String emailsNorm) {
        this.emailsNorm = emailsNorm;
    }

    public String getInvoiceIds() {
        return invoiceIds;
    }

    public void setInvoiceIds(String invoiceIds) {
        this.invoiceIds = invoiceIds;
    }

    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    // getters/setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getDocId() { return docId; }
    public void setDocId(String docId) { this.docId = docId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Integer getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }
}
