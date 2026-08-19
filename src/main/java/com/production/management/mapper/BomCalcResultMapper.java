package com.production.management.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.production.management.dto.BomViewDto;
import com.production.management.entity.BomCalcResultEntity;

@Mapper
public interface BomCalcResultMapper {
  
  void bomCalcResInsert(@Param("bomCalcList") List<BomCalcResultEntity> bomCalcList);
  
  List<BomViewDto> getBomCalcResult(@Param("parentId") String parentId, @Param("calcId") String calcId);
}
