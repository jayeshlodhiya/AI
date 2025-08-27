package com.retailai.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "sale_items")
public class SaleItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // link back to Sale header
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "sale_id", nullable = false)
  private Sale sale;

  // link to product table
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id")
  private Product product;

  @Column(nullable = false)
  private Integer qty;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal price; // unit price

  @Column(length = 100)
  private String variant; // e.g., "L", "Red", "128GB"

  @Column(length = 64, nullable = false)
  private String sku; // still keep SKU for fast lookups

  // ---- Getters & Setters ----
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  // Example: selling price (unit price)
  public BigDecimal getUnitPrice() {
    return price; // already stored in sale_items
  }

  public Sale getSale() { return sale; }
  public void setSale(Sale sale) { this.sale = sale; }

  public Product getProduct() { return product; }
  public void setProduct(Product product) { this.product = product; }

  public Integer getQty() { return qty; }
  public void setQty(Integer qty) { this.qty = qty; }

  public BigDecimal getPrice() { return price; }
  public void setPrice(BigDecimal price) { this.price = price; }

  public String getVariant() { return variant; }
  public void setVariant(String variant) { this.variant = variant; }

  public String getSku() { return sku; }
  public void setSku(String sku) { this.sku = sku; }

}
