<template>
  <div class="page-block-style">
    <a-form v-if="showToolbar" layout="inline" class="search-form-style label-width-4" @keyup.enter.native="handleSearch">
      <a-row>
        <a-col v-bind="searchFormItemLayout">
          <a-form-item label="任务名称" prop="name">
            <a-input type="text" allowClear v-model="searchForm.name" />
          </a-form-item>
        </a-col>
        <a-col v-bind="searchFormItemLayout" class="margin8-left">
          <a-form-item>
            <a-button type="primary" @click="handleSearch" icon="search">查询</a-button>
            <a-button @click="handleReset" class="margin8-left" icon="reload">重置</a-button>
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
    <a-table
      bordered
      :size="showToolbar?'middle':'small'"
      :loading="loading"
      rowKey="id"
      :dataSource="data"
      :pagination="ipagination"
      @change="handleTableChange"
      ref="table">
      <a-table-column title="任务名称" dataIndex="name" :ellipsis="true"></a-table-column>
      <a-table-column title="所属流程" dataIndex="processName" :ellipsis="true"></a-table-column>
<!--      <a-table-column title="委托代办人" dataIndex="owner" :ellipsis="true" v-if="showToolbar"></a-table-column>-->
      <a-table-column title="流程发起人" dataIndex="applyer" :ellipsis="true"></a-table-column>
      <a-table-column title="优先级" :width="85" dataIndex="priority" align="center" key="so" :sorter="(a,b)=>a.priority - b.priority">
        <template slot-scope="text">
          <a-tag :color="colorPriority(text)">{{textPriority(text)}}</a-tag>
        </template>
      </a-table-column>
      <a-table-column v-if="showToolbar" title="状态" dataIndex="isSuspended" align="center" key="z" :sorter="(a,b)=>Boolean(a.isSuspended)?0:1 - Boolean(b.isSuspended)?0:1">
        <template slot-scope="t">
          <span :style="{color: t?'#f5222d':'#52c41a'}">{{t?'已挂起':'已激活'}}</span>
        </template>
      </a-table-column>
      <a-table-column v-if="showToolbar" :ellipsis="true" title="创建时间" dataIndex="createTime"></a-table-column>
      <a-table-column title="操作" dataIndex="" :width="250">
        <template slot-scope="t,r">
          <span v-if="!r.isSuspended">
            <a class="link-btn-table" v-if="r.name==='补充材料'" @click="buchong(r)">补充材料</a>
            <a class="link-btn-table success" @click="passTask(r)">通过</a>
            <a class="link-btn-table error" @click="backTask(r)">驳回</a>
<!--            <a class="link-btn-table" @click="delegateTask(r)">委托</a>-->
          </span>
          <a class="link-btn-table" @click="detail(r)">申请详情</a>
          <a class="link-btn-table" @click="history(r)">历史</a>
        </template>
      </a-table-column>
    </a-table>
    <a-modal title="审批历史" v-model="modalLsVisible"  :mask-closable="false" :width="'80%'" :footer="null">
      <div v-if="modalLsVisible">
        <component :is="historicDetail" :procInstId="procInstId"></component>
      </div>
    </a-modal>
    <!--流程表单-->
    <a-modal :title="lcModa.title" v-model="lcModa.visible" :footer="null"  :maskClosable="false" width="80%">
      <component :disabled="lcModa.disabled" v-if="lcModa.visible" :is="lcModa.formComponent"
                 :processData="lcModa.processData" :isNew = "lcModa.isNew"
                 @close="lcModa.visible=false,lcModa.disabled = false"></component>
       </a-modal>
    <!--   审批操作-->
    <a-modal :title="modalTaskTitle" v-model="modalTaskVisible" :mask-closable="false" :width="500">

      <div  v-if="modalTaskVisible">
        <a-form ref="form" :model="form" :label-width="85" :rules="formValidate">
          <a-form-item label="审批意见" prop="reason">
            <a-input type="textarea" v-model="form.comment" :rows="4" />
          </a-form-item>
          <a-form-item label="下一审批人" prop="assignees" v-show="showAssign" :error="error">
            <a-select
              v-model="form.assignees"
              placeholder="请选择"
              allowClear
              mode="multiple"
              :loading="userLoading"
            >
              <a-select-option v-for="(item, i) in assigneeList" :key="i" :value="item.id">{{item.username}}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="下一审批人" v-show="isGateway">
            <span>分支网关处暂不支持自定义选择下一审批人，将发送给下一节点所有人</span>
          </a-form-item>
          <div v-show="form.type==1">
            <a-form-item label="驳回至">
              <a-select
                v-model="form.backTaskKey"
                :loading="backLoading"
                @change="changeBackTask"
              >
                <a-select-option v-for="(item, i) in backList" :key="i" :value="item.key">{{item.name}}</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="指定原节点审批人" prop="assignees" v-show="form.backTaskKey!=-1" :error="error">
              <a-select
                v-model="form.assignees"
                placeholder="请选择"
                allowClear
                mode="multiple"
                :loading="userLoading"
              >
                <a-select-option v-for="(item, i) in assigneeList" :key="i" :value="item.id">{{item.username}}</a-select-option>
              </a-select>
            </a-form-item>
          </div>
