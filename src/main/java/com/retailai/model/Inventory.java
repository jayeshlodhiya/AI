
package com.retailai.model;
import jakarta.persistence.*;
@Entity @Table(name="inventory")
public class Inventory {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @ManyToOne(optional=false, fetch=FetchType.LAZY) private Product product;
  private String variant; private Integer qty; private Integer reorderLevel; private String location;
  public Long getId(){return id;} public void setId(Long id){this.id=id;}
  public Product getProduct(){return product;} public void setProduct(Product product){this.product=product;}
  public String getVariant(){return variant;} public void setVariant(String variant){this.variant=variant;}
  public Integer getQty(){return qty;} public void setQty(Integer qty){this.qty=qty;}
  public Integer getReorderLevel(){return reorderLevel;} public void setReorderLevel(Integer reorderLevel){this.reorderLevel=reorderLevel;}
  public String getLocation(){return location;} public void setLocation(String location){this.location=location;}
}
