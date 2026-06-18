package com.production.management.entity;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class BomEntity {
  private int level;
  private String parentId;
  private String itemId;
  private String itemName;
  private String unit;
  private BigDecimal quantity; // 構成数
  private BigDecimal stockQuantity; // 在庫数（NULLを許容）
  private BigDecimal defectiveQuantity; // 不良数（NULLを許容）
  private BigDecimal holdQuantity; // 保留数（NULLを許容）
}
