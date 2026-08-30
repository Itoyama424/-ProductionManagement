package com.production.management.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.production.management.dto.AvailableStockMasgerDto;
import com.production.management.entity.StockEntity;

@Mapper
public interface StockMasterMapper {
    /**
     * 品目IDをキーに在庫情報を1件取得する
     * * @param itemId 品目ID
     * @return 在庫情報（存在しない場合はnull）
     */
    StockEntity getStockByItemId(@Param("itemId") String itemId);
    
    /**
     * 全部の在庫の有効在庫を取得する
     * @return
     */
    List<AvailableStockMasgerDto> getAllStockMaster();
}