<template>
  <div>
    <div class="toolbar">
      <a-button type="primary" @click="showModal()">
        <plus-outlined /> 新增绑定配置
      </a-button>
      <a-button class="btn-toolbar-gap" @click="handleRefresh">
        <reload-outlined /> 刷新
      </a-button>
    </div>
    
    <a-table :columns="columns" :data-source="list" :loading="loading" row-key="id">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'dataSource'">
          {{ getDataSourceName(record.dataSourceId) }}
        </template>
        <template v-if="column.key === 'pushInterface'">
          {{ getPushInterfaceName(record.pushInterfaceId) }}
        </template>
        <template v-if="column.key === 'enabled'">
          <a-tag :color="record.enabled ? 'green' : 'red'">{{ record.enabled ? '启用' : '禁用' }}</a-tag>
        </template>
        <template v-if="column.key === 'lastExecutedAt'">
          {{ record.lastExecutedAt || '-' }}
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a-button 
              type="link" 
              size="small" 
              :loading="executingId === record.id"
              @click="handleExecute(record)"
            >
              {{ executingId === record.id ? '执行中' : '执行' }}
            </a-button>
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
      :title="editingId ? '编辑绑定配置' : '新增绑定配置'" 
      width="900px" 
      :confirm-loading="submitting"
      @ok="handleSubmit"
    >
      <a-form :model="form" layout="vertical">
        <a-form-item label="配置名称" required>
          <a-input v-model:value="form.name" placeholder="请输入配置名称" :maxlength="100" show-count />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="数据源接口" required>
              <a-select 
                v-model:value="form.dataSourceId" 
                placeholder="选择数据源接口" 
                @change="onDataSourceChange"
                :loading="!dataSources.length"
              >
                <a-select-option v-for="ds in dataSources" :key="ds.id" :value="ds.id">
                  {{ ds.name }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="推送接口" required>
              <a-select 
                v-model:value="form.pushInterfaceId" 
                placeholder="选择推送接口" 
                @change="onPushInterfaceChange"
                :loading="!pushInterfaces.length"
              >
                <a-select-option v-for="pi in pushInterfaces" :key="pi.id" :value="pi.id">
                  {{ pi.name }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="Cron表达式">
              <a-input v-model:value="form.cronExpression" placeholder="0 0/5 * * * ?" />
              <div class="hint">
                例: 0 0/5 * * * ? (每5分钟) | 0 0 * * * ? (每小时) | 0 0 0 * * ? (每天)
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="状态">
              <a-switch v-model:checked="form.enabled" checked-children="启用" un-checked-children="禁用" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="字段绑定">
          <a-alert 
            v-if="!form.dataSourceId || !form.pushInterfaceId" 
            message="请先选择数据源接口和推送接口" 
            type="info" 
            show-icon 
            class="alert-mb"
          />
          <a-table 
            v-else
            :columns="bindingColumns" 
            :data-source="form.fieldBindings" 
            :pagination="false" 
            size="small"
          >
            <template #bodyCell="{ column, record, index }">
              <template v-if="column.key === 'pushParamName'">
                <a-select v-model:value="record.pushParamName" size="small" class="select-full">
                  <a-select-option v-for="param in selectedPushParams" :key="param.name" :value="param.name">
                    {{ param.name }} ({{ param.type }}){{ param.required ? ' *' : '' }}
                  </a-select-option>
                </a-select>
              </template>
              <template v-if="column.key === 'sourceFieldPath'">
                <a-select 
                  v-model:value="record.sourceFieldPath" 
                  size="small" 
                  class="select-full" 
                  allow-clear 
                  placeholder="选择数据源字段"
                >
                  <a-select-option v-for="field in selectedDataSourceFields" :key="field.path" :value="field.path">
                    {{ field.path }} ({{ field.name }})
                  </a-select-option>
                </a-select>
              </template>
              <template v-if="column.key === 'defaultValue'">
                <a-input v-model:value="record.defaultValue" size="small" placeholder="无绑定时使用" />
              </template>
              <template v-if="column.key === 'action'">
                <a-button type="link" size="small" danger @click="removeBinding(index)">删除</a-button>
              </template>
            </template>
          </a-table>
          <a-button 
            type="dashed" 
            block 
            class="block-mt" 
            @click="addBinding"
            :disabled="!form.dataSourceId || !form.pushInterfaceId"
          >
            + 添加绑定
          </a-button>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { bindingApi, dataSourceApi, pushApi } from '../api'

const loading = ref(false)
const submitting = ref(false)
const executingId = ref(null)
const list = ref([])
const dataSources = ref([])
const pushInterfaces = ref([])
const modalVisible = ref(false)
const editingId = ref(null)

const form = reactive({
  name: '',
  dataSourceId: null,
  pushInterfaceId: null,
  fieldBindings: [],
  cronExpression: '',
  enabled: true
})

const columns = [
  { title: '配置名称', dataIndex: 'name', key: 'name' },
  { title: '数据源接口', key: 'dataSource' },
  { title: '推送接口', key: 'pushInterface' },
  { title: 'Cron表达式', dataIndex: 'cronExpression', key: 'cron' },
  { title: '状态', dataIndex: 'enabled', key: 'enabled', width: 80 },
  { title: '最后执行', key: 'lastExecutedAt', width: 180 },
  { title: '操作', key: 'action', width: 200 }
]

const bindingColumns = [
  { title: '推送参数', key: 'pushParamName', width: 200 },
  { title: '数据源字段', key: 'sourceFieldPath', width: 250 },
  { title: '默认值', key: 'defaultValue' },
  { title: '操作', key: 'action', width: 60 }
]

const selectedDataSourceFields = computed(() => {
  const ds = dataSources.value.find(d => d.id === form.dataSourceId)
  return ds?.responseFields || []
})

const selectedPushParams = computed(() => {
  const pi = pushInterfaces.value.find(p => p.id === form.pushInterfaceId)
  return pi?.requestParams || []
})

const getDataSourceName = (id) => dataSources.value.find(d => d.id === id)?.name || '-'
const getPushInterfaceName = (id) => pushInterfaces.value.find(p => p.id === id)?.name || '-'

const fetchList = async () => {
  loading.value = true
  try {
    const [bindingsRes, dsRes, piRes] = await Promise.all([
      bindingApi.list(),
      dataSourceApi.list(),
      pushApi.list()
    ])
    list.value = bindingsRes.data || []
    dataSources.value = dsRes.data || []
    pushInterfaces.value = piRes.data || []
  } catch (e) {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}

const showModal = (record = null) => {
  if (record) {
    editingId.value = record.id
    Object.assign(form, { ...record, fieldBindings: [...(record.fieldBindings || [])] })
  } else {
    editingId.value = null
    Object.assign(form, { 
      name: '', dataSourceId: null, pushInterfaceId: null, 
      fieldBindings: [], cronExpression: '', enabled: true 
    })
  }
  modalVisible.value = true
}

const handleSubmit = async () => {
  if (!form.name?.trim()) {
    message.warning('请输入配置名称')
    return
  }
  if (!form.dataSourceId || !form.pushInterfaceId) {
    message.warning('请选择数据源接口和推送接口')
    return
  }
  
  submitting.value = true
  try {
    if (editingId.value) {
      await bindingApi.update(editingId.value, form)
      message.success('更新成功')
    } else {
      await bindingApi.create(form)
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
    await bindingApi.delete(id)
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

const handleExecute = async (record) => {
  executingId.value = record.id
  try {
    await bindingApi.execute(record.id)
    message.success(`${record.name} 执行成功`)
    fetchList() // 刷新列表以更新最后执行时间
  } catch (e) {
    // 错误已在拦截器中处理
  } finally {
    executingId.value = null
  }
}

const onDataSourceChange = () => {
  // 数据源变更时可以保留现有绑定
}

const onPushInterfaceChange = () => {
  // 推送接口变更时，自动填充参数
  const params = selectedPushParams.value
  form.fieldBindings = params.map(p => ({
    pushParamName: p.name,
    sourceFieldPath: '',
    defaultValue: p.defaultValue || ''
  }))
}

const addBinding = () => {
  form.fieldBindings.push({ pushParamName: '', sourceFieldPath: '', defaultValue: '' })
}

const removeBinding = (index) => {
  form.fieldBindings.splice(index, 1)
}

onMounted(fetchList)
</script>

<style scoped>
.toolbar { margin-bottom: 16px; }
.hint { font-size: 12px; color: #999; margin-top: 4px; }
</style>
