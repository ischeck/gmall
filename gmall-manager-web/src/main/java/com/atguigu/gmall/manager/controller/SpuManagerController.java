package com.atguigu.gmall.manager.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.ManagerService;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class SpuManagerController {
    @Reference
    ManagerService managerService;
    @RequestMapping("spuListPage")
    public String spuListPage() {
        return "spuListPage";
    }

    @RequestMapping("spuList")
    @ResponseBody
    public List<SpuInfo> getSpuInfoList(@RequestParam Map<String,String> map){
        String catalog3Id = map.get("catalog3Id");
        SpuInfo spuInfo =new SpuInfo();
        spuInfo.setCatalog3Id(catalog3Id);
        List<SpuInfo> spuInfoList = managerService.getSpuInfoList(spuInfo);
        return spuInfoList;

    }
    @RequestMapping(value = "saveSpuInfo",method = RequestMethod.POST)
    @ResponseBody
    public String saveSpuInfo(SpuInfo spuInfo){
        managerService.saveSpuInfo(spuInfo);
        return  "success";
    }


    @RequestMapping("baseSaleAttrList")
    @ResponseBody
    public List<BaseSaleAttr>  getBaseSaleAttr(){
        List<BaseSaleAttr> baseSaleAttr = managerService.getBaseSaleAttrList();
        return baseSaleAttr;

    }

    @RequestMapping("spuSaleAttrList")
    @ResponseBody
    public List<SpuSaleAttr> getSpuSaleAttrList(HttpServletRequest httpServletRequest){
        String spuId = httpServletRequest.getParameter("spuId");
        List<SpuSaleAttr> spuSaleAttrList = managerService.getSpuSaleAttrList(spuId);

        return spuSaleAttrList;

    }


    @RequestMapping("attrInfoList")
    @ResponseBody
    public List<BaseAttrInfo>  getAttrList(@RequestParam Map<String,String> map){
        String catalog3Id =   map.get("catalog3Id") ;
        List<BaseAttrInfo> attrList = managerService.getAttrList(catalog3Id);
        return attrList;
    }

    @RequestMapping("attrListAndInner")
    @ResponseBody
    public List<BaseAttrInfo>  getAttrListAndInner(@RequestParam Map<String,String> map){
        String catalog3Id =   map.get("catalog3Id") ;
        List<BaseAttrInfo> attrList = managerService.getAttrListAndInner(catalog3Id);
        return attrList;
    }

    @RequestMapping(value ="spuImageList" ,method = RequestMethod.GET)
    @ResponseBody
    public  List<SpuImage> getSpuImageList(@RequestParam Map<String,String> map){
        String spuId = map.get("spuId");
        List<SpuImage> spuImageList = managerService.getSpuImageList(spuId);
        return spuImageList;
    }


    @RequestMapping(value ="deleteOneSpu" ,method = RequestMethod.POST)
    @ResponseBody
    public  String deleteOneSpu(@RequestParam Map<String,String> map){
        String spuId = map.get("spuId");
         managerService.deleteOneSpu(spuId);
        return "success";
    }







}
