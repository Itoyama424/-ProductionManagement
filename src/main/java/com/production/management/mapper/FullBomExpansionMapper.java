/**
 * BOM全展開
 */
package com.production.management.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.production.management.entity.BomEntity;

@Mapper
public interface FullBomExpansionMapper {

	List<BomEntity> getFullBomExpansion(String itemId);
	
	List<BomEntity> getAllBomList();
}
