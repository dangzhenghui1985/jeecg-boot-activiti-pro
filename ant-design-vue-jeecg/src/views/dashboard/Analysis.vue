<template>
  <div>
    <!--    <b-pdf :file="'60137d6395657f2fee283f13'"></b-pdf>-->
    <index-task v-if="indexStyle==1"></index-task>
<!--    <div style="width: 100%;text-align: right;margin-top: 20px">-->
<!--      请选择首页样式：-->
<!--      <a-radio-group v-model="indexStyle">-->
<!--        <a-radio :value="1">任务表格</a-radio>-->
<!--      </a-radio-group>-->
<!--    </div>-->
  </div>
</template>

<script>
import IndexChart from './IndexChart'
import IndexTask from "./IndexTask"
import IndexBdc from './IndexBdc'

export default {
  name: "Analysis",
  components: {
    IndexTask,
  },
  data() {
    return {
      indexStyle:1
    }
  },
  created() {

  },
  methods: {

  }
}
</script>