<template>
  <a-card :bordered="false">
    
      <a-empty description="无流程可供选择" v-if="activeKeyAll.length==0" />
      <div v-else>
        <a-input-search style="margin-bottom: 10px;width: 200px"
                        placeholder="输入流程名称" @search="onSearchProcess" />
        <a-collapse v-model="activeKey">
          <a-collapse-panel v-for="(value, index)  in activeKeyAll" :header="filterDictText(dictOptions,value)||'未分类'" :key="value">
            <a-list :grid="{ gutter: 10,column:1}" :dataSource="processDataMap[value]">
              <a-list-item slot="renderItem" slot-scope="item">
                <a-card>
                  <div slot="title">
                    <a-row>
                      <a-col span="12" :title="item.name">{{item.name}} </a-col>
                      <a-col span="12" style="text-align: right;">
                        <a href="javascript:void (0)" @click="chooseProcess(item)">发起申请</a>
                      </a-col>
                    </a-row>
                  </div>
                  <b>版本：</b>v.{{item.version}}
                  <br/>
                  <b>说明：</b>{{item.description}}
                </a-card>
              </a-list-item>
            </a-list>
          </a-collapse-panel>
        </a-collapse>
      </div>

    <!--流程表单-->
    <a-modal :title="lcModa.title" v-model="lcModa.visible" :footer="null" :maskClosable="false" width="80%">
      <component :disabled="lcModa.disabled" v-if="lcModa.visible" :is="lcModa.formComponent"
                 :processData="lcModa.processData" :isNew = "lcModa.isNew"
                 @afterSubmit="afterSub" @close="lcModa.visible=false,lcModa.disabled = false"></component>
    </a-modal>
    <!--提交申请表单-->
    <a-modal title="提交申请" v-model="modalVisible" :mask-closable="false" :width="500" :footer="null">
      <div v-if="modalVisible">
        <a-form-item label="选择审批人" v-show="showAssign">
          <a-select style="width: 100%"
            v-model="form.assignees"
            placeholder="请选择"
            mode="multiple"
            :allowClear="true"
          >
            <a-select-option v-for="(item, i) in assigneeList" :key="i" :value="item.username">{{item.realname}}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="下一审批人" v-show="isGateway">
          <a-alert  type="info" showIcon message="分支网关处不支持自定义选择下一审批人，将自动下发给所有可审批人。">，将发送给下一节点所有人</a-alert>
        </a-form-item>
        <a-form-item label="优先级" prop="priority">
          <a-select v-model="form.priority" placeholder="请选择" :allowClear="true" style="width: 100%">
            <a-select-option :value="0">普通</a-select-option>
            <a-select-option :value="1">重要</a-select-option>
            <a-select-option :value="2">紧急</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="消息通知">
          <a-checkbox v-model="form.sendMessage">站内消息通知</a-checkbox>
          <a-checkbox v-model="form.sendSms" disabled>短信通知</a-checkbox>
          <a-checkbox v-model="form.sendEmail" disabled>邮件通知</a-checkbox>
        </a-form-item>
        <div slot="footer">
          <a-button type="text" @click="modalVisible=false">取消</a-button>
          <div style="display:inline-block;width: 20px;"></div>
          <a-button type="primary" :disabled="submitLoading" @click="applySubmit">提交</a-button>
        </div>
      </div>
    </a-modal>
    <a-modal title="审批历史" v-model="modalLsVisible" :mask-closable="false" :width="'80%'" :footer="null">
      <div v-if="modalLsVisible">
        <historicDetail :procInstId="procInstId"></historicDetail>
      </div>
    </a-modal>
    <a-modal title="确认撤回" v-model="modalCancelVisible" :mask-closable="false" :width="500">
      <a-form ref="delForm" v-model="cancelForm" :label-width="70" v-if="modalCancelVisible">
        <a-form-item label="撤回原因" prop="reason">
          <a-input type="textarea" v-model="cancelForm.reason" :rows="4" />
        </a-form-item>
      </a-form>
      <div slot="footer">
        <a-button type="text" @click="modalCancelVisible=false">取消</a-button>
        <a-button type="primary" :disabled="submitLoading" @click="handelSubmitCancel">提交</a-button>
      </div>
    </a-modal>
  </a-card>

</template>

