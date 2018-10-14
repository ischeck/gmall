package com.atguigu.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.cart.constant.CartConst;
import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.*;


@Service
public class CartServiceImpl implements CartService {

    @Autowired
    CartInfoMapper cartInfoMapper;

    @Autowired
    RedisUtil redisUtil;

    public void addToCart(String userId, SkuInfo skuInfo, Integer skuNum){

        String userCartKey= CartConst.CART_PREFIX+userId+CartConst.CART_SUFFIX;
        //插入前先检查
        CartInfo cartInfoQuery=new CartInfo();
        cartInfoQuery.setSkuId(skuInfo.getId());
        cartInfoQuery.setUserId(userId);
        CartInfo cartInfoExist = cartInfoMapper.selectOne(cartInfoQuery);
        if(cartInfoExist!=null) {
            cartInfoExist.setSkuPrice(skuInfo.getPrice());
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum() + skuNum);
            cartInfoMapper.updateByPrimaryKeySelective(cartInfoExist);

        }else{
            //插入数据库
            CartInfo cartInfo=new CartInfo();
            cartInfo.setSkuId(skuInfo.getId());
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setUserId(userId);
            cartInfo.setSkuNum(skuNum);
            cartInfo.setSkuPrice(skuInfo.getPrice());
            cartInfoMapper.insertSelective(cartInfo);

        }

            //更新缓存
            Jedis jedis = redisUtil.getJedis();
            jedis.hset(userCartKey,skuInfo.getId(), JSON.toJSONString(cartInfoExist) );
           //改过期时间 使订单缓存的过期时间同 用户信息缓存的过期时间一致
            Long ttl = jedis.ttl(CartConst.USER_PREFIX + userId + CartConst.USER_SUFFIX);
            jedis.expire(userCartKey, ttl.intValue());
            jedis.close();

    }


    public List<CartInfo> getCartList(String userId){
        //优先从缓存中取值
        Jedis jedis = redisUtil.getJedis();
        List<String> skuJsonlist = jedis.hvals(CartConst.CART_PREFIX+ userId + CartConst.CART_SUFFIX);
        List<CartInfo> cartInfoList=new ArrayList<>();

        if(skuJsonlist!=null &&skuJsonlist.size()!=0){
            //序列化
            for (String skuJson : skuJsonlist) {
                CartInfo cartInfo = JSON.parseObject(skuJson, CartInfo.class);
                cartInfoList.add( cartInfo);
            }
            //缓存中的值取出来是没有序的 用id进行排序
            cartInfoList.sort(new Comparator<CartInfo>() {
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    return Long.compare(Long.parseLong(o2.getId()) ,Long.parseLong(o1.getId())) ;
                }
            });
            return cartInfoList;
        }else{
            //如果缓存没有就总数据库中加载
            cartInfoList = loadCartCache(  userId);
            return cartInfoList;
        }

    }


    public List<CartInfo> mergeToCart(String userId ,List<CartInfo> cartInfoCookieList){
        //查询用户名下的购物车清单
        CartInfo cartInfoQuery=new CartInfo();
        cartInfoQuery.setUserId(userId);
        List<CartInfo> cartInfoExistList = cartInfoMapper.select(cartInfoQuery);
        for (CartInfo cartInfo : cartInfoCookieList) {
            boolean isExist=false;
            for (CartInfo cartInfoExist : cartInfoExistList) {
                if( cartInfo.getSkuId().equals(cartInfoExist.getSkuId())){
                    cartInfoExist.setSkuNum(cartInfoExist.getSkuNum()+cartInfo.getSkuNum());
                    cartInfoMapper.updateByPrimaryKey(cartInfoExist);
                    isExist=true;
                    break;
                }
            }
            if(!isExist){
                cartInfo.setUserId(userId);
                cartInfoMapper.insertSelective(cartInfo);
            }

        }

        for (CartInfo cartInfo : cartInfoCookieList) {
            if(cartInfo.getIsChecked().equals("1")){
                setCheckedCart( userId,  cartInfo.getSkuId(), "1");
            }
        }

        List<CartInfo> newCartInfoList = loadCartCache(userId);
        return newCartInfoList;
    }


    //更新缓存
    public List<CartInfo> loadCartCache(String userId){
        //携带最新商品价格
        List<CartInfo> cartlist = cartInfoMapper.selectCartListWithCurPrice(Long.parseLong(userId));
        if(cartlist==null||cartlist.size()==0){
            return null;
        }
        Jedis jedis = redisUtil.getJedis();
        String userCartKey=CartConst.CART_PREFIX+userId+CartConst.CART_SUFFIX;
        String userInfoKey=CartConst.USER_PREFIX+userId+CartConst.USER_SUFFIX;
        Map cartMap =new HashMap(cartlist.size());
        for (CartInfo cartInfo : cartlist) {
            cartMap.put(cartInfo.getSkuId(),JSON.toJSONString(cartInfo));
        }
        jedis.hmset(userCartKey,cartMap);
        Long ttl = jedis.ttl(userInfoKey);
        jedis.expire(userCartKey, ttl.intValue());
        return cartlist;
    }

    public void setCheckedCart(String userId,String skuId,String isChecked){
        Jedis jedis = redisUtil.getJedis();
        String userCartKey=CartConst.CART_PREFIX+userId+CartConst.CART_SUFFIX;
        String cartInfoJson = jedis.hget(userCartKey, skuId);
        CartInfo cartInfo = JSON.parseObject(cartInfoJson, CartInfo.class);
        cartInfo.setIsChecked(isChecked);
        String newCartInfoJson=JSON.toJSONString(cartInfo);
        jedis.hset(userCartKey, skuId,newCartInfoJson);

        String cartCheckedKey=CartConst.USER_CHECKED_PREFIX+userId+CartConst.USER_CHECKED_SUFFIX;
        if(isChecked.equals("1")){
            jedis.hset(cartCheckedKey,skuId,newCartInfoJson);
            jedis.expire(cartCheckedKey, jedis.ttl(userCartKey).intValue());
        }else{
            jedis.hdel(cartCheckedKey,skuId );
        }

    }

    public List<CartInfo> getCartCheckedList(String userId){
        String userCheckedKey= CartConst.USER_CHECKED_PREFIX +userId+CartConst.USER_CHECKED_SUFFIX;
        Jedis jedis = redisUtil.getJedis();
        List<String> cartCheckedJsonList = jedis.hvals(userCheckedKey);
        List<CartInfo> cartCheckedList=new ArrayList<>(cartCheckedJsonList.size());
        for (String cartJson : cartCheckedJsonList) {
            CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
            cartCheckedList.add(cartInfo);
        }
        return cartCheckedList;

    }


}
