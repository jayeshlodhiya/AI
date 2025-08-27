
package com.retailai.model;
import jakarta.persistence.*; import java.time.OffsetDateTime;
@Entity @Table(name="promotions")
public class Promotion {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  private String title; private OffsetDateTime startsAt; private OffsetDateTime endsAt; private String status;
  @Column(length=4000) private String ruleJson;
  public Long getId(){return id;} public void setId(Long id){this.id=id;}
  public String getTitle(){return title;} public void setTitle(String title){this.title=title;}
  public OffsetDateTime getStartsAt(){return startsAt;} public void setStartsAt(OffsetDateTime s){this.startsAt=s;}
  public OffsetDateTime getEndsAt(){return endsAt;} public void setEndsAt(OffsetDateTime e){this.endsAt=e;}
  public String getStatus(){return status;} public void setStatus(String status){this.status=status;}
  public String getRuleJson(){return ruleJson;} public void setRuleJson(String ruleJson){this.ruleJson=ruleJson;}
}
