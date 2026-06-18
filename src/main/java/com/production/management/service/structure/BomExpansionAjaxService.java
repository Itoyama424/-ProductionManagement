package com.production.management.service.structure;

import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.production.management.dto.BomExpansionResult;
import com.production.management.dto.BomViewDto;
import com.production.management.mapper.BomCalcResultMapper;

/**
 * AjaxによるBOM展開系サービスクラス
 */
@Service
public class BomExpansionAjaxService {
  
  /** Bom展開計算結果マッパー */
  @Autowired
  private BomCalcResultMapper bomCalcResultMapper;
  
  private static final String EMP_STR = "未設定";
  /**
   * uuidとitemIdで
   * @param uuid
   * @param itemId
   * @return
   */
  public BomExpansionResult getBomRowByUuidAndItemId(String parentId, String uuid, int parentLevel) {
  
    // 結果Bomリスト    
    List<BomViewDto> resList = bomCalcResultMapper.getBomCalcResult(parentId, uuid);
    int level;
    for(BomViewDto view : resList ) {
      level = parentLevel + 1;
      view.setLevel(level);
      // --- 構成数(親1個当り) ---
      view.setQuantityDisp(view.getCalcQuantity().stripTrailingZeros().toPlainString());
      // --- 総必要数 ---
      view.setTotalRequiredDisp(view.getCalcTotalRequired().stripTrailingZeros().toPlainString());
      // --- 不足数 ---
      view.setShortageQtyDisp(view.getCalcShortageQty().stripTrailingZeros().toPlainString());
      
      this.convertViewDto(view);
    }
    
    // 結果情報
    BomExpansionResult result = new BomExpansionResult();
    result.setBomList(resList);
    result.setCalcId(uuid);
  
    return result;
  }

  /**
   * 在庫情報の有無で設定を分ける
   * @param view
   */
  private void convertViewDto(BomViewDto view) {
    
    if(Objects.isNull(view.getCalcStockQty())) {
      // 在庫数
      view.setStockQtyDisp(EMP_STR);
      // 不良在庫数
      view.setDefectiveQtyDisp(EMP_STR);
      // 保留
      view.setHoldQtyDisp(EMP_STR);
      // 有効在庫
      view.setAvailableQtyDisp(EMP_STR);
      
    } else {
      
      // --- 在庫（Stock） ---
      view.setStockQtyDisp(view.getCalcStockQty().stripTrailingZeros().toPlainString());
      //--- 不良（Defective） ---
      view.setDefectiveQtyDisp(view.getCalcDefectiveQty().stripTrailingZeros().toPlainString());
      //--- 保留（Hold） ---
      view.setHoldQtyDisp(view.getCalcHoldQty().stripTrailingZeros().toPlainString());
      //--- 有効在庫 ---
      view.setAvailableQtyDisp(view.getCalcAvailableQty().stripTrailingZeros().toPlainString());
    }
  }
}
