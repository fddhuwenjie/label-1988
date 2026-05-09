<template>
  <div>
    <div class="toolbar">
      <a-button type="primary" @click="showModal()">
        <plus-outlined /> 新增推送接口
      </a-button>
      <a-button class="btn-toolbar-gap" @click="handleRefresh">
        <reload-outlined /> 刷新
      </a-button>
    </div>
    
    <a-table :columns="columns" :data-source="list" :loading="loading" row-key="id">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'method'">
          <a-tag :color="methodColors[record.method]">{{ record.method }}</a-tag>
        </template>
        <template v-if="column.key === 'params'">
          {{ record.requestParams?.length || 0 }} 个参数
        </template>
        <template v-if="column.key === 'enabled'">
          <a-tag :color="record.enabled ? 'green' : 'red'">{{ record.enabled ? '启用' : '禁用' }}</a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="showModal(record)">编辑</a-button>
            <a-popconfirm title="确定删除?" @confirm="handleDelete(record.id)">
              <a-button type="link" size="small" danger>删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal 
      v-model:open="modalVisible" 
      :title="editingId ? '编辑推送接口' : '新增推送接口'" 
      width="800px" 
      :confirm-loading="submitting"
      @ok="handleSubmit"
    >
      <a-form :model="form" layout="vertical">
        <a-form-item label="接口名称" required>
          <a-input v-model:value="form.name" placeholder="请输入接口名称" :maxlength="100" show-count />
        </a-form-item>
        <a-form-item label="接口地址" required>
          <a-input v-model:value="form.url" placeholder="https://api.example.com/endpoint" />
          <div class="hint">必须以 http:// 或 https:// 开头</div>
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="请求方式">
              <a-select v-model:value="form.method">
                <a-select-option value="GET">GET</a-select-option>
                <a-select-option value="POST">POST</a-select-option>
                <a-select-option value="PUT">PUT</a-select-option>
                <a-select-option value="DELETE">DELETE</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="状态">
              <a-switch v-model:checked="form.enabled" checked-children="启用" un-checked-children="禁用" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="请求头">
          <div v-for="(_, key) in form.headers" :key="key" class="header-row">
            <a-input v-model:value="headerKeys[key]" placeholder="Header名称" class="input-header-key" />
            <a-input v-model:value="form.headers[key]" placeholder="Header值" class="input-header-value" />
            <a-button type="link" danger @click="removeHeader(key)">删除</a-button>
          </div>
          <a-button type="dashed" block @click="addHeader">+ 添加请求头</a-button>
        </a-form-item>
        <a-form-item label="请求参数">
          <a-alert 
            message="定义推送接口需要的参数，在绑定配置中可以映射数据源字段到这些参数" 
            type="info" 
            show-icon 
            class="alert-mb"
          />
          <a-table :columns="paramColumns" :data-source="form.requestParams" :pagination="false" size="small">
            <template #bodyCell="{ column, record, index }">
              <template v-if="column.key === 'name'">
                <a-input v-model:value="record.name" size="small" placeholder="参数名" />
              </template>
              <template v-if="column.key === 'type'">
                <a-select v-model:value="record.type" size="small" class="select-full">
                  <a-select-option value="string">string</a-select-option>
                  <a-select-option value="number">number</a-select-option>
                  <a-select-option value="boolean">boolean</a-select-option>
                  <a-select-option value="object">object</a-select-option>
                  <a-select-option value="array">array</a-select-option>
                </a-select>
              </template>
              <template v-if="column.key === 'description'">
                <a-input v-model:value="record.description" size="small" placeholder="参数描述" />
              </template>
              <template v-if="column.key === 'required'">
                <a-checkbox v-model:checked="record.required" />
              </template>
              <template v-if="column.key === 'defaultValue'">
                <a-input v-model:value="record.defaultValue" size="small" placeholder="默认值" />
              </template>
              <template v-if="column.key === 'action'">
                <a-button type="link" size="small" danger @click="removeParam(index)">删除</a-button>
              </template>
            </template>
          </a-table>
          <a-button type="dashed" block class="block-mt" @click="addParam">+ 添加参数</a-button>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { pushApi } from '../api'

