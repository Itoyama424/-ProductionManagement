package com.production.management.entity;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class BomMasterEntity {
  private String parentId; // 親品目ID
  private String itemId;   // 子品目ID (child_id)
  private BigDecimal quantity; // 構成数
}
