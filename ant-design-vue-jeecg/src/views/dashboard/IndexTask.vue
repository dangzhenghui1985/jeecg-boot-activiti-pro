<template>
  <div >
    <a-row :gutter="8">
      <a-col :xl="14" :lg="24" :md="24" :sm="24" :xs="24">
        <div class="dashboard-block">
          <div class="dashboard-title">
            <span>我的待办</span>
            <a @click="toClickedPath('/activiti/todoManage')" class="span-link-btn more">查看更多</a>
          </div>
          <TodoManage :showToolbar="false" />
        </div>
        <div class="dashboard-block">
          <div class="dashboard-title">
            <span>我的申请</span>
            <a @click="toClickedPath('/activiti/applyList')" class="span-link-btn more">查看更多</a>
          </div>
          <ApplyList :showToolbar="false" />
        </div>
      </a-col>
      <a-col :xl="10" :lg="24" :md="24" :sm="24" :xs="24">
        <div class="dashboard-block">
          <div class="dashboard-title">
            <span>考勤</span>
            <a @click="toClickedPath('/oa/checkin/OaKaoQinRiLi')" class="span-link-btn more">更多记录</a>
          </div>
          <a-row :gutter="16">
            <a-col :span="12" v-for="(item, index) in punchList" :key="'punch_'+index">
              <div @click="clickPunch(index)" class="dashpunch-item" :class="'dashpunch-item'+index">
                <img size="large" :src="'/images/'+item.icon" />
                <div class="text-wrap">
                  <span class="p-name">{{item.name}}</span>
                  <span class="p-desc">{{item.desc}}</span>
                </div>
              </div>
            </a-col>
          </a-row>
        </div>
        <div class="dashboard-block">
          <div class="dashboard-title">我的任务</div>
          <a-row>
            <a-col :span="12" v-for="(item, index) in taskList" :key="'task_panel_'+index">
              <div @click="toClickedPath(item.path)" class="dashpanel-item">
                <img size="large" :src="'/images/'+item.icon" />
                <div class="text-wrap">
                  <span class="p-name">{{item.name}}</span>
                  <span class="p-desc">{{item.desc}}</span>
                </div>
              </div>
            </a-col>
          </a-row>
        </div>
        <div class="dashboard-block" v-for="(item, index) in activeKeyAll" :key="'process_'+index">
          <div class="dashboard-title">{{filterDictText(dictOptions, item)}}</div>
          <a-row>
            <a-col :span="12" v-for="(subItem, subIndex) in processDataMap[item]" :key="'process_sub_'+index+subIndex">
              <div @click="chooseProcess(subItem)" class="dashpanel-item">
                <img size="large" :src="'/images/'+subItem.businessTable+'.png'" />
                <div class="text-wrap">
                  <span class="p-name">{{subItem.description}}</span>
                  <span class="p-desc">{{subItem.description}}流程办理</span>
                </div>
              </div>
            </a-col>
          </a-row>
        </div>
        <div class="dashboard-block">
          <div class="dashboard-title">服务支持</div>
          <a-row>
            <a-col :span="12">
              <div @click="showFeedbackModal()" class="dashpanel-item">
                <img size="large" :src="'/images/customer_support.png'" />
                <div class="text-wrap">
                  <span class="p-name">服务支持</span>
                  <span class="p-desc">提供系统优化建议</span>
                </div>
              </div>
            </a-col>
          </a-row>
        </div>
      </a-col>
    </a-row>
    <!--流程表单-->

    <j-modal
      :title="lcModa.title"
      :width="800"
      :visible="lcModa.visible"
      switchFullscreen
      @ok="handleOk"
      :okButtonProps="{ class:{'jee-hidden': !lcModa.isNew} }"
      @cancel="handleCancel"
      cancelText="关闭">
      <component
        ref="realForm"
        :disabled="lcModa.disabled"
        v-if="lcModa.visible"
        :is="lcModa.formComponent"
        :formBpm="!lcModa.isNew"
        :formData="loadData.processData"
        @afterSubmit="afterSub"
        @close="lcModa.visible=false,lcModa.disabled = false">
      </component>
    </j-modal>


