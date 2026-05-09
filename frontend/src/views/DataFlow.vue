<template>
  <div class="data-flow-container">
    <div class="toolbar">
      <a-button @click="handleRefresh">
        <reload-outlined /> 刷新
      </a-button>
      <a-button class="btn-toolbar-gap" @click="handleResetLayout">
        <layout-outlined /> 重置布局
      </a-button>
    </div>
    
    <div class="flow-wrapper" ref="flowWrapper">
      <VueFlow
        v-model:nodes="nodes"
        v-model:edges="edges"
        :fit-view="true"
        :nodes-draggable="true"
        :edges-updatable="false"
        @node-click="handleNodeClick"
        @edge-click="handleEdgeClick"
        @node-drag-stop="handleNodeDragStop"
      >
        <Background :gap="16" />
        <Controls />
        <MiniMap :node-styles="miniMapNodeStyles" />
      </VueFlow>
    </div>

    <a-modal
      v-model:open="nodeDetailVisible"
      :title="getNodeDetailTitle()"
      width="700px"
      :footer="null"
    >
      <a-descriptions :column="2" bordered v-if="selectedNode">
        <a-descriptions-item label="名称">{{ selectedNode.data.name }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="selectedNode.data.enabled ? 'green' : 'red'">
            {{ selectedNode.data.enabled ? '启用' : '禁用' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="URL" :span="2">{{ selectedNode.data.url }}</a-descriptions-item>
        <a-descriptions-item label="方法">
          {{ selectedNode.data.method }}
        </a-descriptions-item>
        <a-descriptions-item label="描述" :span="2">
          {{ selectedNode.data.description || '-' }}
        </a-descriptions-item>
      </a-descriptions>
      
      <a-divider v-if="selectedNode?.type === 'dataSource'">响应字段</a-divider>
      <a-table
        v-if="selectedNode?.type === 'dataSource' && selectedNode.data.responseFields?.length"
        :columns="responseFieldColumns"
        :data-source="selectedNode.data.responseFields"
        :pagination="false"
        size="small"
      />
      <a-empty v-else-if="selectedNode?.type === 'dataSource'" description="暂无响应字段" />
      
      <a-divider v-if="selectedNode?.type === 'pushInterface'">请求参数</a-divider>
      <a-table
        v-if="selectedNode?.type === 'pushInterface' && selectedNode.data.requestParams?.length"
        :columns="requestParamColumns"
        :data-source="selectedNode.data.requestParams"
        :pagination="false"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'required'">
            <a-tag :color="record.required ? 'red' : 'default'">
              {{ record.required ? '必填' : '可选' }}
            </a-tag>
          </template>
        </template>
      </a-table>
      <a-empty v-else-if="selectedNode?.type === 'pushInterface'" description="暂无请求参数" />
    </a-modal>

    <a-modal
      v-model:open="fieldMappingVisible"
      title="编辑字段映射"
      width="800px"
      :confirm-loading="submitting"
      @ok="handleSaveFieldMapping"
    >
      <a-descriptions :column="2" bordered class="binding-info">
        <a-descriptions-item label="绑定配置">{{ selectedBinding?.name }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="selectedBinding?.enabled ? 'green' : 'red'">
            {{ selectedBinding?.enabled ? '启用' : '禁用' }}
          </a-tag>
        </a-descriptions-item>
      </a-descriptions>
      
      <a-divider>字段映射关系</a-divider>
      
      <a-table
        :columns="fieldMappingColumns"
        :data-source="editingFieldBindings"
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
            <a-button type="link" size="small" danger @click="removeFieldBinding(index)">删除</a-button>
          </template>
        </template>
      </a-table>
      
      <a-button
        type="dashed"
        block
        class="block-mt"
        @click="addFieldBinding"
      >
        + 添加绑定
      </a-button>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, h } from 'vue'
import { message } from 'ant-design-vue'
import {
  VueFlow,
  Background,
  Controls,
  MiniMap,
  useVueFlow,
  MarkerType
} from '@vue-flow/core'
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import { ReloadOutlined, LayoutOutlined } from '@ant-design/icons-vue'
import { bindingApi, dataSourceApi, pushApi } from '../api'

const { fitView } = useVueFlow()

const flowWrapper = ref(null)
const loading = ref(false)
const submitting = ref(false)
const nodes = ref([])
const edges = ref([])
const dataSources = ref([])
const pushInterfaces = ref([])
const bindings = ref([])

const nodeDetailVisible = ref(false)
const selectedNode = ref(null)

const fieldMappingVisible = ref(false)
const selectedBinding = ref(null)
const editingFieldBindings = ref([])

const miniMapNodeStyles = {
  input: { fill: '#1890ff' },
  output: { fill: '#52c41a' },
  default: { fill: '#faad14' }
}

const responseFieldColumns = [
  { title: '字段路径', dataIndex: 'path', key: 'path', width: 200 },
  { title: '字段名', dataIndex: 'name', key: 'name', width: 150 },
  { title: '类型', dataIndex: 'type', key: 'type', width: 100 },
  { title: '描述', dataIndex: 'description', key: 'description' }
]

const requestParamColumns = [
  { title: '参数名', dataIndex: 'name', key: 'name', width: 150 },
  { title: '类型', dataIndex: 'type', key: 'type', width: 100 },
  { title: '是否必填', key: 'required', width: 80 },
  { title: '默认值', dataIndex: 'defaultValue', key: 'defaultValue', width: 120 },
  { title: '描述', dataIndex: 'description', key: 'description' }
]

const fieldMappingColumns = [
  { title: '推送参数', key: 'pushParamName', width: 200 },
  { title: '数据源字段', key: 'sourceFieldPath', width: 250 },
  { title: '默认值', key: 'defaultValue' },
  { title: '操作', key: 'action', width: 60 }
]

const selectedDataSourceFields = computed(() => {
  if (!selectedBinding.value) return []
  const ds = dataSources.value.find(d => d.id === selectedBinding.value.dataSourceId)
  return ds?.responseFields || []
})

const selectedPushParams = computed(() => {
  if (!selectedBinding.value) return []
  const pi = pushInterfaces.value.find(p => p.id === selectedBinding.value.pushInterfaceId)
  return pi?.requestParams || []
})

const getNodeDetailTitle = () => {
  if (!selectedNode.value) return '详情'
  if (selectedNode.value.type === 'input') return '数据源接口详情'
  if (selectedNode.value.type === 'output') return '推送接口详情'
  return '数据转换详情'
}

const fetchData = async () => {
  loading.value = true
  try {
    const [bindingsRes, dsRes, piRes] = await Promise.all([
      bindingApi.list(),
      dataSourceApi.list(),
      pushApi.list()
    ])
    bindings.value = bindingsRes.data || []
    dataSources.value = dsRes.data || []
    pushInterfaces.value = piRes.data || []
    buildFlowGraph()
  } catch (e) {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}

const buildFlowGraph = () => {
  const newNodes = []
  const newEdges = []
  
  const usedDataSourceIds = new Set()
  const usedPushInterfaceIds = new Set()
  
  bindings.value.forEach((binding, bindingIndex) => {
    usedDataSourceIds.add(binding.dataSourceId)
    usedPushInterfaceIds.add(binding.pushInterfaceId)
    
    const dataSource = dataSources.value.find(ds => ds.id === binding.dataSourceId)
    const pushInterface = pushInterfaces.value.find(pi => pi.id === binding.pushInterfaceId)
    
    const baseX = 100 + bindingIndex * 400
    const baseY = 100
    
    const dsNodeId = `ds-${binding.dataSourceId}`
    const transformNodeId = `transform-${binding.id}`
    const piNodeId = `pi-${binding.pushInterfaceId}`
    
    if (!newNodes.find(n => n.id === dsNodeId)) {
      newNodes.push({
        id: dsNodeId,
        type: 'input',
        position: { x: baseX, y: baseY },
        data: {
          label: h('div', { style: 'text-align: center;' }, [
            h('div', { style: 'font-weight: bold; font-size: 14px; color: #1890ff;' }, dataSource?.name || '数据源'),
            h('div', { style: 'font-size: 12px; margin-top: 4px;' }, [
              h('span', { style: `display: inline-block; padding: 2px 8px; border-radius: 4px; background: ${dataSource?.enabled ? '#f6ffed' : '#fff2f0'}; color: ${dataSource?.enabled ? '#52c41a' : '#ff4d4f'};` }, dataSource?.enabled ? '启用' : '禁用')
            ]),
            h('div', { style: 'font-size: 11px; color: #999; margin-top: 4px;' }, `方法: ${dataSource?.method || 'GET'}`)
          ]),
          ...dataSource,
          id: binding.dataSourceId,
          type: 'dataSource'
        }
      })
    }
    
    newNodes.push({
      id: transformNodeId,
      type: 'default',
      position: { x: baseX + 250, y: baseY },
      data: {
        label: h('div', { style: 'text-align: center;' }, [
          h('div', { style: 'font-weight: bold; font-size: 14px; color: #faad14;' }, '🔄 数据转换'),
          h('div', { style: 'font-size: 12px; margin-top: 4px; color: #666;' }, binding.name),
          h('div', { style: 'font-size: 11px; color: #999; margin-top: 4px;' }, `字段: ${(binding.fieldBindings || []).length} 个`)
        ]),
        bindingId: binding.id,
        bindingName: binding.name,
        fieldCount: (binding.fieldBindings || []).length
      }
    })
    
    if (!newNodes.find(n => n.id === piNodeId)) {
      newNodes.push({
        id: piNodeId,
        type: 'output',
        position: { x: baseX + 500, y: baseY },
        data: {
          label: h('div', { style: 'text-align: center;' }, [
            h('div', { style: 'font-weight: bold; font-size: 14px; color: #52c41a;' }, pushInterface?.name || '推送接口'),
            h('div', { style: 'font-size: 12px; margin-top: 4px;' }, [
              h('span', { style: `display: inline-block; padding: 2px 8px; border-radius: 4px; background: ${pushInterface?.enabled ? '#f6ffed' : '#fff2f0'}; color: ${pushInterface?.enabled ? '#52c41a' : '#ff4d4f'};` }, pushInterface?.enabled ? '启用' : '禁用')
            ]),
            h('div', { style: 'font-size: 11px; color: #999; margin-top: 4px;' }, `方法: ${pushInterface?.method || 'POST'}`)
          ]),
          ...pushInterface,
          id: binding.pushInterfaceId,
          type: 'pushInterface'
        }
      })
    }
    
    const fieldCount = (binding.fieldBindings || []).length
    const label = fieldCount > 0 ? `字段映射 (${fieldCount})` : '无映射'
    
    newEdges.push({
      id: `edge-ds-${binding.dataSourceId}-transform-${binding.id}`,
      source: dsNodeId,
      target: transformNodeId,
      animated: true,
      markerEnd: {
        type: MarkerType.ArrowClosed
      },
      style: { stroke: '#1890ff', strokeWidth: 2 },
      data: {
        bindingId: binding.id,
        label: '获取数据'
      }
    })
    
    newEdges.push({
      id: `edge-transform-${binding.id}-pi-${binding.pushInterfaceId}`,
      source: transformNodeId,
      target: piNodeId,
      animated: true,
      markerEnd: {
        type: MarkerType.ArrowClosed
      },
      style: { stroke: '#52c41a', strokeWidth: 2 },
      label: label,
      labelStyle: { fill: '#52c41a', fontWeight: 'bold', fontSize: '12px' },
      data: {
        bindingId: binding.id,
        fieldCount: fieldCount
      }
    })
  })
  
  dataSources.value.forEach(ds => {
    if (!usedDataSourceIds.has(ds.id)) {
      newNodes.push({
        id: `ds-${ds.id}`,
        type: 'input',
        position: { x: 100, y: 100 + newNodes.filter(n => n.type === 'input').length * 200 },
        data: {
          label: h('div', { style: 'text-align: center;' }, [
            h('div', { style: 'font-weight: bold; font-size: 14px; color: #1890ff;' }, ds.name),
            h('div', { style: 'font-size: 12px; margin-top: 4px;' }, [
              h('span', { style: `display: inline-block; padding: 2px 8px; border-radius: 4px; background: ${ds.enabled ? '#f6ffed' : '#fff2f0'}; color: ${ds.enabled ? '#52c41a' : '#ff4d4f'};` }, ds.enabled ? '启用' : '禁用')
            ]),
            h('div', { style: 'font-size: 11px; color: #999; margin-top: 4px;' }, `方法: ${ds.method || 'GET'}`)
          ]),
          ...ds,
          id: ds.id,
          type: 'dataSource'
        }
      })
    }
  })
  
  pushInterfaces.value.forEach(pi => {
    if (!usedPushInterfaceIds.has(pi.id)) {
      newNodes.push({
        id: `pi-${pi.id}`,
        type: 'output',
        position: { x: 600, y: 100 + newNodes.filter(n => n.type === 'output').length * 200 },
        data: {
          label: h('div', { style: 'text-align: center;' }, [
            h('div', { style: 'font-weight: bold; font-size: 14px; color: #52c41a;' }, pi.name),
            h('div', { style: 'font-size: 12px; margin-top: 4px;' }, [
              h('span', { style: `display: inline-block; padding: 2px 8px; border-radius: 4px; background: ${pi.enabled ? '#f6ffed' : '#fff2f0'}; color: ${pi.enabled ? '#52c41a' : '#ff4d4f'};` }, pi.enabled ? '启用' : '禁用')
            ]),
            h('div', { style: 'font-size: 11px; color: #999; margin-top: 4px;' }, `方法: ${pi.method || 'POST'}`)
          ]),
          ...pi,
          id: pi.id,
          type: 'pushInterface'
        }
      })
    }
  })
  
  nodes.value = newNodes
  edges.value = newEdges
  
  setTimeout(() => {
    fitView({ padding: 0.2 })
  }, 100)
}

const handleNodeClick = (event, node) => {
  if (node.data.type === 'dataSource' || node.data.type === 'pushInterface') {
    selectedNode.value = node
    nodeDetailVisible.value = true
  }
}

const handleEdgeClick = (event, edge) => {
  const bindingId = edge.data?.bindingId
  if (bindingId) {
    const binding = bindings.value.find(b => b.id === bindingId)
    if (binding) {
      selectedBinding.value = binding
      editingFieldBindings.value = [...(binding.fieldBindings || [])]
      fieldMappingVisible.value = true
    }
  }
}

const handleNodeDragStop = () => {
  // 节点位置已通过 v-model 自动更新
}

const handleRefresh = () => {
  fetchData()
  message.success('刷新成功')
}

const handleResetLayout = () => {
  buildFlowGraph()
  message.success('布局已重置')
}

const handleSaveFieldMapping = async () => {
  if (!selectedBinding.value) return
  
  submitting.value = true
  try {
    const updateData = {
      ...selectedBinding.value,
      fieldBindings: editingFieldBindings.value
    }
    await bindingApi.update(selectedBinding.value.id, updateData)
    message.success('字段映射保存成功')
    fieldMappingVisible.value = false
    fetchData()
  } catch (e) {
    // 错误已在拦截器中处理
  } finally {
    submitting.value = false
  }
}

const addFieldBinding = () => {
  editingFieldBindings.value.push({
    pushParamName: '',
    sourceFieldPath: '',
    defaultValue: ''
  })
}

const removeFieldBinding = (index) => {
  editingFieldBindings.value.splice(index, 1)
}

onMounted(fetchData)
</script>

<style scoped>
.data-flow-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.toolbar {
  margin-bottom: 16px;
}

.btn-toolbar-gap {
  margin-left: 8px;
}

.flow-wrapper {
  flex: 1;
  min-height: 500px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  overflow: hidden;
}

:deep(.vue-flow__node-input) {
  border: 2px solid #1890ff;
  border-radius: 8px;
  background: #fff;
  padding: 8px 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

:deep(.vue-flow__node-output) {
  border: 2px solid #52c41a;
  border-radius: 8px;
  background: #fff;
  padding: 8px 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

:deep(.vue-flow__node-default) {
  border: 2px solid #faad14;
  border-radius: 8px;
  background: #fff;
  padding: 8px 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

:deep(.vue-flow__node.selected) {
  box-shadow: 0 0 0 2px #1890ff, 0 2px 8px rgba(0, 0, 0, 0.15);
}

:deep(.vue-flow__handle) {
  width: 12px;
  height: 12px;
  background: #fff;
  border: 2px solid #1890ff;
  border-radius: 50%;
}

:deep(.vue-flow__node-input .vue-flow__handle) {
  border-color: #1890ff;
}

:deep(.vue-flow__node-output .vue-flow__handle) {
  border-color: #52c41a;
}

:deep(.vue-flow__node-default .vue-flow__handle) {
  border-color: #faad14;
}

.binding-info {
  margin-bottom: 16px;
}

.select-full {
  width: 100%;
}

.block-mt {
  margin-top: 12px;
}
</style>
