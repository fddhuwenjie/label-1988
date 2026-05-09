<template>
  <a-config-provider :locale="zhCN">
    <a-layout class="layout">
      <a-layout-sider v-model:collapsed="collapsed" collapsible theme="dark">
        <div class="logo">
          <span v-if="!collapsed">Tare</span>
          <span v-else>T</span>
        </div>
        <a-menu v-if="isRouterReady" :selected-keys="selectedKeys" theme="dark" mode="inline" @click="handleMenuClick">
          <a-menu-item key="push">
            <template #icon><api-outlined /></template>
            <span>推送接口</span>
          </a-menu-item>
          <a-menu-item key="datasource">
            <template #icon><database-outlined /></template>
            <span>数据源接口</span>
          </a-menu-item>
          <a-menu-item key="binding">
            <template #icon><link-outlined /></template>
            <span>接口绑定</span>
          </a-menu-item>
          <a-menu-item key="dataflow">
            <template #icon><branches-outlined /></template>
            <span>数据流视图</span>
          </a-menu-item>
          <a-menu-item key="logs">
            <template #icon><file-text-outlined /></template>
            <span>执行日志</span>
          </a-menu-item>
        </a-menu>
      </a-layout-sider>
      <a-layout>
        <a-layout-header class="header">
          <h2>{{ pageTitle }}</h2>
        </a-layout-header>
        <a-layout-content class="content">
          <router-view />
        </a-layout-content>
      </a-layout>
    </a-layout>
  </a-config-provider>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ApiOutlined, DatabaseOutlined, LinkOutlined, FileTextOutlined, BranchesOutlined } from '@ant-design/icons-vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN'

const router = useRouter()
const route = useRoute()
const collapsed = ref(false)
const isRouterReady = ref(false)

onMounted(async () => {
  await router.isReady()
  isRouterReady.value = true
})

const selectedKeys = computed(() => {
  const key = route.path.split('/')[1] || 'push'
  return [key]
})

const pageTitles = {
  push: '推送接口管理',
  datasource: '数据源接口管理',
  binding: '接口绑定配置',
  dataflow: '数据流视图',
  logs: '执行日志'
}

const pageTitle = computed(() => pageTitles[selectedKeys.value[0]] || 'Tare')

const handleMenuClick = ({ key }) => {
  router.push(`/${key}`)
}
</script>

<style>
.layout {
  min-height: 100vh;
}
.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 24px;
  font-weight: bold;
  background: rgba(255, 255, 255, 0.1);
}
.header {
  background: #fff !important;
  padding: 0 24px;
  display: flex;
  align-items: center;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
}
.header h2 {
  margin: 0;
  font-size: 18px;
}
.content {
  margin: 24px;
  padding: 24px;
  background: #fff;
  min-height: 280px;
  border-radius: 8px;
}
</style>
