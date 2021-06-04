import Vue from 'vue'
import {ACCESS_TOKEN} from "@/store/mutation-types"
import {VALIDATE_NO_PASSED, validateFormAndTables} from "@/utils/JEditableTableUtil";
import {httpAction} from "@api/manage";

export const activitiFormMixin = {
  components: {

  },
  data(){
    return {
      btndisabled: false,
      url: {
        getForm:'/actBusiness/getForm',
        addApply:'/actBusiness/add',
        editForm:'/actBusiness/editForm',
      }
    }
  },
  computed:{

  },
  created () {
    console.log("流程数据",this.processData)
    if (!this.isNew){
      this.init();
    }else {
    this.edit({});
    }
  }
  ,
  methods:{
    handleOk () {
      console.log(this.data)
      let formData = {}
      formData.procDefId = this.processData.id;
      formData.procDeTitle = this.processData.name;
      if (!formData.tableName){
        formData.tableName = this.processData.businessTable;
      }
      const that = this;
      // 触发表单验证
      this.form.validateFields((err, values) => {
        if (!err) {
          that.confirmLoading = true;

          let body = Object.assign(this.model, values);
          var url = this.url.addApply;
          if (!this.isNew){
            formData.tableName=body.tableName;
            url = this.url.editForm;
          }
          this.btndisabled = true;
          console.log("表单提交数据",body)
          this.postFormAction(url,formData,body).then((res)=>{
            if (res.success){
              this.$message.success("保存成功！")
              this.$emit('afterSubmit',formData)
            }else {
              this.$message.error(res.message)
            }
          }).finally(()=>{
            this.btndisabled = false;
          })
        }

      })
    },


    handleSubmit (e)  {
      console.log(this.data)
      let formData = {}
      formData.procDefId = this.processData.id;
      formData.procDeTitle = this.processData.name;
      if (!formData.tableName){
        formData.tableName = this.processData.businessTable;
      }
      /** 触发表单验证 */
      this.getAllTable().then(tables => {
        /** 一次性验证主表和所有的次表 */
        return validateFormAndTables(this.form, tables)
      }).then(allValues => {
        if (typeof this.classifyIntoFormData !== 'function') {
          throw this.throwNotFunction('classifyIntoFormData')
        }

        let body = this.classifyIntoFormData(allValues)
        // formData.filedNames = _.keys(values).join(",");
        // console.log('formData', values)

        var url = this.url.addApply;
        if (!this.isNew){
          formData.tableName=body.tableName;
          url = this.url.editForm;
        }
        this.btndisabled = true;
        this.postFormAction(url,formData,body).then((res)=>{
          if (res.success){
            this.$message.success("保存成功！")
            this.$emit('afterSubmit',formData)
          }else {
            this.$message.error(res.message)
          }
        }).finally(()=>{
          this.btndisabled = false;
        })
      }).catch(e => {
        if (e.error === VALIDATE_NO_PASSED) {
          // 如果有未通过表单验证的子表，就自动跳转到它所在的tab
          this.activeKey = e.index == null ? this.activeKey : this.refKeys[e.index]
        } else {
          console.error(e)
        }
      })
    },
    init(){
      this.btndisabled = true;
      var r = this.processData;
      this.getAction(this.url.getForm,{
        tableId:r.tableId,
        tableName:r.tableName,
      }).then((res)=>{
        if (res.success){
          let formData = res.result;
          formData.tableName = r.tableName;
          this.data = formData;
          this.edit(this.data)
          // console.log("表单回显数据",this.data)
          // this.$nextTick(() => {
          //   this.form.setFieldsValue(pick(this.data,'type','qjsy','start_date','end_date','days','file_path'))
          // });
          this.btndisabled = false;
        }else {
          this.$message.error(res.message)
        }
      })
    },
    close() {
      this.$emit('close')
    },
  }

}