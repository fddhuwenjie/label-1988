<template>
  <div>
    <div class="stats-row" v-if="stats">
      <a-row :gutter="16">
        <a-col :span="6">
          <a-statistic title="总执行次数" :value="stats.total" />
        </a-col>
        <a-col :span="6">
          <a-statistic title="成功次数" :value="stats.success" class="stat-success" />
        </a-col>
        <a-col :span="6">
          <a-statistic title="失败次数" :value="stats.failed" class="stat-failed" />
        </a-col>
        <a-col :span="6">
          <a-statistic title="平均耗时" :value="stats.avgDuration.toFixed(0)" suffix="ms" />
        </a-col>
      </a-row>
    </div>
    
    <div class="toolbar">
      <a-space>
        <a-select v-model:value="filters.bindingId" placeholder="选择绑定配置" allow-clear class="select-filter" @change="fetchList">
          <a-select-option v-for="b in bindings" :key="b.id" :value="b.id">{{ b.name }}</a-select-option>
        </a-select>
        <a-select v-model:value="filters.status" placeholder="执行状态" allow-clear class="select-filter-sm" @change="fetchList">
          <a-select-option value="SUCCESS">成功</a-select-option>
          <a-select-option value="FAILED">失败</a-select-option>
        </a-select>
        <a-button @click="handleRefresh">
          <reload-outlined /> 刷新
        </a-button>
      </a-space>
    </div>

    <a-table 
      :columns="columns" 
      :data-source="list" 
      :loading="loading" 
      :pagination="pagination"
      row-key="id"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="record.status === 'SUCCESS' ? 'green' : 'red'">
            {{ record.status === 'SUCCESS' ? '成功' : '失败' }}
          </a-tag>
        </template>
        <template v-if="column.key === 'executedAt'">
          {{ formatTime(record.executedAt) }}
        </template>
        <template v-if="column.key === 'duration'">
          <span :class="{ 'duration-warn': record.duration > 1000 }">
            {{ record.duration }}ms
          </span>
        </template>
        <template v-if="column.key === 'action'">
          <a-button type="link" size="small" @click="showDetail(record)">详情</a-button>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="detailVisible" title="执行详情" width="900px" :footer="null">
      <a-descriptions :column="2" bordered size="small" v-if="currentLog">
        <a-descriptions-item label="绑定配置">{{ currentLog.bindingName }}</a-descriptions-item>
        <a-descriptions-item label="执行状态">
          <a-tag :color="currentLog.status === 'SUCCESS' ? 'green' : 'red'">
            {{ currentLog.status === 'SUCCESS' ? '成功' : '失败' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="数据源接口">{{ currentLog.dataSourceName }}</a-descriptions-item>
        <a-descriptions-item label="推送接口">{{ currentLog.pushInterfaceName }}</a-descriptions-item>
        <a-descriptions-item label="执行时间">{{ formatTime(currentLog.executedAt) }}</a-descriptions-item>
        <a-descriptions-item label="耗时">{{ currentLog.duration }}ms</a-descriptions-item>
        <a-descriptions-item label="错误信息" :span="2" v-if="currentLog.errorMessage">
          <a-alert type="error" :message="currentLog.errorMessage" show-icon />
        </a-descriptions-item>
      </a-descriptions>
      
      <a-divider>数据源接口调用</a-divider>
      <a-row :gutter="16">
        <a-col :span="12">
          <div class="json-label">请求:</div>
          <pre class="json-box">{{ formatJson(currentLog?.dataSourceRequest) }}</pre>
        </a-col>
        <a-col :span="12">
          <div class="json-label">响应 ({{ currentLog?.dataSourceStatus }}):</div>
          <pre class="json-box">{{ formatJson(currentLog?.dataSourceResponse) }}</pre>
        </a-col>
      </a-row>
      
      <a-divider>推送接口调用</a-divider>
      <a-row :gutter="16">
        <a-col :span="12">
          <div class="json-label">请求:</div>
          <pre class="json-box">{{ formatJson(currentLog?.pushRequest) }}</pre>
        </a-col>
        <a-col :span="12">
          <div class="json-label">响应 ({{ currentLog?.pushStatus }}):</div>
          <pre class="json-box">{{ formatJson(currentLog?.pushResponse) }}</pre>
        </a-col>
      </a-row>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { logApi, bindingApi } from '../api'
import dayjs from 'dayjs'

const loading = ref(false)
const list = ref([])
const bindings = ref([])
const stats = ref(null)
const detailVisible = ref(false)
const currentLog = ref(null)

const filters = reactive({
  bindingId: null,
  status: null
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total) => `共 ${total} 条`
})

const columns = [
  { title: '绑定配置', dataIndex: 'bindingName', key: 'bindingName' },
  { title: '数据源', dataIndex: 'dataSourceName', key: 'dataSourceName' },
  { title: '推送接口', dataIndex: 'pushInterfaceName', key: 'pushInterfaceName' },
  { title: '状态', key: 'status', width: 80 },
  { title: '执行时间', key: 'executedAt', width: 180 },
  { title: '耗时', key: 'duration', width: 100 },
  { title: '操作', key: 'action', width: 80 }
]

const formatTime = (time) => {
  if (!time) return '-'
  if (typeof time === 'string' && time.match(/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/)) {
    return time
  }
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

const formatJson = (str) => {
  if (!str) return ''
  try {
    return JSON.stringify(JSON.parse(str), null, 2)
  } catch {
    return str
  }
}

const fetchStats = async () => {
  try {
    const res = await logApi.stats()
    stats.value = res.data
  } catch (e) {
    console.error('Failed to fetch stats')
  }
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await logApi.list({
      current: pagination.current,
      size: pagination.pageSize,
      bindingId: filters.bindingId,
      status: filters.status
    })
    list.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch (e) {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}

const fetchBindings = async () => {
  try {
    const res = await bindingApi.list()
    bindings.value = res.data || []
  } catch (e) {
    console.error('Failed to fetch bindings')
  }
}

const handleRefresh = () => {
  fetchStats()
  fetchList()
  message.success('刷新成功')
}

const handleTableChange = (pag) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchList()
}

const showDetail = (record) => {
  currentLog.value = record
  detailVisible.value = true
}

onMounted(() => {
  fetchBindings()
  fetchStats()
  fetchList()
})
</script>

<style scoped>
.stats-row { margin-bottom: 24px; padding: 16px; background: #fafafa; border-radius: 4px; }
.toolbar { margin-bottom: 16px; }
.json-label { font-weight: bold; margin-bottom: 8px; }
.json-box {
  background: #f5f5f5;
  padding: 12px;
  border-radius: 4px;
  max-height: 200px;
  overflow: auto;
  font-size: 12px;
}
</style>
