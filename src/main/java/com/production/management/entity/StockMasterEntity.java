package com.production.management.entity;

import java.math.BigDecimal;
import lombok.Data;

/**
 * 在庫マスタ Entity
 * 実在庫、不良在庫、保留在庫を管理する
 */
@Data
public class StockMasterEntity {
    /** 品目ID (varchar 20) */
    private String itemId;

    /** 実在庫（倉庫にある現物総数） (numeric 18,3) */
    private BigDecimal stockQuantity;

    /** 不良在庫数 (numeric 18,3) */
    private BigDecimal defectiveQuantity;

    /** 保留在庫数（手がかり） (numeric 18,3) */
    private BigDecimal holdQuantity;
}
