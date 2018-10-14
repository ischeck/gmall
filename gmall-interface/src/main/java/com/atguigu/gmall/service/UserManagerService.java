package com.atguigu.gmall.service;




import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.UserInfo;

import java.util.List;

public interface UserManagerService {
    List<UserInfo> getUserInfoList(UserInfo userInfoQuery);

    UserInfo getUserInfo(UserInfo userInfoQuery);

    void delete(UserInfo userInfoQuery);

    void addUserInfo(UserInfo userInfo);

    void updateUserInfo(UserInfo userInfo);

    List<UserAddress> getUserAddressList(String userId);

    String sayOk();

    UserInfo login(UserInfo userInfo);

    boolean verify(String userId);
}
