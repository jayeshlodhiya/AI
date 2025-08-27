
package com.retailai.model;
import jakarta.persistence.*;
import java.math.BigDecimal;
@Entity @Table(name="products")
public class Product {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @Column(nullable=false, unique=true) private String sku;
  @Column(nullable=false) private String name;
  private String category; private BigDecimal mrp; private BigDecimal cost;
  @Column(length=4000) private String attributesJson;
  public Long getId(){return id;} public void setId(Long id){this.id=id;}
  public String getSku(){return sku;} public void setSku(String sku){this.sku=sku;}
  public String getName(){return name;} public void setName(String name){this.name=name;}
  public String getCategory(){return category;} public void setCategory(String category){this.category=category;}
  public BigDecimal getMrp(){return mrp;} public void setMrp(BigDecimal mrp){this.mrp=mrp;}
  public BigDecimal getCost(){return cost;} public void setCost(BigDecimal cost){this.cost=cost;}
  public String getAttributesJson(){return attributesJson;} public void setAttributesJson(String a){this.attributesJson=a;}
  // Example: selling price (unit price)
  public BigDecimal getUnitPrice() {
    // If you store price separately, return that
    // Or decide logic (e.g., use MRP as default)
    return this.mrp != null ? this.mrp : this.cost;
  }
}