<script>
  import { JeecgListMixin } from '@/mixins/JeecgListMixin'
  import { activitiMixin } from '@/views/activiti/mixins/activitiMixin'
  import { filterObj } from '@/utils/util';
  import JEllipsis from '@/components/jeecg/JEllipsis'
  import { deleteAction, getAction,downFile } from '@/api/manage'
  import pick from "lodash.pick";
  import JTreeSelect from '@/components/jeecg/JTreeSelect'
  import {initDictOptions, filterDictText} from '@/components/dict/JDictSelectUtil'
  import historicDetail from '@/views/activiti/historicDetail'
  export default {
    name: "applyList",
    mixins:[activitiMixin,JeecgListMixin],
    components: {
      JEllipsis
      ,JTreeSelect
      ,historicDetail
    },
    data () {
      return {
        description: '我的申请',
        dictOptions:[],
        url: {
          list: "/actBusiness/listData",
          getProcessDataList: "/activiti_process/listData",
          delByIds:'/actBusiness/delByIds',
          getFirstNode:'/actProcessIns/getFirstNode',
          applyBusiness:'/actBusiness/apply',
          cancelApply:'/actBusiness/cancel',
        },
        // 查询条件
        queryParam: {
          createTimeRange:[],
          keyWord:'',
        },
        // 表头
        labelCol: {
          xs: { span: 4 },
          sm: { span: 4 },
        },
        wrapperCol: {
          xs: { span: 20 },
          sm: { span: 20 },
        },
        processModalVisible: null,
        activeKeyAll: [],
        activeKey: [],
        processDataMap: {},
        searchProcessKey: null,
        addApplyLoading: false,
        lcModa: {
          title:'',
          disabled:false,
          visible:false,
          formComponent : null,
          isNew : false
        },
        form:{
          priority:0,
          assignees:[],
          sendMessage:true
        },
        modalVisible: false,
        showAssign: false,
        assigneeList: [],
        isGateway: false,
        dictPriority: [],
        submitLoading: false,
        error: "",
        /*审批历史*/
        modalLsVisible: false,
        procInstId: '',
        modalCancelVisible: false,
        cancelForm: {}
      }
    },
    computed:{
    },
    methods: {

      initDictConfig() {
        //初始化字典 - 流程分类
        initDictOptions('bpm_process_type').then((res) => {
          if (res.success) {
            this.dictOptions = res.result;
          }
        });
      },
      filterDictText(dictOptions, text) {
        if (dictOptions instanceof Array) {
          for (let dictItem of dictOptions) {
            if (text === dictItem.value) {
              return dictItem.text
            }
          }
        }
        return text||text=='null'?'':text
      },
      getProcessList() {
        this.addApplyLoading = true;
        this.postFormAction(this.url.getProcessDataList,{status:1,roles:true}).then(res => {
          this.activeKeyAll = [];
          if (res.success) {
            var result = res.result||[];
            if (result.length>0){
              let searchProcessKey = this.searchProcessKey;
              if (searchProcessKey){ //过滤条件
                result = _.filter(result, function(o) { return o.name.indexOf(searchProcessKey)>-1; });
              }
              this.processDataMap = _.groupBy(result,'categoryId');
              for (const categoryId in this.processDataMap) {
                this.activeKeyAll.push(categoryId)
              }
              this.activeKey = this.activeKeyAll;
            }
            this.processModalVisible = true;
          }else {
            this.$message.warning(res.message)
          }
        }).finally(()=>this.addApplyLoading = false);
      },
      loadData(arg) {
        console.log("loadData")
        if(!this.url.list){
          this.$message.error("请设置url.list属性!")
          return
        }
        //加载数据 若传入参数1则加载第一页的内容
        if (arg === 1) {
          this.ipagination.current = 1;
        }
        var params = this.getQueryParams();//查询条件
        this.loading = true;
        getAction(this.url.list, params).then((res) => {
          if (res.success) {
            let records = res.result||[];
            this.dataSource = records;
            this.ipagination.total = records.length;
          }
          if(res.code===510){
            this.$message.warning(res.message)
          }
          this.loading = false;
        })
      },
      getQueryParams(){
        var param = Object.assign({}, this.queryParam,this.isorter);
        delete param.createTimeRange; // 时间参数不传递后台
        return filterObj(param);
      },

      // 重置
      searchReset(){
        var that = this;
        var logType = that.queryParam.logType;
        that.queryParam = {}; //清空查询区域参数
        that.queryParam.logType = logType;
        that.loadData(this.ipagination.current);
      },
      onDateChange: function (value, dateString) {
        console.log(dateString[0],dateString[1]);
        this.queryParam.createTime_begin=dateString[0];
        this.queryParam.createTime_end=dateString[1];
      },
      onDateOk(value) {
        console.log(value);
      },

      getStatus(status) {
        let text = "未知", color = "";
        if (status == 0) {
          text = "草稿";
          color = "default";
        } else if (status == 1) {
          text = "处理中";
          color = "orange";
        } else if (status == 2) {
          text = "已结束";
          color = "blue";
        } else if (status == 3) {
          text = "已撤回";
          color = "magenta";
        }
        return {text:text,color:color}
      },
      getResult(result) {
        let text = "未知",
          color = "";
        if (result == 0) {
          text = "未提交";
          color = "default";
        } else if (result == 1) {
          text = "处理中";
          color = "orange";
        } else if (result == 2) {
          text = "已通过";
          color = "green";
        } else if (result == 3) {
          text = "已驳回";
          color = "red";
        }
        return {text:text,color:color}
      },
      apply(v) {
        if (!v.procDefId || v.procDefId == "null") {
          this.$message.error("流程定义为空");
          return;
        }
        this.form.id = v.id;
        this.form.procDefId = v.procDefId;
        this.form.title = v.title;
        // 加载审批人
        this.getAction(this.url.getFirstNode,{procDefId:v.procDefId}).then(res => {
          if (res.success) {
            if (res.result.type == 3 || res.result.type == 4) {
              this.isGateway = true;
              this.modalVisible = true;
              this.form.firstGateway = true;
              this.showAssign = false;
              this.error = "";
              return;
            }
            this.form.firstGateway = false;
            this.isGateway = false;
            if (res.result.users && res.result.users.length > 0) {
              this.error = "";
              this.assigneeList = res.result.users;
              // 默认勾选
              let ids = [];
              res.result.users.forEach(e => {
                ids.push(e.username);
              });
              this.form.assignees = ids;
              this.showAssign = true;
            } else {
              this.form.assignees = [];
              this.showAssign = true;
              this.error = '审批节点未分配候选审批人员，请联系管理员！';
            }
            if (this.error){
              this.$message.error(this.error)
              return;
            }
            this.modalVisible = true;
          }else {
            this.$message.error(res.message)
          }
        });

      },
      applySubmit() {
        if (this.showAssign && this.form.assignees.length < 1) {
          this.error = "请至少选择一个审批人";
          this.$message.error(this.error)
          return;
        } else {
          this.error = "";
        }
        this.submitLoading = true;
        var params = Object.assign({},this.form);
        params.assignees = params.assignees.join(",")
        this.postFormAction(this.url.applyBusiness,params).then(res => {
          if (res.success) {
            this.$message.success("操作成功");
            this.loadData();
            this.modalVisible = false;
          }else {
            this.$message.error(res.message)
          }
        }).finally(()=>this.submitLoading = false);
      },
      edit(r,isView) {
        if (!r.routeName) {
          this.$message.warning(
            "该流程信息未配置表单，请联系开发人员！"
          );
          return;
        }
        isView = isView||false;
        this.lcModa.disabled = isView;
        this.lcModa.title = '修改流程业务信息：'+r.title;
        if (isView) this.lcModa.title = '查看流程业务信息：'+r.title;
        this.lcModa.formComponent = this.getFormComponent(r.routeName).component;
        this.lcModa.processData = r;
        this.lcModa.isNew = false;
        this.lcModa.visible = true;
      },
      remove(r) {
        this.postFormAction(this.url.delByIds,{ids:r.id}).then((res)=>{
          if (res.success){
            this.$message.success(res.message)
            this.loadData();
          }else {
            this.$message.error(res.message)
          }
        })
      },
      cancel(v) {
        this.cancelForm.id = v.id;
        this.cancelForm.procInstId = v.procInstId;
        this.modalCancelVisible = true;
      },
      handelSubmitCancel() {
        this.submitLoading = true;
        this.postFormAction(this.url.cancelApply,this.cancelForm).then(res => {
          if (res.success) {
            this.$message.success("操作成功");
            this.loadData();
            this.modalCancelVisible = false;
          }else {
            this.$message.error(res.message);
          }
        }).finally(()=>this.submitLoading = false);
      },
      history(v) {
        if (!v.procInstId) {
          this.$message.error("流程实例ID不存在");
          return;
        }
        this.procInstId = v.procInstId;
        this.modalLsVisible = true;
      },
      detail(v) {
        this.edit(v,true);
      },
      handleTableChange(pagination, filters, sorter) {
        //分页、排序、筛选变化时触发
        //TODO 筛选
        if (Object.keys(sorter).length > 0) {
          this.isorter.column = sorter.field;
          this.isorter.order = "ascend" == sorter.order ? "asc" : "desc"
        }
        this.ipagination = pagination;
        // this.loadData();
      },
      addApply() {
        this.getProcessList()
      },
      onSearchProcess(value) {
        this.searchProcessKey = value;
        this.getProcessList()
      },
      chooseProcess(v) {
        if (!v.routeName) {
          this.$message.warning(
            "该流程信息未配置表单，请联系开发人员！"
          );
          return;
        }
        this.lcModa.formComponent = this.getFormComponent(v.routeName).component;
        this.lcModa.title = '发起流程：'+v.name;
        this.lcModa.isNew = true;
        this.lcModa.processData = v;
        this.lcModa.visible = true;
        console.log("发起",v)
      },
      afterSub(formData){
          this.lcModa.visible = false;
          this.loadData();
      },

    }
  }
</script>
<style scoped>
  @import '~@assets/less/common.less';
</style>