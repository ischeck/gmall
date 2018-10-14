package com.atguigu.gmall.manager.serviceimpl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.manager.constant.ManagerConst;
import com.atguigu.gmall.manager.mapper.*;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManagerService;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import tk.mybatis.mapper.entity.Example;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

@Service
public class ManagerServiceImpl implements ManagerService {


    @Reference
    ListService listService;

    @Autowired
    BaseAttrInfoMapper baseAttrInfoMapper;//基本属性表(平台属性表)`base_attr_info`

    @Autowired
    BaseAttrValueMapper baseAttrValueMapper;//基本属性值表`base_attr_value`

    @Autowired
    BaseCatalog1Mapper baseCatalog1Mapper;//一级分类表`base_catalog1`

    @Autowired
    BaseCatalog2Mapper baseCatalog2Mapper;

    @Autowired
    BaseCatalog3Mapper baseCatalog3Mapper;

    @Autowired
    SpuInfoMapper spuInfoMapper;//spu商品表`spu_info`

    @Autowired
    SpuImageMapper spuImageMapper;//spu商品图片表`spu_image`
    @Autowired
    SpuSaleAttrMapper spuSaleAttrMapper;//spu商品属性表`spu_sale_attr`
    @Autowired
    SpuSaleAttrValueMapper spuSaleAttrValueMapper;//spu商品属性值表 `spu_sale_attr_value`

    @Autowired
    BaseSaleAttrMapper baseSaleAttrMapper;//基本销售属性表`base_sale_attr`

    @Autowired
     SkuInfoMapper skuInfoMapper;

    @Autowired
    SkuImageMapper skuImageMapper;

    @Autowired
    SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    RedisUtil redisUtil;


    @Override
    public List<BaseCatalog1> getCatalog1() {
        List<BaseCatalog1> baseCatalog1List = baseCatalog1Mapper.selectAll();
        return baseCatalog1List;
    }

    @Override
    public List<BaseCatalog2> getCatalog2(String catalog1Id) {
        BaseCatalog2 baseCatalog2 = new BaseCatalog2();
        baseCatalog2.setCatalog1Id(catalog1Id);

        List<BaseCatalog2> baseCatalog2List = baseCatalog2Mapper.select(baseCatalog2);
        return baseCatalog2List;
    }

    @Override
    public List<BaseCatalog3> getCatalog3(String catalog2Id) {
        BaseCatalog3 baseCatalog3 = new BaseCatalog3();
        baseCatalog3.setCatalog2Id(catalog2Id);

        List<BaseCatalog3> baseCatalog3List = baseCatalog3Mapper.select(baseCatalog3);
        return baseCatalog3List;
    }

    @Override
    public List<BaseAttrInfo> getAttrList(String catalog3_id) {
        BaseAttrInfo baseAttrInfo = new BaseAttrInfo();
        baseAttrInfo.setCatalog3Id(catalog3_id);

        List<BaseAttrInfo> baseAttrInfoList = baseAttrInfoMapper.select(baseAttrInfo);
        return baseAttrInfoList;

    }
    @Override
    public List<BaseAttrInfo> getAttrListAndInner(String catalog3_id) {

        List<BaseAttrInfo> baseAttrInfoList = baseAttrInfoMapper.selectAttrInfoList(Long.parseLong(catalog3_id));
        return baseAttrInfoList;

    }

