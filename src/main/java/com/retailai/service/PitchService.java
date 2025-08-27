package com.retailai.service;

import com.retailai.model.Product;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Generates short sales tips based on product info and sales history.
 * Can be swapped out later to use an LLM for richer suggestions.
 */
@Service
public class PitchService {

  public String craftPitch(Product p, Map<String, Object> history) {
    StringBuilder pitch = new StringBuilder();

    // Basic category hook
    switch (p.getCategory().toLowerCase()) {
      case "jewelry" -> pitch.append("Highlight craftsmanship and purity. ");
      case "apparel" -> pitch.append("Mention fabric quality and comfort. ");
      case "electronics" -> pitch.append("Emphasize features and warranty. ");
      default -> pitch.append("Share unique selling points. ");
    }

    // Price-based tactic
    if (p.getMrp() != null && p.getCost() != null) {
      BigDecimal discount = p.getMrp().subtract(p.getCost());
      if (discount.compareTo(BigDecimal.ZERO) > 0) {
        pitch.append("Offer a limited-time discount of ₹")
          .append(discount.intValue())
          .append(". ");
      }
    }

    // History-based tactic
    if (history != null) {
      Object units30 = history.get("units_30d");
      if (units30 instanceof Number units && units.intValue() > 5) {
        pitch.append("It's been selling fast — create urgency!");
      } else {
        pitch.append("Build interest by showing customer reviews.");
      }
    }

    return pitch.toString().trim();
  }
}
