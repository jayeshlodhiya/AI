package com.retailai.service;

import com.retailai.repo.SaleItemRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class SalesHistoryService {

  private final SaleItemRepository saleItemRepository;

  public SalesHistoryService(SaleItemRepository saleItemRepository) {
    this.saleItemRepository = saleItemRepository;
  }

  /**
   * Summarize sales for a given product ID
   * @param productId product to summarize
   * @return map with sales history stats
   */
  public Map<String, Object> summarizeByProductId(Long productId) {
    Map<String, Object> summary = new HashMap<>();

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime thirtyDaysAgo = now.minusDays(30);

    int units30d = saleItemRepository.countByProductId(productId);
    summary.put("units_30d", units30d);

    Double avgPrice = saleItemRepository.findAveragePriceByProductId(productId);
    summary.put("avg_price", avgPrice != null ? avgPrice : 0.0);

    return summary;
  }
}