    /**
     * 保存sku
     * @param skuInfo
     */
    @Override
    public void saveSkuInfo(SkuInfo skuInfo){
        if(skuInfo.getId()==null||skuInfo.getId().length()==0){
            skuInfo.setId(null);
            skuInfoMapper.insertSelective(skuInfo);
        }else {
            skuInfoMapper.updateByPrimaryKeySelective(skuInfo);
        }


        Example example=new Example(SkuImage.class);
        example.createCriteria().andEqualTo("skuId",skuInfo.getId());
        skuImageMapper.deleteByExample(example);

        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        for (SkuImage skuImage : skuImageList) {
            skuImage.setSkuId(skuInfo.getId());
            if(skuImage.getId()!=null&&skuImage.getId().length()==0) {
                skuImage.setId(null);
            }
            skuImageMapper.insertSelective(skuImage);
        }


        Example skuAttrValueExample=new Example(SkuAttrValue.class);
        skuAttrValueExample.createCriteria().andEqualTo("skuId",skuInfo.getId());
        skuAttrValueMapper.deleteByExample(skuAttrValueExample);

        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        for (SkuAttrValue skuAttrValue : skuAttrValueList) {
            skuAttrValue.setSkuId(skuInfo.getId());
            if(skuAttrValue.getId()!=null&&skuAttrValue.getId().length()==0) {
                skuAttrValue.setId(null);
            }
            skuAttrValueMapper.insertSelective(skuAttrValue);
        }


        Example skuSaleAttrValueExample=new Example(SkuSaleAttrValue.class);
        skuSaleAttrValueExample.createCriteria().andEqualTo("skuId",skuInfo.getId());
        skuSaleAttrValueMapper.deleteByExample(skuSaleAttrValueExample);

        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
            skuSaleAttrValue.setSkuId(skuInfo.getId());
            skuSaleAttrValue.setId(null);
            skuSaleAttrValueMapper.insertSelective(skuSaleAttrValue);
        }



