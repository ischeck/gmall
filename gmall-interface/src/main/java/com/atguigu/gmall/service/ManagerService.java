package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.*;

import java.util.List;


public interface ManagerService {

    List<BaseCatalog1> getCatalog1();

    List<BaseCatalog2> getCatalog2(String catalog1Id);

    List<BaseCatalog3> getCatalog3(String catalog2Id);

    List<BaseAttrInfo> getAttrList(String catalog3Id);

    void saveAttrInfo(BaseAttrInfo baseAttrInfo);

    BaseAttrInfo getAttrInfo(String attrId);

    List<SpuInfo> getSpuInfoList(SpuInfo spuInfo);

    void saveSpuInfo(SpuInfo spuInfo);

    List<BaseSaleAttr> getBaseSaleAttrList();

    void deleteAttrInfo(BaseAttrInfo baseAttrInfo);

    List<SpuSaleAttr> getSpuSaleAttrList(String spuId);


    List<SpuImage> getSpuImageList(String spuId);

    void deleteOneSpu(String spuId);

    List<BaseAttrInfo> getAttrListAndInner(String catalog3Id);

    void saveSkuInfo(SkuInfo skuInfo);

    List<SkuInfo> getSkuInfoListBySpu(String spuId);

    SkuInfo getSkuInfo(String skuId);

    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(String id, String spuId);

    List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId);

    List<BaseAttrInfo> getAttrList(List<String> valueIds);
}
