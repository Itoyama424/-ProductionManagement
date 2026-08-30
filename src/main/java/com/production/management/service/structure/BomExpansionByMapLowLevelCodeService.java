package com.production.management.service.structure;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.production.management.dto.BomExpansionResult;
import com.production.management.dto.BomViewDto;
import com.production.management.entity.BomCalcResultEntity;
import com.production.management.entity.BomMasterEntity;
import com.production.management.entity.ItemMasterEntity;
import com.production.management.entity.StockEntity;
import com.production.management.mapper.BomCalcResultMapper;
import com.production.management.mapper.BomMasterMapper;
import com.production.management.mapper.ItemMasterMapper;
import com.production.management.mapper.StockMasterMapper;

/**
 * BOM展開系サービスクラス
 */
@Service
public class BomExpansionByMapLowLevelCodeService {

  /** ItemMasterEntity マッパー */
  @Autowired
  private ItemMasterMapper itemMasterMapper;

  /** BomMaster マッパー */
  @Autowired
  private BomMasterMapper bomMasterMapper;

  /** StockMaster マッパー */
  @Autowired
  private StockMasterMapper stockMasterMapper;

  /** bomCalcResultMapper マッパー */
  @Autowired
  private BomCalcResultMapper bomCalcResultMapper;

  /** 開始階層 */
  private static final int START_LEVEL = 1;
  private static final String EMP_STR = "未設定";

  /**
   * 全展開したBOMをMap<親item_id, 直下の子のList<BomMasterEntity>>に作り直す
   * @param bomList 単純な全展開BOMのリスト
   * @return
   */
  private Map<String, List<BomMasterEntity>> getBomMap() {
    // 無条件に全BOMを取得
    List<BomMasterEntity> bomList = bomMasterMapper.getRawBomList();
    
    Map<String, List<BomMasterEntity>> bomEntityMap = bomList.stream()
        .collect(Collectors.groupingBy(BomMasterEntity::getParentId, Collectors.toList()));

    return bomEntityMap;
  }
  /**
   * 必要数計算処理
   * 
   * @param targetId
   * @param orderQty
   * @return
   */
  public BomExpansionResult calculateRequirements(String targetId, BigDecimal orderQty) {

    // 結果Bomリスト
    List<BomViewDto> resList = new ArrayList<BomViewDto>();
    // 集計マップ
    Map<String, BigDecimal> summaryMap = new HashMap<>();
    // 全BOMのマップ
    Map<String, List<BomMasterEntity>> allBomMap = this.getBomMap();

    // 全在庫マスタ（有効在庫計算済み 全在庫数-不良品-予約済）
    Map<String, BigDecimal> allAvaivalStock = 
        stockMasterMapper.getAllStockMaster().stream().collect(
            Collectors.toMap(avaival -> avaival.getItemId(), avaival -> avaival.getAvailableQty())
            );
    
    Map<Integer,List<String>>  lccGroupMap = this.initializeLLcMap(allBomMap);
     
//    // 再帰展開処理
//    this.recursiveExpandRequirements(targetId, orderQty, START_LEVEL, allBomMap, resList,
//        currentPath, summaryMap, memoMap, allStockMap);

    // 起点
    summaryMap.put(targetId, orderQty);
    // 最大の階層
    int maxLevel = lccGroupMap.keySet().stream().max((a, b) -> a.compareTo(b)).orElse(0);
    
    for(int lv = 0 ; lv < maxLevel ; lv++) {
      
      List<String> itemInlevel = lccGroupMap.getOrDefault(lv, Collections.emptyList());
      
      // LccMapからレベルの属するitemId
      for(String itemId : itemInlevel) {
        // いま
        BigDecimal totalGross = summaryMap.getOrDefault(itemId, BigDecimal.ZERO);
        
        
        
      }
      
      
    }
    // item_idごとの総必要数をbom_calc_resultに登録する 
    String uuid = UUID.randomUUID().toString();
    this.insertBomCalcResult(summaryMap, targetId, uuid);

    // 結果情報
    BomExpansionResult result = new BomExpansionResult();
  //  result.setParentItem(targetItem);
    result.setBomList(resList);
    result.setOrderQty(orderQty);

    // ↓ これがセットされていないと、画面のボタンにUUIDが埋め込まれず、Ajaxが失敗します。
    result.setCalcId(uuid);

    return result;
  }

