package org.jeecg.modules.activiti.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.activiti.entity.ActNode;
import org.jeecg.modules.activiti.entity.Department;
import org.jeecg.modules.activiti.entity.Role;

import java.util.List;

/**
 * @Description: 流程节点扩展表
 * @Author: jeecg-boot
 * @Date:   2020-03-30
 * @Version: V1.0
 */
public interface ActNodeMapper extends BaseMapper<ActNode> {

    List<LoginUser> findUserByNodeId(@Param("nodeId") String nodeId,@Param("procDefId") String procDefId);

    List<Role> findRoleByNodeId(@Param("nodeId") String nodeId,@Param("procDefId") String procDefId);

    List<Department> findDepartmentByNodeId(@Param("nodeId") String nodeId,@Param("procDefId") String procDefId);

    List<Department> findDepartmentManageByNodeId(@Param("nodeId") String nodeId,@Param("procDefId") String procDefId);

    List<String> findFormVariableByNodeId(@Param("nodeId") String nodeId,@Param("procDefId") String procDefId);

    @Select("select role_code from sys_role where id in (select role_id from sys_user_role where user_id = (select id from sys_user where username=#{username}))")
    List<String> getRoleByUserName(@Param("username") String username);
    @Select("select * from sys_user")
    List<LoginUser> queryAllUser();
    @Select("select * from sys_user where id in (select user_id from sys_user_role where role_id = #{id})")
    List<LoginUser> findUserByRoleId(@Param("id") String id);
    @Select("select * from sys_user where id in (select user_id from sys_user_depart where dep_id = #{id})")
    List<LoginUser> findUserDepartmentId(@Param("id") String id);
    @Select("select * from sys_user where FIND_IN_SET(#{id},depart_ids)")
    List<LoginUser> findUserDepartmentManageId(@Param("id") String id);
    @Select("select * from act_z_node where node_id = #{nodeId}")
    List<ActNode> findByNodeId(@Param("nodeId") String nodeId);

}
