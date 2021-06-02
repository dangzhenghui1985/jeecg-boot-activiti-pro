package org.jeecg.modules.activiti.web;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.*;
import org.activiti.editor.constants.ModelDataJsonConstants;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.Model;
import org.activiti.engine.repository.ProcessDefinition;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.activiti.entity.ActNode;
import org.jeecg.modules.activiti.entity.ActZprocess;
import org.jeecg.modules.activiti.entity.ProcessNodeVo;
import org.jeecg.modules.activiti.service.Impl.ActBusinessServiceImpl;
import org.jeecg.modules.activiti.service.Impl.ActNodeServiceImpl;
import org.jeecg.modules.activiti.service.Impl.ActZprocessServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/activiti_process")
@Slf4j
@Transactional
@Api(tags = "流程")
public class ActivitiProcessController {
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ActZprocessServiceImpl actZprocessService;
    @Autowired
    private ActNodeServiceImpl actNodeService;
    @Autowired
    private ActBusinessServiceImpl actBusinessService;

    @AutoLog(value = "流程-获取可用流程")
    @ApiOperation(value="流程-获取可用流程", notes="获取可用流程")
    @RequestMapping(value = "/listData" ,method = {RequestMethod.POST,RequestMethod.GET})
    public Result listData(@ApiParam(value = "流程名称" )String lcmc,
                           @ApiParam(value = "流程key" )String lckey,
                           @ApiParam(value = "是否最新" )Boolean zx,
                           @ApiParam(value = "流程状态 部署后默认1激活" )String status,
                           @ApiParam(value = "如果此项不为空，则会过滤当前用户的角色权限" )Boolean roles,
                           HttpServletRequest request){
        log.info("-------------流程列表-------------");
        LambdaQueryWrapper<ActZprocess> wrapper = new LambdaQueryWrapper<ActZprocess>();
        wrapper.orderByAsc(ActZprocess::getSort).orderByDesc(ActZprocess::getVersion);
        if (StrUtil.isNotBlank(lcmc)){
            wrapper.like(ActZprocess::getName, lcmc);
        }
        if (StrUtil.isNotBlank(lckey)){
            wrapper.like(ActZprocess::getProcessKey, lckey);
        }
        if (zx!=null&&zx){
            wrapper.eq(ActZprocess::getLatest, 1);
        }
        if (StrUtil.isNotBlank(status)){
            wrapper.eq(ActZprocess::getStatus, status);
        }
        String statuss = request.getParameter("statuss");
        if (StrUtil.isNotBlank(statuss)){
            wrapper.in(ActZprocess::getStatus, statuss);
        }
        List<ActZprocess> list = actZprocessService.list(wrapper);
        if (roles!=null&&roles){ //过滤角色
            LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
            List<String> roleByUserName = actNodeService.getRoleByUserName(sysUser.getUsername());
            list = list.stream().filter(p->{
                String pRoles = p.getRoles();
                if (StrUtil.isBlank(pRoles)) {
                    return true; //未设置授权的所有人都能用
                }else {
                    String[] split = pRoles.split(",");
                    for (String role : split) {
                        if (roleByUserName.contains(role)){
                            return true;
                        }
                    }
                }
                return false;
            }).collect(Collectors.toList());

        }
        return Result.ok(list);
    }

