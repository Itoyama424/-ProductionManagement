package com.production.management.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.production.management.entity.ItemMasterEntity;

@Mapper
public interface ItemMasterMapper {
	/**
	 * item_masterからIdをキーに取得する
	 * @param itemId
	 * @return ItemMasterEntity
	 */
	ItemMasterEntity getItemMasterById(String itemId);
	
	/**
	 * item_masterから全て取得する
     * @return List<ItemMasterEntity>
	 */
	List<ItemMasterEntity> getAllItemMaster();
}