  private Map<Integer,List<String>> initializeLLcMap(Map<String, List<BomMasterEntity>> allBomMap) {
    
    boolean isChanged = true;
    int loopCount = 0;
    int MAX_DEPTH = 100;
    
    // 部品マスタのIDと初期値０のマップ
    Map<String,Integer> llcMap = this.getInitialLccMap();
    
    while(isChanged) {
    
      isChanged = false;
      loopCount++;
      
      for(String parentId : allBomMap.keySet()) {
        
        Integer parentLvel = llcMap.get(parentId);
        if(parentLvel == null) continue;
        
        Integer newPotentialLevel = parentLvel + 1;
        
        List<BomMasterEntity> childList = allBomMap.get(parentId);
        
        for(BomMasterEntity bomEntity : childList) {
          
          Integer nowChildLevel = llcMap.get(bomEntity.getItemId());
          
          if(nowChildLevel != null && nowChildLevel < newPotentialLevel) {
            // 深いレベルへ書き換え
            llcMap.put(bomEntity.getItemId(), newPotentialLevel);
            // 誰か一人でも動いたら「おかわり（もう一周）」フラグを立てる
            isChanged = true;
          } 
        }
      }
      
      if(loopCount > MAX_DEPTH) {
        throw new RuntimeException("BOMに循環参照の疑いがあります。計算を中止しました。");
      }
    }
    
    Map<Integer,List<String>> llcGroupMap =  
        llcMap.entrySet().stream().collect(Collectors.groupingBy(llcMapSet -> llcMapSet.getValue(),
        Collectors.mapping(llcMapSet -> llcMapSet.getKey(), Collectors.toList())));
    
    
    return llcGroupMap;
  }
  /**
   * LccMapを初期値レベル０で作る
   * @return lccMap
   */
  private Map<String,Integer> getInitialLccMap() {

    List<ItemMasterEntity> itemList = itemMasterMapper.getAllItemMaster();

    Map<String, Integer> lccMap = itemList.stream().collect(Collectors.toMap(item -> item.getItemId(), item -> 0));

    return lccMap;

  }

