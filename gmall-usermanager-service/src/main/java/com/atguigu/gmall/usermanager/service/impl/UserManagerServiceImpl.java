package com.atguigu.gmall.usermanager.service.impl;


import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.usermanager.constant.UserConst;
import com.atguigu.gmall.usermanager.mapper.UserAddressMapper;
import com.atguigu.gmall.usermanager.mapper.UserInfoMapper;
import com.atguigu.gmall.service.UserManagerService;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;

import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Service
public class UserManagerServiceImpl implements UserManagerService {

    @Autowired
    RedisUtil redisUtil;
    @Autowired
    private UserInfoMapper userInfoMapper;
    @Autowired
    private UserAddressMapper userAddressMapper;
    //查询所有


    public List<UserInfo> getUserInfoList(UserInfo userInfoQuery){
        List<UserInfo> userInfos=null;
        //查询所有
        //userInfos = userInfoMapper.selectAll();
        //条件匹配查询
        //userInfos =userInfoMapper.select(userInfoQuery);
        //特殊条件匹配查询 比如：按姓氏匹配
        Example example=new Example(UserInfo.class);
        example.createCriteria().andLike("loginName","%"+userInfoQuery.getLoginName()+"%");
        userInfos = userInfoMapper.selectByExample(example);
        return userInfos;
    }

    //查询单表
    public UserInfo getUserInfo(UserInfo userInfoQuery){
        UserInfo userInfo=null;
        //按主键查找
         userInfo = userInfoMapper.selectByPrimaryKey(userInfoQuery.getId());

        //按所有非空值查询   必须只有一行 否则报错
        //userInfo = userInfoMapper.selectOne(userInfoQuery );
        return userInfo;
    }

    //增加用户
    public void addUserInfo(UserInfo userInfo){
     /*   //会覆盖数据默认值
        userInfoMapper.insert(userInfo);*/

        String md5Password=DigestUtils.md5Hex(userInfo.getPasswd());
        userInfo.setPasswd(md5Password);
        // 不会覆盖数据库默认值
        userInfoMapper.insertSelective(userInfo);
    }


    public void updateUserInfo(UserInfo userInfo){
        //修改用户  依靠主键去查询 ，然后更新其他值，如果某个值为空，那么原值被清空
        //       userInfoMapper.updateByPrimaryKey(userInfo);
        //修改用户  依靠主键去查询 ，然后更新其他不为空的值.
        //        userInfoMapper.updateByPrimaryKeySelective(userInfo);

        //修改用户  依靠自定义条件去修改
        Example example=new Example(UserInfo.class);
        example.createCriteria().andLike("loginName","%"+userInfo.getLoginName()+"%");
        userInfo.setLoginName(null);
//        userInfoMapper.updateByExample( userInfo,example );
        userInfoMapper.updateByExampleSelective( userInfo,example );
        //
    }

    public void delete(UserInfo userInfoQuery){
        userInfoMapper.deleteByPrimaryKey(userInfoQuery.getId());
        //按非空值匹配删除
        //  userInfoMapper.delete(userInfoQuery);
        //按条件匹配删除
        //  userInfoMapper.deleteByExample(new Example(UserInfo.class));
    }



    public List<UserAddress> getUserAddressList(String userId) {
        List<UserAddress> addressList = null;
        UserAddress userAddress = new UserAddress();
        userAddress.setUserId(userId);
        addressList = userAddressMapper.select(userAddress);
        return addressList;
    }


    public String sayOk() {
        return "ok";
    }


    /**
     * 验证登录，若用户存在做redis缓存
     * @param userInfo
     * @return
     */
    public UserInfo login(UserInfo userInfo){
        String passwd = DigestUtils.md5Hex(userInfo.getPasswd());
        userInfo.setPasswd(passwd);

        UserInfo userInfoResult = userInfoMapper.selectOne(userInfo);
        if(userInfoResult!=null){

            String userInfoKey=UserConst.LOGIN_PREFIX+userInfoResult.getId()+UserConst.LOGIN_SUFFIX;

            Jedis jedis = redisUtil.getJedis();
            String userInfoJson = JSON.toJSONString(userInfoResult);
            jedis.setex(userInfoKey, UserConst.SESSIONEXPIRE,userInfoJson);

            return userInfoResult ;
        }else{
            return null;
        }
    }


    public boolean verify(String userId){
        Jedis jedis = redisUtil.getJedis();
        String userInfoKey=UserConst.LOGIN_PREFIX+userId+UserConst.LOGIN_SUFFIX;
        boolean exists = jedis.exists(userInfoKey);

        if(exists) {//若存在延长有效期，否则登录过期，重新登录
            jedis.expire(userInfoKey, UserConst.SESSIONEXPIRE);
        }
        jedis.close();
        return exists;
    }



}
