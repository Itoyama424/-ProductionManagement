package com.production.management.controller.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import com.production.management.dto.BomExpansionResult;
import com.production.management.exception.CircularReferenceException;
import com.production.management.form.BomSearchForm;
import com.production.management.service.structure.BomExpansionByMapService;
import com.production.management.service.structure.BomExpansionService;

@Controller
@RequestMapping("/bom")
public class BomStructureController {

  /** BOM展開系サービスクラス */
  @Autowired
  private BomExpansionService bomExpansionService;
  
  /** BOM展開系(Map使用)サービスクラス */
  @Autowired
  private BomExpansionByMapService bomExpansionByMapService;

  /**
   * 初期表示を行う
   * 
   * @param form
   * @return
   */
  @GetMapping("/index")
  public String index(@ModelAttribute BomSearchForm form) {
    return "/bom/index";
  }
  
  /**
   * 総展開でMapを使い、対象部品IDから紐づくBOMを展開する
   * 
   * @param form
   * @param result
   * @param model
   * @return
   */
  @GetMapping(value = "display", params = "gross_map" )
  public String calculateByMapGross(@Validated @ModelAttribute BomSearchForm form,
      BindingResult result, Model model) {

    // 入力チェック
    if (result.hasErrors()) {
      return "bom/index";

    }

    try {

      BomExpansionResult ExpansionResult = bomExpansionByMapService
          .calculateRequirements(form.getParentId(), form.getOrderQty(), false);

      model.addAttribute("res", ExpansionResult);

    } catch (CircularReferenceException e) {

      model.addAttribute("errorMessage", e.getMessage());
      return "/bom/index";
    }

    return "/bom/bom_display";
  }

  /**
   * 純展開でMapを使い、対象部品IDから紐づくBOMを展開する
   * 
   * @param form
   * @param result
   * @param model
   * @return
   */
  @GetMapping(value = "display", params = "net_map")
  public String calculateByMapNet(@Validated @ModelAttribute BomSearchForm form,
      BindingResult result, Model model) {

    // 入力チェック
    if (result.hasErrors()) {
      return "bom/index";

    }

    try {
      BomExpansionResult ExpansionResult = bomExpansionByMapService
          .calculateRequirements(form.getParentId(), form.getOrderQty(), true);

      model.addAttribute("res", ExpansionResult);

    } catch (CircularReferenceException e) {

      model.addAttribute("errorMessage", e.getMessage());
      return "/bom/index";
    }

    return "/bom/bom_display";
  }
  
  /**
   * 総展開で対象部品IDから紐づくBOMを展開する
   * 
   * @param form
   * @param result
   * @param model
   * @return
   */
  @GetMapping(value = "display", params = "gross" )
  public String calculateGross(@Validated @ModelAttribute BomSearchForm form, BindingResult result,
      Model model) {

    // 入力チェック
    if (result.hasErrors()) {
      return "bom/index";

    }

    BomExpansionResult ExpansionResult =
        bomExpansionService.calculateRequirements(form.getParentId(), form.getOrderQty(), false);

    model.addAttribute("res", ExpansionResult);
    
    return "/bom/bom_display";
  }
  
  /**
   * 純展開で対象部品IDから紐づくBOMを展開する
   * 
   * @param form
   * @param result
   * @param model
   * @return
   */
  @GetMapping(value = "display", params = "net" )
  public String calculateNet(@Validated @ModelAttribute BomSearchForm form, BindingResult result,
      Model model) {

    // 入力チェック
    if (result.hasErrors()) {
      return "bom/index";

    }

    BomExpansionResult ExpansionResult =
        bomExpansionService.calculateRequirements(form.getParentId(), form.getOrderQty(), true);

    model.addAttribute("res", ExpansionResult);
    
    return "/bom/bom_display";
  }
}
