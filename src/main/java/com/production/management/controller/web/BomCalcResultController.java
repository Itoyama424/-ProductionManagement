package com.production.management.controller.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.production.management.dto.BomExpansionResult;
import com.production.management.exception.CircularReferenceException;
import com.production.management.form.BomSearchForm;
import com.production.management.service.structure.BomExpansionAjaxService;
import com.production.management.service.structure.BomExpansionByMapToCalcResService;

@Controller
@RequestMapping("/bom")
public class BomCalcResultController {
  
  @Autowired
  private BomExpansionByMapToCalcResService bomExpansionByMapToCalcResService; 
  
  @Autowired
  private BomExpansionAjaxService bomExpansionAjaxService;
  /**
   * 総展開でMapを使い、対象部品IDから紐づくBOMを展開する
   * 
   * @param form
   * @param result
   * @param model
   * @return
   */
  @GetMapping(value = "display", params = "bomcalc" )
  public String calculateByMapGross(@Validated @ModelAttribute BomSearchForm form,
      BindingResult result, Model model) {

    // 入力チェック
    if (result.hasErrors()) {
      return "bom/index";

    }

    try {

      BomExpansionResult ExpansionResult = bomExpansionByMapToCalcResService
          .calculateRequirements(form.getParentId(), form.getOrderQty());

      model.addAttribute("res", ExpansionResult);

    } catch (CircularReferenceException e) {

      model.addAttribute("errorMessage", e.getMessage());
      return "/bom/index";
    }

    return "/bom/bom_display_lazyload";
  }
  
  
  @GetMapping("expand-child")
  public String expandChild(
      @RequestParam("parentId") String parentId,
      @RequestParam("calcId") String calcId,
      @RequestParam("parentLevel") int parentLevel,
      Model model
      ) {
    System.out.println(parentId);
    System.out.println(calcId);
    System.out.println(parentLevel);
    
    BomExpansionResult ExpansionResult = bomExpansionAjaxService.getBomRowByUuidAndItemId(parentId, calcId, parentLevel);
    
    model.addAttribute("res", ExpansionResult);
    
    return "bom/bom_display_lazyload::bom-row-unit";
  }

}