<!--    <a-modal-->
<!--      :title="lcModa.title"-->
<!--      v-model="lcModa.visible"-->
<!--      :footer="null"-->
<!--      :maskClosable="false"-->
<!--      width="80%">-->
<!--      -->
<!--    </a-modal>-->
    <!--提交申请表单-->
    <a-modal title="提交申请" v-model="modalVisible" :mask-closable="false" :width="500" :footer="null">
      <div v-if="modalVisible">
        <a-form-item label="选择审批人" v-show="showAssign">
          <a-select style="width: 100%"
                    v-model="form.assignees"
                    placeholder="请选择"
                    mode="multiple"
                    :allowClear="true">
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
  </div>
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
import TodoManage from '@views/activiti/todoManage'
import ApplyList from '@views/activiti/applyList'
import {activitiFormMixin} from "@views/activiti/mixins/activitiFormMixin";

export default {
  name: "analysis",
  mixins:[activitiMixin,JeecgListMixin],
  components: {
    TodoManage,
    ApplyList,
    JEllipsis,
    JTreeSelect,
    historicDetail,
  },
  data () {
    return {
      taskList: [
        {icon: 'home_00.png', name: '我的已办', desc: '查看已办消息记录', path: '/activiti/doneManage'},
        {icon: 'tongzhi_00.png', name: '我的公告', desc: '重要信息全员播报', path: '/isystem/annountCement'},
      ],
      punchList: [
        {icon: 'zichan_00.png', name: '签到', desc: '上班打卡'},
        {icon: 'daka.png', name: '签退', desc: '下班打卡'},
      ],
      financeList: [],
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
      cancelForm: {},
      feedbackVisible: false,
      getForm:'/actBusiness/getForm',
      addApply:'/actBusiness/add',
      editForm:'/actBusiness/editForm',
    }
  },
  computed:{
  },
  methods: {
    toClickedPath(n) {
      this.$router.push(n)
    },
    clickPunch(n) {
      n === 1 ? this.handleAdd(false) : this.handleAdd()
    },
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
    loadData(arg) {
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
            this.financeList = this.processDataMap['oa']
            console.log('=================', this.processDataMap, this.activeKeyAll)

          }
          this.processModalVisible = true;
        }else {
          this.$message.warning(res.message)
        }
      }).finally(()=>this.addApplyLoading = false);
    },
    getQueryParams(){
      var param = Object.assign({}, this.queryParam,this.isorter);
      delete param.createTimeRange; // 时间参数不传递后台
      return filterObj(param);
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
      this.lcModa.disabled=false;
      this.lcModa.processData = v;
      this.lcModa.visible = true;
      console.log("发起",v)
    },
    afterSub(formData){
      this.lcModa.visible = false;
      this.loadData();
    },
    showFeedbackModal() {
      this.$refs.feedbackModalForm.add();
      this.$refs.feedbackModalForm.title = "提交反馈";
    },
    feedbackSent() {
      this.feedbackVisible = false;
    },
    handleOk () {
      this.$refs.realForm.$refs.form.validate(valid =>{
        let url=this.addApply
        if (this.$refs.realForm.model.id){
          url=this.editForm;
        }
        if (valid){
          let param={
         "procDefId":   this.lcModa.processData.id,
          "procDeTitle":  this.lcModa.processData.name,
          "tableName":  this.lcModa.processData.businessTable
          }
          this.postFormAction(url, param,this.$refs.realForm.model).then((res)=>{
            if (res.success){
              this.$message.success("保存成功！")
              this.afterSub();
            }else {
              this.$message.error(res.message)
            }
          }).finally(()=>{
            this.lcModa.visible = false;
          })
        }
      })


    },
    close () {
      this.lcModa.visible = false;
    },
    handleCancel () {
      this.close()
    }

  }
}
</script>
<style lang="less" scoped>
@import '~@assets/less/common.less';
@import './components/style.less';
</style>