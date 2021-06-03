package org.jeecg.modules.oa.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSONObject;
import org.jeecg.common.api.IActBizService;
import org.jeecg.common.system.api.ISysBaseAPI;
import org.jeecg.modules.oa.entity.AppLeave;
import org.jeecg.modules.oa.mapper.AppLeaveMapper;
import org.jeecg.modules.oa.service.IAppLeaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * @Description: 请假
 * @Author: jeecg-boot
 * @Date:   2021-06-03
 * @Version: V1.0
 */
@Service
public class AppLeaveServiceImpl extends ServiceImpl<AppLeaveMapper, AppLeave> implements IAppLeaveService , IActBizService {
    @Autowired
    private ISysBaseAPI baseAPI;
    @Override
    public String ActSave(JSONObject req) {
        AppLeave appLeave = JSONObject.toJavaObject(req, AppLeave.class);
        saveOrUpdate(appLeave);
        return appLeave.getId();
    }

    @Override
    public JSONObject getFormById(String id) {
        final AppLeave appLeave = getById(id);
        return baseAPI.parseDict(appLeave);
    }
}
