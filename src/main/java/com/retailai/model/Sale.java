// src/main/java/com/retailai/model/Sale.java
package com.retailai.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sales")
public class Sale {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 20)
  private String channel; // e.g., "pos", "online"

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "invoice_no", nullable = false, unique = true, length = 50)
  private String invoiceNo;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal total;

  @Column(name = "customer_id")
  private Long customerId;

  // Optional: bidirectional mapping to items
  @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private List<SaleItem> items= new ArrayList<>();;
  // Convenience methods keep both sides in sync
  public void addItem(SaleItem item) {
    items.add(item);
    item.setSale(this);
  }

  public void removeItem(SaleItem item) {
    items.remove(item);
    item.setSale(null);
  }
  // getters/setters …
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getChannel() { return channel; }
  public void setChannel(String channel) { this.channel = channel; }
  public OffsetDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
  public String getInvoiceNo() { return invoiceNo; }
  public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }
  public BigDecimal getTotal() { return total; }
  public void setTotal(BigDecimal total) { this.total = total; }
  public Long getCustomerId() { return customerId; }
  public void setCustomerId(Long customerId) { this.customerId = customerId; }
  public List<SaleItem> getItems() { return items; }
  public void setItems(List<SaleItem> items) { this.items = items; }
}
