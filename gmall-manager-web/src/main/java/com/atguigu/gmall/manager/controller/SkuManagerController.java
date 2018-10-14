package com.atguigu.gmall.manager.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManagerService;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

@Controller
public class SkuManagerController {

    @Reference
    ManagerService managerService;


    @RequestMapping(value = "saveSkuInfo",method = RequestMethod.POST)
    @ResponseBody
    public String saveSkuInfo(SkuInfo skuInfo){
        managerService.saveSkuInfo(skuInfo);

        return "success";
    }


    @RequestMapping(value="skuInfoListBySpu")
    @ResponseBody
    public List<SkuInfo> getSkuInfoListBySpu(HttpServletRequest httpServletRequest){
        String spuId = httpServletRequest.getParameter("spuId");
        List<SkuInfo> skuInfoList = managerService.getSkuInfoListBySpu(spuId);
        return skuInfoList;
    }





}
