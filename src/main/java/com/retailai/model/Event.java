
package com.retailai.model;
import jakarta.persistence.*; import java.time.OffsetDateTime;
@Entity @Table(name="events")
public class Event {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  private String type; @Column(length=4000) private String payloadJson; private OffsetDateTime createdAt = OffsetDateTime.now();
  public Long getId(){return id;} public void setId(Long id){this.id=id;}
  public String getType(){return type;} public void setType(String type){this.type=type;}
  public String getPayloadJson(){return payloadJson;} public void setPayloadJson(String payloadJson){this.payloadJson=payloadJson;}
  public OffsetDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(OffsetDateTime createdAt){this.createdAt=createdAt;}
}
