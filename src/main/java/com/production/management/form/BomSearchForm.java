package com.production.management.form;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class BomSearchForm {
    @NotBlank(message = "IDは必須です")
    @Pattern(regexp = "^[A-Z0-9-_]+$", message = "英数字のみ入力可能です")
    private String parentId;
    @NotNull(message = "生産指示数を入力してください")
    @Min(value = 1, message = "1個以上で入力してください")
    private BigDecimal orderQty;
    @NotNull(message = "基準日を入力してください")
    @DateTimeFormat(pattern = "yyyy-MM-dd") // HTMLのdate型から変換するために必要
    private LocalDate baseDate;
    
	public String getParentId() {
		return parentId;
	}
	public void setParentId(String parentId) {
		this.parentId = parentId;
	}
	public BigDecimal getOrderQty() {
		return orderQty;
	}
	public void setOrderQty(BigDecimal orderQty) {
		this.orderQty = orderQty;
	}
	public LocalDate getBaseDate() {
		return baseDate;
	}
	public void setBaseDate(LocalDate baseDate) {
		this.baseDate = baseDate;
	}

}
