package com.production.management.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.production.management.entity.BomMasterEntity;

@Mapper
public interface BomMasterMapper {
  
  List<BomMasterEntity> getRawBomList();

}
