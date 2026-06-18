package com.production.management.dto;

import java.math.BigDecimal;
import java.util.List;
import com.production.management.entity.ItemMasterEntity;
import lombok.Data;

@Data
public class BomExpansionResult {
    // 親アイテムの基本情報（名称、ID、単位）
    private ItemMasterEntity parentItem; 
    
    // 画面に渡された製造指示数（生の値）
    private BigDecimal orderQty; 
    
    // 再帰的に展開された子部品のフラットなリスト
    private List<BomViewDto> bomList; 
    
    private String calcId;
}
