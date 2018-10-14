package com.atguigu.gmall.bean;

import org.springframework.boot.autoconfigure.web.ResourceProperties;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.List;

public class SpuInfo implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    private String spuName;

    private String description;

    private  String catalog3Id;


    @Transient
    private List<SpuSaleAttr> spuSaleAttrList;
    @Transient
    private List<SpuImage> spuImageList;

    public List<SpuSaleAttr> getSpuSaleAttrList() {
        return spuSaleAttrList;
    }

    public void setSpuSaleAttrList(List<SpuSaleAttr> spuSaleAttrList) {
        this.spuSaleAttrList = spuSaleAttrList;
    }

    public List<SpuImage> getSpuImageList() {
        return spuImageList;
    }

    public void setSpuImageList(List<SpuImage> spuImageList) {
        this.spuImageList = spuImageList;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSpuName() {
        return spuName;
    }

    public void setSpuName(String spuName) {
        this.spuName = spuName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCatalog3Id() {
        return catalog3Id;
    }

    public void setCatalog3Id(String catalog3Id) {
        this.catalog3Id = catalog3Id;
    }

    @Override
    public String toString() {
        return "SpuInfo{" +
                "id='" + id + '\'' +
                ", spuName='" + spuName + '\'' +
                ", description='" + description + '\'' +
                ", catalog3Id='" + catalog3Id + '\'' +
                ", spuSaleAttrList=" + spuSaleAttrList +
                ", spuImageList=" + spuImageList +
                '}';
    }
}