    /*激活或挂起流程定义*/
    @RequestMapping(value = "/updateStatus")
    public Result updateStatus( String id, Integer status){

        ActZprocess actProcess = actZprocessService.getById(id);
        if(status==1){
            //启动前检查一下 路由等信息是否齐全
            String routeName = actProcess.getRouteName();
            String businessTable = actProcess.getBusinessTable();
            if (StrUtil.isBlank(routeName)||StrUtil.isBlank(businessTable)){
                return Result.error("未设置关联表单，点击编辑设置。");
            }

            repositoryService.activateProcessDefinitionById(id, true, new Date());
        }else {
            repositoryService.suspendProcessDefinitionById(id, true, new Date());
        }
        actProcess.setStatus(status);
        actZprocessService.updateById(actProcess);
        return Result.ok("修改成功！");
    }
    /*通过id删除流程*/
    @RequestMapping(value = "/delByIds")
    public Result<Object> delByIds(String ids){
        for(String id : ids.split(",")){
            if(CollectionUtil.isNotEmpty(actBusinessService.findByProcDefId(id))){
                return Result.error("包含已发起申请的流程，无法删除");
            }
            ActZprocess actProcess = actZprocessService.getById(id);
            // 当删除最后一个版本时 删除关联数据
            if (actProcess==null) return Result.error("该数据已删除！");
            if(actProcess.getVersion()==1){
                deleteNodeUsers(id);
            }
            // 级联删除
            repositoryService.deleteDeployment(actProcess.getDeploymentId(), true);
            actZprocessService.removeById(id);
            // 更新最新版本
            actZprocessService.setLatestByProcessKey(actProcess.getProcessKey());
        }
        return Result.ok("删除成功");
    }
    public void deleteNodeUsers(String processId){

        BpmnModel bpmnModel = repositoryService.getBpmnModel(processId);
        List<Process> processes = bpmnModel.getProcesses();
        for(Process process : processes){
            Collection<FlowElement> elements = process.getFlowElements();
            for(FlowElement element : elements) {
                actNodeService.deleteByNodeId(element.getId(),processId);
            }
        }
    }
    /**
     * 转化流程为模型
     * @param id
     * @return
     */
    @RequestMapping(value = "/convertToModel")
    public Result convertToModel( String id){

        ProcessDefinition pd = repositoryService.createProcessDefinitionQuery().processDefinitionId(id).singleResult();
        InputStream bpmnStream = repositoryService.getResourceAsStream(pd.getDeploymentId(), pd.getResourceName());
        ActZprocess actProcess = actZprocessService.getById(id);

        try {
            XMLInputFactory xif = XMLInputFactory.newInstance();
            InputStreamReader in = new InputStreamReader(bpmnStream, "UTF-8");
            XMLStreamReader xtr = xif.createXMLStreamReader(in);
            BpmnModel bpmnModel = new BpmnXMLConverter().convertToBpmnModel(xtr);
            BpmnJsonConverter converter = new BpmnJsonConverter();

            ObjectNode modelNode = converter.convertToJson(bpmnModel);
            Model modelData = repositoryService.newModel();
            modelData.setKey(pd.getKey());
            modelData.setName(pd.getResourceName());

            ObjectNode modelObjectNode = new ObjectMapper().createObjectNode();
            modelObjectNode.put(ModelDataJsonConstants.MODEL_NAME, actProcess.getName());
            modelObjectNode.put(ModelDataJsonConstants.MODEL_REVISION, modelData.getVersion());
            modelObjectNode.put(ModelDataJsonConstants.MODEL_DESCRIPTION, actProcess.getDescription());
            modelData.setMetaInfo(modelObjectNode.toString());

            repositoryService.saveModel(modelData);
            repositoryService.addModelEditorSource(modelData.getId(), modelNode.toString().getBytes("utf-8"));

        }catch (Exception e){
            log.error(e.getMessage(),e);
            return Result.error("转化流程为模型失败");
        }
        return Result.ok("修改成功");
    }
    @RequestMapping(value = "/updateInfo", method = RequestMethod.POST)
    public Result<Object> updateInfo(ActZprocess actProcess){

        ProcessDefinition pd = repositoryService.getProcessDefinition(actProcess.getId());
        if(pd==null){
            return Result.error("流程定义不存在");
        }
        if(StrUtil.isNotBlank(actProcess.getCategoryId())){
            repositoryService.setProcessDefinitionCategory(actProcess.getId(), actProcess.getCategoryId());
            repositoryService.setDeploymentCategory(pd.getDeploymentId(), actProcess.getCategoryId());
        }
        actZprocessService.updateById(actProcess);
        return Result.ok("修改成功");
    }
    /*通过流程定义id获取流程节点*/
    @RequestMapping(value = "/getProcessNode")
    public Result getProcessNode(String id){

        BpmnModel bpmnModel = repositoryService.getBpmnModel(id);

        List<ProcessNodeVo> list = new ArrayList<>();

        List<Process> processes = bpmnModel.getProcesses();
        if(processes==null||processes.size()==0){
            return Result.ok();
        }
        for(Process process : processes){
            Collection<FlowElement> elements = process.getFlowElements();
            for(FlowElement element : elements){
                ProcessNodeVo node = new ProcessNodeVo();
                node.setProcDefId(id);
                node.setId(element.getId());
                node.setTitle(element.getName());
                if(element instanceof StartEvent){
                    // 开始节点
                    node.setType(0);
                }else if(element instanceof UserTask){
                    // 用户任务
                    node.setType(1);
                    // 设置关联用户
                    node.setUsers(actNodeService.findUserByNodeId(element.getId(),id));
                    // 设置关联角色
                    node.setRoles(actNodeService.findRoleByNodeId(element.getId(),id));
                    // 设置关联部门
                    node.setDepartments(actNodeService.findDepartmentByNodeId(element.getId(),id));
                    // 设置关联部门负责人
                    node.setDepartmentManages(actNodeService.findDepartmentManageByNodeId(element.getId(),id));
                    // 是否设置发起人部门负责人
                    node.setChooseDepHeader(actNodeService.hasChooseDepHeader(element.getId(),id));
                    // 是否设置发起人
                    node.setChooseSponsor(actNodeService.hasChooseSponsor(element.getId(),id));

                    // 设置表单变量
                    String variable = "";
                    List<String> formVariables = actNodeService.findFormVariableByNodeId(element.getId(),id);
                    for (String formVariable : formVariables) {
                        variable = variable + formVariable + ",";
                    }
                    node.setFormVariables(variable.length()>0?variable.substring(0,variable.length()-1):variable);
                }else if(element instanceof EndEvent){
                    // 结束
                    node.setType(2);
                }else{
                    // 排除其他连线或节点
                    continue;
                }
                list.add(node);
            }
        }
        list.sort(Comparator.comparing(ProcessNodeVo::getType));
        return Result.ok(list);
    }