<!--          <a-form-item label="选择委托人" prop="userId" :error="error" v-show="form.type==2">-->
<!--            <JSelectUserByDep v-model="form.userId" :multi="false"></JSelectUserByDep>-->
<!--          </a-form-item>-->
          <a-form-item label="消息通知">
            <a-checkbox v-model="form.sendMessage">站内消息通知</a-checkbox>
            <a-checkbox v-model="form.sendSms" disabled>短信通知</a-checkbox>
            <a-checkbox v-model="form.sendEmail" disabled>邮件通知</a-checkbox>
          </a-form-item>
        </a-form>
      </div>
      <div slot="footer">
        <a-button type="text" @click="modalTaskVisible=false">取消</a-button>
        <a-button type="primary" :loading="submitLoading" @click="handelSubmit">提交</a-button>
      </div>
    </a-modal>
  </div>
</template>

<script>
import CommonTableMixin from '@/mixins/CommonTableMixin'


import { JeecgListMixin } from '@/mixins/JeecgListMixin'
import { activitiMixin } from '@/views/activiti/mixins/activitiMixin'
import JSelectUserByDep from '@/components/jeecgbiz/JSelectUserByDep'
export default {
  name: "todo-manage",
  mixins:[CommonTableMixin, activitiMixin,JeecgListMixin],
  components:{JSelectUserByDep},
  props: {
    showToolbar: {
      type: Boolean,
      default: true
    }
  },
  data() {
    return {
      openSearch: true,
      openTip: true,
      loading: true, // 表单加载状态
      modalTaskVisible: false,
      userLoading: false,
      backLoading: false,
      selectCount: 0, // 多选计数
      selectList: [], // 多选数据
      assigneeList: [],
      backList: [
        {
          key: "-1",
          name: "发起人"
        }
      ],
      error: "",
      showAssign: false,
      searchForm: {
        // 搜索框对应data对象
        name: "",
      },
      modalTaskTitle: "",
      modalTitle: "", // 添加或编辑标题
      form: {
        id: "",
        userId: "",
        procInstId: "",
        comment: "",
        type: 0,
        assignees: [],
        backTaskKey: "-1",
        sendMessage: true,
        sendSms: false,
        sendEmail: false
      },
      formValidate: {
        // 表单验证规则
      },
      submitLoading: false, // 添加或编辑提交状态
      data: [], // 表单数据
      total: 0, // 表单数据总数
      dictPriority: [],
      isGateway: false,
      lcModa: {
        title:'',
        disabled:false,
        visible:false,
        formComponent : null,
        isNew : false
      },
      url:{
        todoList:'/actTask/todoList',
        pass:'/actTask/pass',
        back:'/actTask/back',
        backToTask:'/actTask/backToTask',
        delegate:'/actTask/delegate',
        getNextNode:'/activiti_process/getNextNode',
        getNode:'/activiti_process/getNode/',
        getBackList:'/actTask/getBackList/',
        passAll:'/actTask/passAll/',
        backAll:'/actTask/backAll/',
      },
      /*历史*/
      modalLsVisible: false,
      procInstId:''
    };
  },
  mounted() {
    this.init();
  },
  methods: {
    colorPriority(n) {
      let str_n = String(n)
      switch (str_n) {
        case '0':
          return '#87d068'
        case '1':
          return '#fa8c16'
        case '2':
          return '#ff4d4f'
        default:
          return '#999'
      }
    },
    textPriority(n) {
      let str_n = String(n)
      switch (str_n) {
        case '0':
          return '普通'
        case '1':
          return '重要'
        case '2':
          return '紧急'
        default:
          return '无'
      }
    },
    init() {
      this.getDataList();
    },
    buchong(r) {
      if (!r.routeName) {
        this.$message.warning(
          "该流程信息未配置表单，请联系开发人员！"
        );
        return;
      }
      this.lcModa.disabled = false;
      this.lcModa.title = '修改流程业务信息：'+r.processName;
      this.lcModa.formComponent = this.getFormComponent(r.routeName).component;
      this.lcModa.processData = r;
      this.lcModa.isNew = false;
      this.lcModa.visible = true;
    },
    loadData(){},
    getDataList() {
      this.loading = true;
      this.postFormAction(this.url.todoList,this.searchForm).then(res => {
        this.loading = false;
        if (res.success) {
          this.data = res.result||[];
          this.total = this.data.leading;
        }
      });
    },
    handleTableChange(pagination, filters, sorter) {
      //分页、排序、筛选变化时触发
      if (Object.keys(sorter).length > 0) {
        this.isorter.column = sorter.field;
        this.isorter.order = "ascend" == sorter.order ? "asc" : "desc"
      }
      this.ipagination = pagination;
      // this.loadData();
    },
    handleSearch() {
      this.getDataList();
    },
    handleReset() {
      this.searchForm={};
      // 重新加载数据
      this.getDataList();
    },
    showSelect(e) {
      this.selectList = e;
      this.selectCount = e.length;
    },
    clearSelectAll() {
      this.$refs.table.selectAll(false);
    },
    handelSubmit() {
      console.log("提交")
      this.submitLoading = true;
      var formData = Object.assign({},this.form);
      formData.assignees = formData.assignees.join(",");
      if (formData.type == 0) {
        // 通过
        if (this.showAssign && formData.assignees.length < 1) {
          this.$message.error("请至少选择一个审批人")
          this.submitLoading = false;
          return;
        } else {
          this.error = "";
        }
        this.postFormAction(this.url.pass,formData).then(res => {
          this.submitLoading = false;
          if (res.success) {
            this.$message.success("操作成功");
            this.modalTaskVisible = false;
            this.getDataList();
          }
        });
      } else if (formData.type == 1) {
        // 驳回
        if (formData.backTaskKey == "-1") {
          // 驳回至发起人
          this.postFormAction(this.url.back,formData).then(res => {
            this.submitLoading = false;
            if (res.success) {
              this.$message.success("操作成功");
              this.modalTaskVisible = false;
              this.getDataList();
            }
          });
        } else {
          // 自定义驳回
          if (formData.backTaskKey != "-1" && formData.assignees.length < 1) {
            this.$message.error("请至少选择一个审批人")
            this.submitLoading = false;
            return;
          } else {
            this.error = "";
          }
          this.postFormAction(this.url.backToTask,formData).then(res => {
            this.submitLoading = false;
            if (res.success) {
              this.$message.success("操作成功");
              this.modalTaskVisible = false;
              this.getDataList();
            }
          });
        }
      } else if (formData.type == 2) {
        // 委托
        if (!formData.userId) {
          this.$message.error("请选择一委托人")
          this.submitLoading = false;
          return;
        } else {
          this.error = "";
        }
        this.postFormAction(this.url.delegate,formData).then(res => {
          this.submitLoading = false;
          if (res.success) {
            this.$message.success("操作成功");
            this.modalTaskVisible = false;
            this.getDataList();
          }
        });
      }
    },
    detail(r) {
      if (!r.routeName) {
        this.$message.warning(
          "该流程信息未配置表单，请联系开发人员！"
        );
        return;
      }
      this.lcModa.disabled = true;
      this.lcModa.title = '查看流程业务信息：'+r.processName;
      this.lcModa.formComponent = this.getFormComponent(r.routeName).component;
      this.lcModa.processData = r;
      this.lcModa.isNew = false;
      this.lcModa.visible = true;
    },
    passTask(v) {
      console.log(v);
      this.modalTaskTitle = "审批通过";
      this.form.id = v.id;
      this.form.procInstId = v.procInstId;
      this.form.priority = v.priority;
      this.form.type = 0;
      this.modalTaskVisible = true;
      this.userLoading = true;
      this.getAction(this.url.getNextNode,{procDefId:v.procDefId, currActId:v.key}).then(res => {
        this.userLoading = false;
        if (res.success) {
          if (res.result.type == 3 || res.result.type == 4) {
            this.isGateway = true;
            this.showAssign = false;
            this.error = "";
            return;
          }
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
            this.showAssign = false;
          }
        }
      });

    },
    changeBackTask(v) {
      if (v == "-1") {
        return;
      }
      this.userLoading = true;
      this.getAction(this.url.getNode+v).then(res => {
        this.userLoading = false;
        if (res.success) {
          if (res.result.users && res.result.users.length > 0) {
            this.assigneeList = res.result.users;
            // 默认勾选
            let ids = [];
            res.result.users.forEach(e => {
              ids.push(e.username);
            });
            this.form.assignees = ids;
          }
        }
      });
    },
    backTask(v) {
      this.modalTaskTitle = "审批驳回";
      this.form.id = v.id;
      this.form.procInstId = v.procInstId;
      this.form.procDefId = v.procDefId;
      this.form.priority = v.priority;
      this.form.type = 1;
      this.showAssign = false;
      this.modalTaskVisible = true;
      // 获取可驳回节点
      this.backList = [
        {
          key: "-1",
          name: "发起人"
        }
      ];
      this.form.backTaskKey = "-1";
      this.backLoading = true;
      this.getAction(this.url.getBackList+v.procInstId).then(res => {
        this.backLoading = false;
        if (res.success) {
          res.result.forEach(e => {
            this.backList.push(e);
          });
        }
      });
    },
    delegateTask(v) {
      this.modalTaskTitle = "委托他人代办";
      this.form.id = v.id;
      this.form.procInstId = v.procInstId;
      this.form.type = 2;
      this.showAssign = false;
      this.modalTaskVisible = true;
    },
    history(v) {
      if (!v.procInstId) {
        this.$message.error("流程实例ID不存在");
        return;
      }
      this.procInstId = v.procInstId;
      this.modalLsVisible = true;
    },
    passAll() {
      if (this.selectCount <= 0) {
        this.$message.warning("您还未选择要通过的数据");
        return;
      }
      // 批量通过
      this.modalVisible = true;
      this.$confirm({
        title: "确认通过",
        content:
          "您确认要通过所选的 " +
          this.selectCount +
          " 条数据? 注意：将默认分配给节点设定的所有可审批用户",
        loading: true,
        onOk: () => {
          let ids = "";
          this.selectList.forEach(function(e) {
            ids += e.id + ",";
          });
          ids = ids.substring(0, ids.length - 1);
          this.postFormAction(this.url.passAll,{ids:ids}).then(res => {
            if (res.success) {
              this.$message.success("操作成功");
              this.modalVisible = false;
              this.clearSelectAll();
              this.getDataList();
            }
          });
        }
      });
    },
    backAll() {
      if (this.selectCount <= 0) {
        this.$message.warning("您还未选择要驳回的数据");
        return;
      }
      // 批量驳回
      this.modalVisible = true;
      this.$confirm({
        title: "确认驳回",
        content:
          "您确认要驳回所选的 " +
          this.selectCount +
          " 条数据? 注意：所有流程将驳回至发起人",
        loading: true,
        onOk: () => {
          let procInstIds = "";
          this.selectList.forEach(function(e) {
            procInstIds += e.procInstId + ",";
          });
          procInstIds = procInstIds.substring(0, procInstIds.length - 1);
          this.postFormAction(this.url.backAll,{procInstIds:procInstIds}).then(res => {
            if (res.success) {
              this.$message.success("操作成功");
              this.modalVisible = false;
              this.clearSelectAll();
              this.getDataList();
            }
          });
        }
      });
    }
  },

};
</script>
<style>
.ant-card-wider-padding .ant-card-head {
  padding: 0 32px;
  background-color: #ddd;
}

</style>