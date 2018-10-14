package com.atguigu.gmall.manager.mapper;

import com.atguigu.gmall.bean.BaseAttrInfo;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface BaseAttrInfoMapper extends Mapper<BaseAttrInfo> {
    List<BaseAttrInfo> selectAttrInfoList(long l);
    List<BaseAttrInfo> getBaseAttrInfoListByValueIds(@Param("valueIds") String valueIds);
}
