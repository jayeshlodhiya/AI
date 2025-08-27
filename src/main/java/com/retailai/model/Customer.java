
package com.retailai.model;
import jakarta.persistence.*; import java.time.OffsetDateTime;
@Entity @Table(name="customers")
public class Customer {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  private String phoneHash; private String tier; private OffsetDateTime lastVisitAt;
  public Long getId(){return id;} public void setId(Long id){this.id=id;}
  public String getPhoneHash(){return phoneHash;} public void setPhoneHash(String p){this.phoneHash=p;}
  public String getTier(){return tier;} public void setTier(String t){this.tier=t;}
  public OffsetDateTime getLastVisitAt(){return lastVisitAt;} public void setLastVisitAt(OffsetDateTime t){this.lastVisitAt=t;}
}
