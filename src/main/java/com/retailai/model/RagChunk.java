
package com.retailai.model;
import jakarta.persistence.*;
@Entity @Table(name="rag_chunks")
public class RagChunk {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  private String tenantId; private String docType; private String docId;
  @Column(length=8000) private String text; private String tags;
  public Long getId(){return id;} public void setId(Long id){this.id=id;}
  public String getTenantId(){return tenantId;} public void setTenantId(String t){this.tenantId=t;}
  public String getDocType(){return docType;} public void setDocType(String d){this.docType=d;}
  public String getDocId(){return docId;} public void setDocId(String d){this.docId=d;}
  public String getText(){return text;} public void setText(String t){this.text=t;}
  public String getTags(){return tags;} public void setTags(String t){this.tags=t;}
}
