package com.production.management.service.structure;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.production.management.dto.BomExpansionResult;
import com.production.management.dto.BomViewDto;
import com.production.management.entity.BomEntity;
import com.production.management.entity.ItemMasterEntity;
import com.production.management.exception.CircularReferenceException;
import com.production.management.mapper.FullBomExpansionMapper;
import com.production.management.mapper.ItemMasterMapper;

/**
 * BOM展開系サービスクラス
 */
@Service
public class BomExpansionByMapService {

  /** 部品マスタマッパー */
  @Autowired
  private ItemMasterMapper itemMasterMapper;

  /** Bom全展開マッパー */
  @Autowired
  private FullBomExpansionMapper fullBomExpansionMapper;

  /** 開始階層 */
  private static final int START_LEVEL = 1;
  private static final String EMP_STR = "未設定";

  /**
   * 必要数計算処理
   * 
   * @param targetId
   * @param orderQty
   * @return
   */
  public BomExpansionResult calculateRequirements(String targetId, BigDecimal orderQty,
      boolean isNet) {

    // 展開対象の部材（完成品）
    ItemMasterEntity targetItem = itemMasterMapper.getItemMasterById(targetId);

    List<BomEntity> allBomList = fullBomExpansionMapper.getAllBomList();
    // 全BOMのマップ
    Map<String, List<BomEntity>> allBomMap = this.getBomMap(allBomList);
    // 結果Bomリスト
    List<BomViewDto> resList = new ArrayList<BomViewDto>();
    // 循環参照監視Set
    Set<String> currentPath = new LinkedHashSet<>(55);
    // 再帰展開処理
    this.recursiveExpandRequirements(targetId, orderQty, START_LEVEL, allBomMap, resList,
        currentPath, isNet);

    BomExpansionResult result = new BomExpansionResult();

    // 結果情報
    result.setParentItem(targetItem);
    result.setBomList(resList);
    result.setOrderQty(orderQty);

    return result;
  }

  /**
   * 再帰展開必要数量取得処理
   * 
   * @param pId 部品ID
   * @param pQty
   * @param lv
   * @param resList
   */
  private void recursiveExpandRequirements(String targetId, BigDecimal orderQty, int lv,
      Map<String, List<BomEntity>> allBomMap, List<BomViewDto> resList, Set<String> currentPath,
      boolean isNet) {

    List<BomEntity> childList = allBomMap.get(targetId);

    // 捜査を記録する
    if (!currentPath.add(targetId)) {
      throw new CircularReferenceException(
          currentPath.stream().reduce((a, b) -> a + ">" + b).orElse(""));
    }

    try {
      
      if( Objects.nonNull(childList)) {
        
        for (BomEntity bomEntity : childList) {
    
          BomViewDto bomViewDto = this.convertToViewDto(lv, bomEntity, orderQty);
          
          if(isNet) {
            
            if(START_LEVEL == lv) {
              // 結果リストへ追加
              resList.add(bomViewDto);
            }
            
          } else {
            // 結果リストへ追加
            resList.add(bomViewDto);
          }
    
          if (isNet) {
            // 純展開(net)再帰呼出
            if (bomViewDto.getCalcShortageQty().signum() > 0) {
              this.recursiveExpandRequirements(bomViewDto.getItemId(), bomViewDto.getCalcShortageQty(),
                  lv + 1, allBomMap, resList, currentPath, isNet);
            }
          } else {
            // 総展開(gross)再帰呼出
            this.recursiveExpandRequirements(bomViewDto.getItemId(), bomViewDto.getCalcTotalRequired(),
                lv + 1, allBomMap, resList, currentPath, isNet);
          }
        };
      }
      
    } finally {
      // CircularReferenceException意外の例外発生時でもログを正しく出すためにはFinalyで削除
      currentPath.remove(targetId);
    }
  }