  /**
   * Bom情報をEntityからViewへ変換設定する
   * 
   * @param lv
   * @param entity
   * @param parentOrderQty
   * @return
   */
  private BomViewDto convertToViewDto(int lv, BomMasterEntity bomEntity,
      BigDecimal parentOrderQty) {

    // 部品情報の取得
    ItemMasterEntity itemEntity = itemMasterMapper.getItemMasterById(bomEntity.getItemId());
    // 在庫情報の取得
    StockEntity stockEntity = stockMasterMapper.getStockByItemId(bomEntity.getItemId());

    BomViewDto view = new BomViewDto();

    // 基本項目のコピー
    view.setLevel(lv);
    view.setItemId(bomEntity.getItemId());
    view.setItemName(itemEntity.getItemName());
    view.setUnit(itemEntity.getUnit());
    // 構成数（親１個あたり）
    view.setCalcQuantity(bomEntity.getQuantity());
    view.setQuantityDisp(bomEntity.getQuantity().toPlainString());

    // 総必要数 製造指示数 * 構成数
    BigDecimal totalRequired = parentOrderQty.multiply(bomEntity.getQuantity());
    view.setTotalRequiredDisp(totalRequired.stripTrailingZeros().toPlainString());
    view.setCalcTotalRequired(totalRequired);

    if (Objects.isNull(stockEntity)) {
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
      BigDecimal stock = stockEntity.getStockQty();
      // 不良在庫数
      BigDecimal defective =
          Optional.ofNullable(stockEntity.getDefectiveQty()).orElse(BigDecimal.ZERO);
      // 保留在庫数（手がかり）
      BigDecimal hold = Optional.ofNullable(stockEntity.getHoldQty()).orElse(BigDecimal.ZERO);
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
  /**
   * BomCalcResult 登録
   * @param summaryMap
   */
  private void insertBomCalcResult(Map<String, BigDecimal> summaryMap, String topParentId, String uuid) {

    List<BomCalcResultEntity> bomCalcList = new ArrayList<>(1000);

    summaryMap.forEach((itemId, totaclQuantity) -> {

      BomCalcResultEntity bomCalcEntity = new BomCalcResultEntity();
      // item_idの設定
      bomCalcEntity.setItemId(itemId);
      // 対象品番(item_id)の設定
      bomCalcEntity.setTopParentId(topParentId);
      // 総必要数
      bomCalcEntity.setGrossQty(totaclQuantity);

      // 在庫情報の取得
      StockEntity stockEntity = stockMasterMapper.getStockByItemId(itemId);

      bomCalcEntity.setCalcId(uuid);

      if(Objects.nonNull(stockEntity)) {
        // -- 有効在庫 --
        // 実在庫（倉庫にある現物総数）
        BigDecimal stock = stockEntity.getStockQty();
        // 不良在庫数
        BigDecimal defective =
            Optional.ofNullable(stockEntity.getDefectiveQty()).orElse(BigDecimal.ZERO);
        // 保留在庫数（手がかり）
        BigDecimal hold = Optional.ofNullable(stockEntity.getHoldQty()).orElse(BigDecimal.ZERO);
        // 有効在庫 = 実在個数 - 不良在庫数 - 保留在庫数
        BigDecimal availableQuantity = stock.subtract(defective).subtract(hold);

        bomCalcEntity.setAvailableStock(availableQuantity);

        // -- 不足 --
        BigDecimal shortageQuantity = totaclQuantity.subtract(availableQuantity);
        shortageQuantity = (shortageQuantity.signum() > 0) ? shortageQuantity : BigDecimal.ZERO;

        bomCalcEntity.setNetQty(shortageQuantity);

      } else {

        bomCalcEntity.setAvailableStock(BigDecimal.ZERO);
        // 在庫データが存在しないから、総必要数が全てNet（不足）数
        bomCalcEntity.setNetQty(totaclQuantity);
      }


      bomCalcList.add(bomCalcEntity);

      if(bomCalcList.size() >= 1000) {
        // 登録
        bomCalcResultMapper.bomCalcResInsert(bomCalcList);
        // クリア
        bomCalcList.clear();
      }

    });

    // 【重要】ループ終了後、リストに残っている「端数」を最後に保存する
    if (!bomCalcList.isEmpty()) {
      bomCalcResultMapper.bomCalcResInsert(bomCalcList);
      bomCalcList.clear();
    }

  }



  //  /**
  //   * 必要数計算処理
  //   * 
  //   * @param targetId
  //   * @param orderQty
  //   * @return
  //   */
  //  public BomExpansionResult calculateRequirements(String targetId, BigDecimal orderQty) {
  //
  //    // 結果Bomリスト
  //    List<BomViewDto> resList = new ArrayList<BomViewDto>();
  //    // 循環参照監視Set
  //    Set<String> currentPath = new LinkedHashSet<>(55);
  //    // 集計マップ
  //    Map<String, BigDecimal> summaryMap = new HashMap<>();
  //
  //    // 展開対象の部材（完成品）
  //    ItemMasterEntity targetItem = itemMasterMapper.getItemMasterById(targetId);
  //    // 無条件に全BOMを取得
  //    List<BomMasterEntity> allBomList = bomMasterMapper.getRawBomList();
  //    // 全BOMのマップ
  //    Map<String, List<BomMasterEntity>> allBomMap = this.getBomMap(allBomList);
  //
  //    // 全在庫マスタ（有効在庫計算済み）
  //    Map<String, BigDecimal> allStockMap = 
  //        stockMasterMapper.getAllStockMaster().stream().
  //            collect(Collectors.toMap((AvailableStockMasgerDto a) -> a.getItemId(), (AvailableStockMasgerDto a) -> a.getAvailableQty()));
  //    
  //    // 再帰展開処理
  //    this.recursiveExpandRequirements(targetId, orderQty, START_LEVEL, allBomMap, resList,
  //        currentPath, summaryMap, memoMap, allStockMap);
  //
  //    // item_idごとの総必要数をbom_calc_resultに登録する 
  //    String uuid = UUID.randomUUID().toString();
  //    this.insertBomCalcResult(summaryMap, targetId, uuid);
  //
  //    // 結果情報
  //    BomExpansionResult result = new BomExpansionResult();
  //    result.setParentItem(targetItem);
  //    result.setBomList(resList);
  //    result.setOrderQty(orderQty);
  //
  //    // ↓ これがセットされていないと、画面のボタンにUUIDが埋め込まれず、Ajaxが失敗します。
  //    result.setCalcId(uuid);
  //
  //    return result;
  //  }
  //
  //  /**
  //   * 再帰展開必要数量取得処理
  //   * @param targetId
  //   * @param orderQty
  //   * @param lv
  //   * @param allBomMap
  //   * @param currentPath
  //   * @param summaryMap
  //   */
  //  private void recursiveExpandRequirements(
  //      String targetId, 
  //      BigDecimal orderQty, 
  //      int lv,
  //      Map<String, List<BomMasterEntity>> allBomMap, 
  //      Set<String> currentPath,
  //      Map<String, BigDecimal> summaryMap,
  //      Map<String, Map<String, BigDecimal>> memoMap,
  //      Map<String, BigDecimal> allStockMap
  //      ) {
  //    this.recursiveExpandRequirements(targetId, orderQty, lv, allBomMap, null, currentPath, summaryMap, memoMap, allStockMap);
  //  }
  //  /**
  //   * 再帰展開必要数量取得処理
  //   * @param targetId            親のitem_id
  //   * @param orderQty            注文数
  //   * @param lv                  階層 
  //   * @param allBomMap           item_idごとの子のitem_idのList<BomMasterEntity>を保持したMap
  //   * @param resList             List<BomViewDto> 画面に表示する、このメソッドではlv1しか表示しない
  //   * @param currentPath         親から辿ってきたパス、重複参照を見つけるため
  //   * @param summaryMap          item_idごとの総必要数を足しこむ
  //   */
  //  private void recursiveExpandRequirements(
  //      String targetId, 
  //      BigDecimal orderQty, 
  //      int lv,
  //      Map<String, List<BomMasterEntity>> allBomMap, 
  //      List<BomViewDto> resList,
  //      Set<String> currentPath, 
  //      Map<String, BigDecimal> summaryMap,
  //      Map<String, Map<String, BigDecimal>> memoMap,
  //      Map<String, BigDecimal> allStockMap
  //      ) {
  //
  //    try {
  //      // 捜査を記録する
  //      if (!currentPath.add(targetId)) {
  //        throw new CircularReferenceException(
  //            currentPath.stream().reduce((a, b) -> a + ">" + b).orElse("") + ">" + targetId) ;
  //      }
  //
  //      if(memoMap.containsKey(targetId)) {
  //        // メモに存在する
  //        Map<String, BigDecimal> memo = memoMap.get(targetId);
  //
  //        for (Map.Entry<String, BigDecimal> entry : memo.entrySet()) {
  //          summaryMap.merge(entry.getKey(), entry.getValue().multiply(orderQty), BigDecimal::add);
  //        }
  //        return;
  //      }
  //
  //      List<BomMasterEntity> childList = allBomMap.get(targetId);
  //
  //      Map<String, BigDecimal> localRecipe = new HashMap<String, BigDecimal>();
  //
  //      if (Objects.nonNull(childList)) {
  //
  //        for (BomMasterEntity bomEntity : childList) {
  //
  //          localRecipe.merge(bomEntity.getItemId(), bomEntity.getQuantity(), BigDecimal::add);
  //
  //          // 親の必要数 * 構成数
  //          BigDecimal totalRequired = orderQty.multiply(bomEntity.getQuantity());
  //          // bomEntity.getItemId()はbom_master.child_id
  //          summaryMap.merge(bomEntity.getItemId(), totalRequired, BigDecimal::add);
  //
  //          if (Objects.nonNull(resList)) {
  //            // 結果リストへ追加
  //            resList.add(this.convertToViewDto(lv, bomEntity, orderQty));
  //          }
  //
  //          if(allStockMap.get(targetId).compareTo(totalRequired) < 0) {
  //            // 総展開(gross)再帰呼出
  //            this.recursiveExpandRequirements(bomEntity.getItemId(),
  //                totalRequired, lv + 1, allBomMap, currentPath, summaryMap, memoMap, allStockMap);
  //          }
  //          
  //          if(memoMap.containsKey(bomEntity.getItemId())) {
  //
  //            Map<String, BigDecimal> childRecipeMap = memoMap.get(bomEntity.getItemId());
  //
  //            for(Map.Entry<String, BigDecimal> entry : childRecipeMap.entrySet()) {
  //              localRecipe.merge(entry.getKey(), entry.getValue().multiply(bomEntity.getQuantity()), BigDecimal::add);
  //            }
  //          }
  //
  //        };
  //
  //        memoMap.put(targetId, localRecipe);
  //      }
  //// ★llc
  //      for(Map.Entry<String, List<BomMasterEntity>> entory : allBomMap.entrySet()) {
  //        List<BomMasterEntity> entityList = allBomMap.get(entory.getKey());
  //        for(BomMasterEntity bomEntity : entityList) {
  //          
  //        }
  //      }
  //      
  //    } finally {
  //      // CircularReferenceException意外の例外発生時でもログを正しく出すためにはFinalyで削除
  //      currentPath.remove(targetId);
  //    }
  //  }

}
