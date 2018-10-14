package com.atguigu.gmall.manager.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.BaseAttrInfo;
import com.atguigu.gmall.bean.BaseAttrValue;
import com.atguigu.gmall.service.ManagerService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
public class AttrManagerController {
    @Reference
    ManagerService managerService;



    /***
     * 获得属性列表
     * @param map
     * @return
     */
    @RequestMapping("getAttrList")
    @ResponseBody
    public List<BaseAttrInfo>  getAttrList(@RequestParam Map<String,String> map){
        String catalog3Id =   map.get("catalog3Id") ;
        List<BaseAttrInfo> attrList = managerService.getAttrList(catalog3Id);
        return attrList;
    }

    @RequestMapping(value = "saveAttrInfo",method = RequestMethod.POST)
    @ResponseBody
    public String saveAttrInfo(BaseAttrInfo baseAttrInfo){
        managerService.saveAttrInfo(baseAttrInfo);
        return "success";
    }

    @RequestMapping(value = "deleteOneRow",method = RequestMethod.POST)
    @ResponseBody
    public String deleteAttrInfo(BaseAttrInfo baseAttrInfo){
        managerService.deleteAttrInfo(baseAttrInfo);
        return "success";
    }



    @RequestMapping(value = "getAttrValueList",method = RequestMethod.POST)
    @ResponseBody
    public List<BaseAttrValue> getAttrValueList(@RequestParam Map<String,String> map){
        String attrId= map.get("attrId");
        BaseAttrInfo attrInfo = managerService.getAttrInfo(attrId);
        return attrInfo.getAttrValueList();
    }

}