  private Map<String, List<BomEntity>> getBomMap(List<BomEntity> bomList) {

    Map<String, List<BomEntity>> bomEntityMap = new HashMap<String, List<BomEntity>>();

    bomList.stream().forEach(bom -> {

      if (Objects.nonNull(bomEntityMap.get(bom.getParentId()))) {

        bomEntityMap.get(bom.getParentId()).add(bom);
      } else {

        List<BomEntity> childList = new ArrayList<BomEntity>();
        childList.add(bom);
        bomEntityMap.put(bom.getParentId(), childList);
      }
    });

    return bomEntityMap;
  }

  /**
   * Bom情報をEntityからViewへ変換設定する
   * 
   * @param lv
   * @param entity
   * @param parentOrderQty
   * @return
   */
  private BomViewDto convertToViewDto(int lv, BomEntity entity, BigDecimal parentOrderQty) {

    BomViewDto view = new BomViewDto();

    // 基本項目のコピー
    view.setLevel(lv);
    view.setItemId(entity.getItemId());
    view.setItemName(entity.getItemName());
    view.setUnit(entity.getUnit());
    // 構成数（親１個あたり）
    view.setCalcQuantity(entity.getQuantity());
    view.setQuantityDisp(entity.getQuantity().toPlainString());

    // 総必要数 製造指示数 * 構成数
    BigDecimal totalRequired = parentOrderQty.multiply(entity.getQuantity());
    view.setTotalRequiredDisp(totalRequired.stripTrailingZeros().toPlainString());
    view.setCalcTotalRequired(totalRequired);

    if (Objects.isNull(entity.getStockQuantity())) {
      // --- 在庫設定なし ---

      // 在庫数
      view.setStockQtyDisp(EMP_STR);
      view.setCalcStockQty(BigDecimal.ZERO);
      // 不良在庫数
      view.setDefectiveQtyDisp(EMP_STR);
      view.setCalcDefectiveQty(BigDecimal.ZERO);
      // 保留
      view.setHoldQtyDisp(EMP_STR);
      view.setCalcHoldQty(BigDecimal.ZERO);
      // 有効在庫
      view.setAvailableQtyDisp(EMP_STR);
      view.setCalcAvailableQty(BigDecimal.ZERO);

    } else {
      // --- 在庫設定あり ---

      // 実在庫（倉庫にある現物総数）
      BigDecimal stock = entity.getStockQuantity();
      // 不良在庫数
      BigDecimal defective =
          Optional.ofNullable(entity.getDefectiveQuantity()).orElse(BigDecimal.ZERO);
      // 保留在庫数（手がかり）
      BigDecimal hold = Optional.ofNullable(entity.getHoldQuantity()).orElse(BigDecimal.ZERO);
      // 有効在庫 = 実在個数 - 不良在庫数 - 保留在庫数
      BigDecimal availableQuantity = stock.subtract(defective).subtract(hold);

      // 在庫数
      view.setStockQtyDisp(stock.stripTrailingZeros().toPlainString());
      view.setCalcStockQty(stock);
      // 不良在庫数
      view.setDefectiveQtyDisp(defective.stripTrailingZeros().toPlainString());
      view.setCalcDefectiveQty(defective);
      // 保留
      view.setHoldQtyDisp(hold.stripTrailingZeros().toPlainString());
      view.setCalcHoldQty(hold);

      view.setAvailableQtyDisp(availableQuantity.stripTrailingZeros().toPlainString());
      view.setCalcAvailableQty(availableQuantity);

    }

    // 不足の算出
    BigDecimal shortageQuantity = view.getCalcTotalRequired().subtract(view.getCalcAvailableQty());
    shortageQuantity = (shortageQuantity.signum() > 0) ? shortageQuantity : BigDecimal.ZERO;

    view.setCalcShortageQty(shortageQuantity);
    view.setShortageQtyDisp(shortageQuantity.stripTrailingZeros().toPlainString());

    return view;

  }

}