const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const modalVisible = ref(false)
const editingId = ref(null)
const headerKeys = ref({})

const form = reactive({
  name: '',
  url: '',
  method: 'POST',
  headers: {},
  requestParams: [],
  enabled: true
})

const columns = [
  { title: '接口名称', dataIndex: 'name', key: 'name' },
  { title: '接口地址', dataIndex: 'url', key: 'url', ellipsis: true },
  { title: '请求方式', dataIndex: 'method', key: 'method', width: 100 },
  { title: '参数', key: 'params', width: 100 },
  { title: '状态', dataIndex: 'enabled', key: 'enabled', width: 80 },
  { title: '操作', key: 'action', width: 150 }
]

const paramColumns = [
  { title: '参数名', key: 'name', width: 120 },
  { title: '类型', key: 'type', width: 100 },
  { title: '描述', key: 'description' },
  { title: '必填', key: 'required', width: 60 },
  { title: '默认值', key: 'defaultValue', width: 120 },
  { title: '操作', key: 'action', width: 60 }
]

const methodColors = { GET: 'blue', POST: 'green', PUT: 'orange', DELETE: 'red' }

const fetchList = async () => {
  loading.value = true
  try {
    const res = await pushApi.list()
    list.value = res.data || []
  } catch (e) {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}

const showModal = (record = null) => {
  if (record) {
    editingId.value = record.id
    Object.assign(form, { ...record, headers: { ...record.headers }, requestParams: [...(record.requestParams || [])] })
    headerKeys.value = Object.fromEntries(Object.keys(record.headers || {}).map((k) => [k, k]))
  } else {
    editingId.value = null
    Object.assign(form, { name: '', url: '', method: 'POST', headers: {}, requestParams: [], enabled: true })
    headerKeys.value = {}
  }
  modalVisible.value = true
}

const handleSubmit = async () => {
  // 前端校验
  if (!form.name?.trim()) {
    message.warning('请输入接口名称')
    return
  }
  if (!form.url?.trim()) {
    message.warning('请输入接口地址')
    return
  }
  if (!/^https?:\/\//.test(form.url)) {
    message.warning('接口地址必须以 http:// 或 https:// 开头')
    return
  }
  
  // 验证参数名不能为空
  const invalidParams = form.requestParams.filter(p => !p.name?.trim())
  if (invalidParams.length > 0) {
    message.warning('请填写所有参数的名称')
    return
  }
  
  const newHeaders = {}
  Object.keys(form.headers).forEach(oldKey => {
    const newKey = headerKeys.value[oldKey] || oldKey
    if (newKey) {
      newHeaders[newKey] = form.headers[oldKey]
    }
  })
  const data = { ...form, headers: newHeaders }
  
  submitting.value = true
  try {
    if (editingId.value) {
      await pushApi.update(editingId.value, data)
      message.success('更新成功')
    } else {
      await pushApi.create(data)
      message.success('创建成功')
    }
    modalVisible.value = false
    fetchList()
  } catch (e) {
    // 错误已在拦截器中处理
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (id) => {
  try {
    await pushApi.delete(id)
    message.success('删除成功')
    fetchList()
  } catch (e) {
    // 错误已在拦截器中处理
  }
}

const handleRefresh = () => {
  fetchList()
  message.success('刷新成功')
}

const addHeader = () => {
  const key = `header_${Date.now()}`
  form.headers[key] = ''
  headerKeys.value[key] = ''
}

const removeHeader = (key) => {
  delete form.headers[key]
  delete headerKeys.value[key]
}

const addParam = () => {
  form.requestParams.push({ name: '', type: 'string', description: '', required: false, defaultValue: '' })
}

const removeParam = (index) => {
  form.requestParams.splice(index, 1)
}

onMounted(fetchList)
</script>

<style scoped>
.toolbar { margin-bottom: 16px; }
.header-row { display: flex; align-items: center; margin-bottom: 8px; }
.hint { font-size: 12px; color: #999; margin-top: 4px; }
</style>
