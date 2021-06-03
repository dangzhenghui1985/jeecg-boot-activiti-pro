package org.jeecg.common.api;

import com.alibaba.fastjson.JSONObject;
import org.jeecg.common.api.vo.Result;

import javax.servlet.http.HttpServletRequest;

public interface IActBizService {
    public String ActSave(JSONObject req);
    public JSONObject getFormById(String id);
    public default Result preCheck(HttpServletRequest request){
        return Result.OK();
    }
    public default void finishCallBack(String id){};
    public default void startCallBack(String id){ };
    public default void taskCompletCallBack(JSONObject task,JSONObject actBusiness){}
}
