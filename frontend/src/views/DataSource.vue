<template>
  <div>
    <div class="toolbar">
      <a-button type="primary" @click="showModal()">
        <plus-outlined /> 新增数据源接口
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
        <template v-if="column.key === 'enabled'">
          <a-tag :color="record.enabled ? 'green' : 'red'">{{ record.enabled ? '启用' : '禁用' }}</a-tag>
        </template>
        <template v-if="column.key === 'postProcessor'">
          <a-tag v-if="record.postProcessor" color="blue">已配置</a-tag>
          <span v-else>-</span>
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
      :title="editingId ? '编辑数据源接口' : '新增数据源接口'" 
      width="900px" 
      :confirm-loading="submitting"
      @ok="handleSubmit"
    >
      <a-form :model="form" layout="vertical">
        <a-form-item label="接口名称" required>
          <a-input v-model:value="form.name" placeholder="请输入接口名称" :maxlength="100" show-count />
        </a-form-item>
        <a-form-item label="接口地址" required>
          <a-input v-model:value="form.url" placeholder="https://api.example.com/data" />
          <div class="hint">必须以 http:// 或 https:// 开头</div>
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="请求方式">
              <a-select v-model:value="form.method">
                <a-select-option value="GET">GET</a-select-option>
                <a-select-option value="POST">POST</a-select-option>
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
        <a-form-item label="请求体 (JSON)" v-if="form.method === 'POST'">
          <a-textarea v-model:value="requestBodyStr" :rows="4" placeholder='{"key": "value"}' />
          <div class="hint hint-error" v-if="requestBodyError">{{ requestBodyError }}</div>
        </a-form-item>
        <a-form-item label="返回数据字段定义">
          <a-alert 
            message="定义数据源返回的字段，用于在绑定配置中进行字段映射" 
            type="info" 
            show-icon 
            class="alert-mb"
          />
          <a-table :columns="fieldColumns" :data-source="form.responseFields" :pagination="false" size="small">
            <template #bodyCell="{ column, record, index }">
              <template v-if="column.key === 'path'">
                <a-input v-model:value="record.path" size="small" placeholder="data.items[0].name" />
              </template>
              <template v-if="column.key === 'name'">
                <a-input v-model:value="record.name" size="small" placeholder="字段名称" />
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
                <a-input v-model:value="record.description" size="small" placeholder="字段描述" />
              </template>
              <template v-if="column.key === 'action'">
                <a-button type="link" size="small" danger @click="removeField(index)">删除</a-button>
              </template>
            </template>
          </a-table>
          <a-button type="dashed" block class="block-mt" @click="addField">+ 添加字段</a-button>
        </a-form-item>
        <a-form-item label="数据后置处理">
          <a-alert 
            message="可选：对返回数据进行处理，支持简单指令或JavaScript代码" 
            type="info" 
            show-icon 
            class="alert-mb"
          />
          <a-textarea 
            v-model:value="form.postProcessor" 
            :rows="4" 
            placeholder="// 简单指令示例:&#10;// extract:data.items  - 提取指定路径的数据&#10;// map:oldField->newField  - 字段重命名&#10;&#10;// JavaScript示例:&#10;// var result = data.items.filter(i => i.status === 'active');&#10;// return result;" 
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { dataSourceApi } from '../api'

const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const modalVisible = ref(false)
const editingId = ref(null)
const headerKeys = ref({})
const requestBodyStr = ref('{}')
const requestBodyError = ref('')

const form = reactive({
  name: '',
  url: '',
  method: 'GET',
  headers: {},
  requestBody: {},
  responseFields: [],
  postProcessor: '',
  enabled: true
})

const columns = [
  { title: '接口名称', dataIndex: 'name', key: 'name' },
  { title: '接口地址', dataIndex: 'url', key: 'url', ellipsis: true },
  { title: '请求方式', dataIndex: 'method', key: 'method', width: 100 },
  { title: '后置处理', key: 'postProcessor', width: 100 },
  { title: '状态', dataIndex: 'enabled', key: 'enabled', width: 80 },
  { title: '操作', key: 'action', width: 150 }
]

const fieldColumns = [
  { title: 'JSON路径', key: 'path', width: 200 },
  { title: '字段名称', key: 'name', width: 120 },
  { title: '类型', key: 'type', width: 100 },
  { title: '描述', key: 'description' },
  { title: '操作', key: 'action', width: 60 }
]

const methodColors = { GET: 'blue', POST: 'green' }

// 验证请求体JSON格式
watch(requestBodyStr, (val) => {
  try {
    JSON.parse(val || '{}')
    requestBodyError.value = ''
  } catch (e) {
    requestBodyError.value = 'JSON格式错误: ' + e.message
  }
})

const fetchList = async () => {
  loading.value = true
  try {
    const res = await dataSourceApi.list()
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
    Object.assign(form, { 
      ...record, 
      headers: { ...record.headers }, 
      responseFields: [...(record.responseFields || [])] 
    })
    headerKeys.value = Object.fromEntries(Object.keys(record.headers || {}).map((k) => [k, k]))
    requestBodyStr.value = JSON.stringify(record.requestBody || {}, null, 2)
  } else {
    editingId.value = null
    Object.assign(form, { 
      name: '', url: '', method: 'GET', headers: {}, 
      requestBody: {}, responseFields: [], postProcessor: '', enabled: true 
    })
    headerKeys.value = {}
    requestBodyStr.value = '{}'
  }
  requestBodyError.value = ''
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
  
  let requestBody = {}
  try {
    requestBody = JSON.parse(requestBodyStr.value || '{}')
  } catch (e) {
    message.error('请求体JSON格式错误')
    return
  }
  
  const newHeaders = {}
  Object.keys(form.headers).forEach(oldKey => {
    const newKey = headerKeys.value[oldKey] || oldKey
    if (newKey) {
      newHeaders[newKey] = form.headers[oldKey]
    }
  })
  
  const data = { ...form, headers: newHeaders, requestBody }
  
  submitting.value = true
  try {
    if (editingId.value) {
      await dataSourceApi.update(editingId.value, data)
      message.success('更新成功')
    } else {
      await dataSourceApi.create(data)
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
    await dataSourceApi.delete(id)
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

const addField = () => {
  form.responseFields.push({ path: '', name: '', type: 'string', description: '' })
}

const removeField = (index) => {
  form.responseFields.splice(index, 1)
}

onMounted(fetchList)
</script>

<style scoped>
.toolbar { margin-bottom: 16px; }
.header-row { display: flex; align-items: center; margin-bottom: 8px; }
.hint { font-size: 12px; color: #999; margin-top: 4px; }
</style>
