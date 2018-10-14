package com.atguigu.gmall.list.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.BaseAttrInfo;
import com.atguigu.gmall.bean.BaseAttrValue;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.bean.SkuLsParam;
import com.atguigu.gmall.bean.SkuLsResult;
import com.atguigu.gmall.service.ManagerService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @param
 * @return
 */

@Controller
public class ListController {


    @Reference
    ListService listService;

    @Reference
    ManagerService managerService;


    @RequestMapping("list.html")
    public String getList(  SkuLsParam skuLsParam, Model model){

        skuLsParam.setPageSize(2);
        //全文检索返回匹配到的sku列表
        SkuLsResult skuLsResult = listService.searchSkuInfoList(skuLsParam);
        model.addAttribute("keyword",skuLsParam.getKeyword());
        model.addAttribute("skuLsInfoList",skuLsResult.getSkuLsInfoList());
        //查询出所有sku的平台属性
        List<BaseAttrInfo> attrList = managerService.getAttrList(skuLsResult.getValueIdList());
        model.addAttribute("attrList",attrList);

        //声明面包屑数据集合
        List<BaseAttrValue> selectedValueList= new ArrayList<>(skuLsParam.getValueId().length);


        String urlParam=makeUrlParam(skuLsParam);

        for (Iterator<BaseAttrInfo> iterator = attrList.iterator(); iterator.hasNext(); ) {
            BaseAttrInfo baseAttrInfo =  iterator.next();
            List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
            for (BaseAttrValue baseAttrValue : attrValueList) {
                //存url
                baseAttrValue.setUrlParam(urlParam);
                // 构建面包屑
                if(skuLsParam.getValueId()!=null&&skuLsParam.getValueId().length>0){

                    for (String valueId : skuLsParam.getValueId()) {
                        if(valueId.equals(baseAttrValue.getId())){
                            iterator.remove();

                            BaseAttrValue  attrValueSelected=new BaseAttrValue();
                            attrValueSelected.setValueName(baseAttrInfo.getAttrName()+":"+baseAttrValue.getValueName());
                            attrValueSelected.setId(valueId);
                            attrValueSelected.setUrlParam(makeUrlParam(skuLsParam,valueId));
                            selectedValueList.add(attrValueSelected);
                        }

                    }
                }

            }

        }

        model.addAttribute("selectedValueList",selectedValueList);
        int totalPages = (skuLsResult.getTotal() + skuLsParam.getPageSize() - 1) / skuLsParam.getPageSize();

        model.addAttribute("totalPages",totalPages);

        model.addAttribute("pageNo",skuLsParam.getPageNo());

        model.addAttribute("urlParam",urlParam);
        return "list";
    }

    private  String makeUrlParam(SkuLsParam skuLsParam,String... excludeValueIds){
        String urlParam="";
        if(skuLsParam.getKeyword()!=null){
            urlParam+="keyword="+skuLsParam.getKeyword();
        }
        if(skuLsParam.getCatalog3Id()!=null){
            if(urlParam.length()>0){
                urlParam+="&";
            }
            urlParam+="catalog3Id="+skuLsParam.getCatalog3Id();
        }
        if(skuLsParam.getValueId()!=null&&skuLsParam.getValueId().length>0){

            for (int i = 0; i < skuLsParam.getValueId().length; i++) {
                String valueId  = skuLsParam.getValueId()[i];
                //面包屑的url需排除自己的valueId
                if(excludeValueIds!=null&&excludeValueIds.length>0){
                    String excludeValueId = excludeValueIds[0];
                    if (excludeValueId.equals(valueId)){
                        continue;
                    }
                }

                if(urlParam.length()>0){
                    urlParam+="&";
                }
                urlParam+="valueId="+valueId;
            }
        }
        return  urlParam;

    }

}
