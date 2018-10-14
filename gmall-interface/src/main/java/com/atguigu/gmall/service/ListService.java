package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.bean.SkuLsParam;
import com.atguigu.gmall.bean.SkuLsResult;

public interface ListService {

    void saveSkuInfo(SkuLsInfo skuLsInfo);

    SkuLsResult searchSkuInfoList(SkuLsParam skuLsParam);

    void incrHotScore(String skuId);
}