    /**
     * 编辑节点分配用户
     * @param nodeId
     * @param userIds
     * @param roleIds
     * @param departmentIds
     * @param chooseDepHeader 是否勾选操作人的部门负责人
     * @return
     */
    @RequestMapping(value = "/editNodeUser", method = RequestMethod.POST)
    public Result editNodeUser(String nodeId,String procDefId, String userIds, String roleIds, String departmentIds,String departmentManageIds,String formVariables, Boolean chooseDepHeader, Boolean chooseSponsor){

        // 删除其关联权限
        actNodeService.deleteByNodeId(nodeId,procDefId);
        // 分配新用户
        for(String userId : userIds.split(",")){
            ActNode actNode = new ActNode();
            actNode.setProcDefId(procDefId);
            actNode.setNodeId(nodeId);
            actNode.setRelateId(userId);
            actNode.setType(1);
            actNodeService.save(actNode);
        }
        // 分配新角色
        for(String roleId : roleIds.split(",")){
            ActNode actNode = new ActNode();
            actNode.setProcDefId(procDefId);
            actNode.setNodeId(nodeId);
            actNode.setRelateId(roleId);
            actNode.setType(0);
            actNodeService.save(actNode);
        }
        // 分配新部门
        for(String departmentId : departmentIds.split(",")){
            ActNode actNode = new ActNode();
            actNode.setProcDefId(procDefId);
            actNode.setNodeId(nodeId);
            actNode.setRelateId(departmentId);
            actNode.setType(2);
            actNodeService.save(actNode);
        }
        // 分配新部门负责人
        for(String departmentId : departmentManageIds.split(",")){
            ActNode actNode = new ActNode();
            actNode.setProcDefId(procDefId);
            actNode.setNodeId(nodeId);
            actNode.setRelateId(departmentId);
            actNode.setType(5);
            actNodeService.save(actNode);
        }

        // 表单变量
        for(String formVariable : formVariables.split(",")){
            ActNode actNode = new ActNode();
            actNode.setProcDefId(procDefId);
            actNode.setNodeId(nodeId);
            actNode.setRelateId(formVariable);
            actNode.setType(6);
            actNodeService.save(actNode);
        }

        if(chooseDepHeader!=null&&chooseDepHeader){
            ActNode actNode = new ActNode();
            actNode.setProcDefId(procDefId);
            actNode.setNodeId(nodeId);
            actNode.setType(4);
            actNodeService.save(actNode);
        }
        if(chooseSponsor!=null&&chooseSponsor){
            ActNode actNode = new ActNode();
            actNode.setProcDefId(procDefId);
            actNode.setNodeId(nodeId);
            actNode.setType(3);
            actNodeService.save(actNode);
        }
        return Result.ok("操作成功");
    }
    @RequestMapping(value = "/getNextNode", method = RequestMethod.GET)
    @ApiOperation(value = "通过当前节点定义id获取下一个节点")
    public Result getNextNode(@ApiParam("流程定义id")  String procDefId,
                              @ApiParam("当前节点定义id")  String currActId,
                              @ApiParam("当前节点定义id")  String procInstId){
        ProcessNodeVo node = actZprocessService.getNextNode(procDefId, currActId,procInstId);
        return Result.ok(node);
    }
    @RequestMapping(value = "/getNode", method = RequestMethod.GET)
    @ApiOperation(value = "通过节点nodeId获取审批人")
    public Result getNode(@ApiParam("节点nodeId") @RequestParam("nodeId") String nodeId,
                          @ApiParam("表单名称") @RequestParam("tableName") String tableName,
                          @ApiParam("表单id") @RequestParam("tableId") String tableId){
        ProcessNodeVo node = actZprocessService.getNode(nodeId,tableName,tableId);
        return Result.ok(node);
    }
    @RequestMapping(value = "/queryNewestProcess", method = RequestMethod.GET)
    @ApiOperation(value = "获取最新部署的流程定义")
    public Result queryNewestProcess(@ApiParam("流程定义key") @RequestParam(value = "processKey",defaultValue = "") String processKey){
        List<ActZprocess> actZprocesses = actZprocessService.queryNewestProcess(processKey);
        return Result.ok(actZprocesses);
    }

}