        sendSkuToList(skuInfo);
    }
    private void sendSkuToList(SkuInfo skuInfo){

        SkuLsInfo skuLsInfo=new SkuLsInfo();

        try {
            BeanUtils.copyProperties(skuLsInfo, skuInfo);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        listService.saveSkuInfo(skuLsInfo);

    }
    @Override
    public List<SkuInfo> getSkuInfoListBySpu(String spuId){
        List<SkuInfo> skuInfoList = skuInfoMapper.selectSkuInfoListBySpu(Long.parseLong(spuId));
        return  skuInfoList;

    }

    @Override
    public SkuInfo getSkuInfo(String skuId) {
        SkuInfo skuInfo = null;
        try {
            Jedis jedis = redisUtil.getJedis();
            String skuInfoKey = ManagerConst.SKUKEY_PREFIX + skuId + ManagerConst.SKUKEY_SUFFIX;
            String skuInfoJson = jedis.get(skuInfoKey);

            if (skuInfoJson == null || skuInfoJson.length() == 0) {
                System.err.println(Thread.currentThread().getName()+"缓存未命中！");
                String skuLockKey = ManagerConst.SKUKEY_PREFIX + skuId + ManagerConst.SKULOCK_SUFFIX;
                String lock = jedis.set(skuLockKey, "OK", "NX", "PX", ManagerConst.SKULOCK_EXPIRE_PX);

                if ("OK".equals(lock) ){
                    System.err.println(Thread.currentThread().getName()+"获得分布式锁！");
                    skuInfo = getSkuInfoFromDB(skuId);
                    if(skuInfo==null){
                        jedis.setex(skuInfoKey, ManagerConst.SKUKEY_TIMEOUT, "empty");
                        return null;
                    }


                    String skuInfoJsonNew = JSON.toJSONString(skuInfo);
                    jedis.setex(skuInfoKey, ManagerConst.SKUKEY_TIMEOUT, skuInfoJsonNew);
                    jedis.close();
                    System.err.println(Thread.currentThread().getName()+"查完数据库并存入redis,溜了，溜了");
                    return skuInfo;
                }else{
                    System.err.println(Thread.currentThread().getName()+"未获得分布式锁，开始自旋！");
                    Thread.sleep(1000);
                    jedis.close();
                    return   getSkuInfo(  skuId);
                }

            } else if(skuInfoJson.equals("empty")){
                return null;
            } else {
                System.err.println(Thread.currentThread().getName()+"缓存已命中！！！！！！！！！！！！！！！！！！！");
                skuInfo = JSON.parseObject(skuInfoJson, SkuInfo.class);
                jedis.close();
                return skuInfo;
            }

        }catch (JedisConnectionException e){
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return getSkuInfoFromDB(skuId);

    }


    public SkuInfo getSkuInfoFromDB(String skuId){
        System.err.println(Thread.currentThread().getName()+"查询数据库！");


        SkuInfo skuInfo = skuInfoMapper.selectByPrimaryKey(skuId);
        if(skuInfo!=null){
            SkuImage skuImageQuery=new SkuImage();
            skuImageQuery.setSkuId(skuId);
            List<SkuImage> skuImageList = skuImageMapper.select(skuImageQuery);

            skuInfo.setSkuImageList(skuImageList);
        }
/*

        if(skuInfo!=null){
            SkuAttrValue skuAttrValueQuery=new SkuAttrValue();
            skuAttrValueQuery.setSkuId(skuId);
            List<SkuAttrValue> skuAttrValueList = skuAttrValueMapper.select(skuAttrValueQuery);

            skuInfo.setSkuAttrValueList(skuAttrValueList);
        }
        if(skuInfo!=null){
            SkuSaleAttrValue skuSaleAttrValueQuery=new SkuSaleAttrValue();
            skuSaleAttrValueQuery.setSkuId(skuId);
            List<SkuSaleAttrValue> skuSaleAttrValueList = skuSaleAttrValueMapper.select(skuSaleAttrValueQuery);

            skuInfo.setSkuSaleAttrValueList(skuSaleAttrValueList);
        }
*/

        return skuInfo;
    }
    @Override
    public  List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(String skuId,String spuId){

        List<SpuSaleAttr> spuSaleAttrList = spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(Long.parseLong(skuId),Long.parseLong(spuId));
        return spuSaleAttrList;

    }

    @Override
    public List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId) {
        List<SkuSaleAttrValue> skuSaleAttrValueList= skuSaleAttrValueMapper.selectSkuSaleAttrValueListBySpu(Long.parseLong(spuId));
        return skuSaleAttrValueList;
    }

    @Override
    public List<BaseAttrInfo> getAttrList(List<String> valueIds) {
        String valueIdsStr = StringUtils.join(valueIds, ",");
        List<BaseAttrInfo> baseAttrInfoListByValueIds = baseAttrInfoMapper.getBaseAttrInfoListByValueIds(valueIdsStr);
        return baseAttrInfoListByValueIds;
    }


    @Override
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {

        //如果有主键就进行更新，如果没有就插入
        if (baseAttrInfo.getId() != null && baseAttrInfo.getId().length() > 0) {
            baseAttrInfoMapper.updateByPrimaryKey(baseAttrInfo);
        } else {
            //防止主键被赋上一个空字符串
            if (baseAttrInfo.getId().length() == 0) {
                baseAttrInfo.setId(null);
            }
            baseAttrInfoMapper.insertSelective(baseAttrInfo);
        }
        //把原属性值全部清空
        BaseAttrValue baseAttrValue4Del = new BaseAttrValue();
        baseAttrValue4Del.setAttrId(baseAttrInfo.getId());
        baseAttrValueMapper.delete(baseAttrValue4Del);

        //重新插入属性
        if (baseAttrInfo.getAttrValueList() != null && baseAttrInfo.getAttrValueList().size() > 0) {
            for (BaseAttrValue attrValue : baseAttrInfo.getAttrValueList()) {
                //防止主键被赋上一个空字符串
                if (attrValue.getId() != null && attrValue.getId().length() == 0) {
                    attrValue.setId(null);
                }
                attrValue.setAttrId(baseAttrInfo.getId());
                baseAttrValueMapper.insertSelective(attrValue);
            }
        }
    }

    @Override
    public BaseAttrInfo getAttrInfo(String id) {
        //查询属性基本信息
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectByPrimaryKey(id);

        //查询属性对应的属性值
        BaseAttrValue baseAttrValue4Query = new BaseAttrValue();
        baseAttrValue4Query.setAttrId(baseAttrInfo.getId());
        List<BaseAttrValue> baseAttrValueList = baseAttrValueMapper.select(baseAttrValue4Query);

        baseAttrInfo.setAttrValueList(baseAttrValueList);
        return baseAttrInfo;
    }


    @Override
    public List<SpuInfo> getSpuInfoList(SpuInfo spuInfo) {

        List<SpuInfo> spuInfoList = spuInfoMapper.select(spuInfo);
        return spuInfoList;
    }

    @Override
    public void saveSpuInfo(SpuInfo spuInfo) {
        //存入商品基本信息
        if (spuInfo.getId() == null || spuInfo.getId().length() == 0) {
            spuInfo.setId(null);
            spuInfoMapper.insertSelective(spuInfo);
        } else {
            spuInfoMapper.updateByPrimaryKey(spuInfo);
        }
//删除之前的数据
        Example spuImageExample = new Example(SpuImage.class);
        spuImageExample.createCriteria().andEqualTo("spuId", spuInfo.getId());
        spuImageMapper.deleteByExample(spuImageExample);
//存入商品图片
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if (spuImageList != null) {
            for (SpuImage spuImage : spuImageList) {
                if (spuImage.getId() != null && spuImage.getId().length() == 0) {
                    spuImage.setId(null);
                }
                spuImage.setSpuId(spuInfo.getId());
                spuImageMapper.insertSelective(spuImage);
            }
        }

        Example spuSaleAttrExample = new Example(SpuSaleAttr.class);
        spuSaleAttrExample.createCriteria().andEqualTo("spuId", spuInfo.getId());
        spuSaleAttrMapper.deleteByExample(spuSaleAttrExample);


        Example spuSaleAttrValueExample = new Example(SpuSaleAttrValue.class);
        spuSaleAttrValueExample.createCriteria().andEqualTo("spuId", spuInfo.getId());
        spuSaleAttrValueMapper.deleteByExample(spuSaleAttrValueExample);

        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if (spuSaleAttrList != null) {
            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
                if (spuSaleAttr.getId() != null && spuSaleAttr.getId().length() == 0) {
                    spuSaleAttr.setId(null);
                }
                spuSaleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insertSelective(spuSaleAttr);
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                    if (spuSaleAttrValue.getId() != null && spuSaleAttrValue.getId().length() == 0) {
                        spuSaleAttrValue.setId(null);
                    }
                    spuSaleAttrValue.setSpuId(spuInfo.getId());
                    spuSaleAttrValueMapper.insertSelective(spuSaleAttrValue);
                }
            }

        }
    }

    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        return baseSaleAttrMapper.selectAll();
    }

    @Override
    public void deleteAttrInfo(BaseAttrInfo baseAttrInfo) {
        baseAttrInfoMapper.deleteByPrimaryKey(baseAttrInfo);
        //把原属性值全部清空
        BaseAttrValue baseAttrValue4Del = new BaseAttrValue();
        baseAttrValue4Del.setAttrId(baseAttrInfo.getId());
        baseAttrValueMapper.delete(baseAttrValue4Del);
    }


    @Override
    public  List<SpuSaleAttr> getSpuSaleAttrList(String spuId){

        List<SpuSaleAttr> spuSaleAttrList = spuSaleAttrMapper.selectSpuSaleAttrList(Long.parseLong(spuId));
        return spuSaleAttrList;

    }

    @Override
    public List<SpuImage> getSpuImageList(String spuId){
        SpuImage spuImage=new SpuImage();
        spuImage.setSpuId(spuId);
        List<SpuImage> spuImageList = spuImageMapper.select(spuImage);
        return spuImageList;
    }

    /**
     * 删除一个spu商品(删除spuInfo表及其对应商品图片表、对应销售属性表、销售属性值表的数据)
     *
     * @param spuId
     */
    @Override
    public void deleteOneSpu(String spuId) {

        SpuInfo spuInfo=new SpuInfo();
        spuInfo.setId(spuId);
       spuInfoMapper.delete(spuInfo);

       //删除图片
        Example spuImageExample = new Example(SpuImage.class);
        spuImageExample.createCriteria().andEqualTo("spuId", spuInfo.getId());
        spuImageMapper.deleteByExample(spuImageExample);
        //删除销售属性
        Example spuSaleAttrExample = new Example(SpuSaleAttr.class);
        spuSaleAttrExample.createCriteria().andEqualTo("spuId", spuInfo.getId());
        spuSaleAttrMapper.deleteByExample(spuSaleAttrExample);

        //删除销售属性值
        Example spuSaleAttrValueExample = new Example(SpuSaleAttrValue.class);
        spuSaleAttrValueExample.createCriteria().andEqualTo("spuId", spuInfo.getId());
        spuSaleAttrValueMapper.deleteByExample(spuSaleAttrValueExample);

    }

}
