package com.production.management.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class AvailableStockMasgerDto {
  
  private String itemId;
  
  private BigDecimal AvailableQty;

}
