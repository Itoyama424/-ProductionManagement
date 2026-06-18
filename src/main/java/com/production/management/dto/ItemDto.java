package com.production.management.dto;

import lombok.Data;

@Data
public class ItemDto {
    private String itemId;   // 品番 (EV-PRO-01 等)
    private String itemName; // 品名
    private String unit;     // 単位 (台, 個 等)
}
