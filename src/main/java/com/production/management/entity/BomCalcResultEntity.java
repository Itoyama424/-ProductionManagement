package com.production.management.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class BomCalcResultEntity {
  

  /** 計算実行ID (UUID等) */
  private String calcId;

  /** 品目ID */
  private String itemId;

  /** 展開の起点となった製品ID */
  private String topParentId;

  /** 総必要数 (Gross) */
  private BigDecimal grossQty;

  /** 有効在庫 (計算時点) */
  private BigDecimal availableStock;

  /** 不足数 (Net) */
  private BigDecimal netQty;

  /** 計算日時 */
  private LocalDateTime createdAt;

}
