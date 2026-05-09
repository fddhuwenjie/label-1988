# Tare 数据推送管理系统 API 文档

## 概述

本文档描述了 Tare 数据推送管理系统的 RESTful API 接口。

**基础URL**: `/api`

**响应格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

**错误响应**:
```json
{
  "code": 400,
  "message": "错误信息",
  "data": {
    "errorCode": "VALIDATION_ERROR",
    "fields": {
      "name": "名称不能为空"
    }
  }
}
```

---

## 数据源接口 (Data Source)

### 获取数据源列表
```
GET /api/data-sources
```

**响应示例**:
```json
{
  "code": 200,
  "data": [
    {
      "id": "ds-001",
      "name": "用户信息接口",
      "url": "https://api.example.com/users",
      "method": "GET",
      "headers": {"Authorization": "Bearer xxx"},
      "responseFields": [
        {"path": "data.id", "name": "用户ID", "type": "string"}
      ],
      "postProcessor": "extract:data",
      "enabled": true
    }
  ]
}
```

### 创建数据源
```
POST /api/data-sources
Content-Type: application/json
```

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 是 | 接口名称，最大100字符 |
| url | string | 是 | 接口地址，必须以http://或https://开头 |
| method | string | 是 | 请求方法：GET/POST |
| headers | object | 否 | 请求头 |
| requestBody | object | 否 | 请求体(POST时使用) |
| responseFields | array | 否 | 返回字段定义 |
| postProcessor | string | 否 | 后置处理脚本 |
| enabled | boolean | 否 | 是否启用，默认true |

### 更新数据源
```
PUT /api/data-sources/{id}
```

### 删除数据源
```
DELETE /api/data-sources/{id}
```

---

## 推送接口 (Push Interface)

### 获取推送接口列表
```
GET /api/push-interfaces
```

### 创建推送接口
```
POST /api/push-interfaces
```

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 是 | 接口名称 |
| url | string | 是 | 接口地址 |
| method | string | 是 | 请求方法：GET/POST/PUT/DELETE |
| headers | object | 否 | 请求头 |
| requestParams | array | 否 | 请求参数定义 |
| enabled | boolean | 否 | 是否启用 |

**requestParams 结构**:
```json
{
  "name": "userId",
  "type": "string",
  "description": "用户ID",
  "required": true,
  "defaultValue": ""
}
```

---

## 绑定配置 (Binding)

### 获取绑定配置列表
```
GET /api/bindings
```

### 创建绑定配置
```
POST /api/bindings
```

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 是 | 配置名称 |
| dataSourceId | string | 是 | 数据源接口ID |
| pushInterfaceId | string | 是 | 推送接口ID |
| fieldBindings | array | 否 | 字段绑定配置 |
| cronExpression | string | 否 | Cron表达式 |
| enabled | boolean | 否 | 是否启用 |

**fieldBindings 结构**:
```json
{
  "pushParamName": "userId",
  "sourceFieldPath": "data.id",
  "defaultValue": "default"
}
```

**Cron表达式示例**:
- `0 0/5 * * * ?` - 每5分钟
- `0 0 * * * ?` - 每小时
- `0 0 0 * * ?` - 每天零点
- `0 0 9 * * MON-FRI` - 工作日9点

### 手动执行绑定
```
POST /api/bindings/{id}/execute
```

---

## 执行日志 (Execution Log)

### 获取执行日志列表
```
GET /api/execution-logs?current=1&size=10&bindingId=xxx&status=SUCCESS
```

**查询参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| current | int | 页码，默认1 |
| size | int | 每页条数，默认10 |
| bindingId | string | 绑定配置ID筛选 |
| status | string | 状态筛选：SUCCESS/FAILED |

### 获取执行统计
```
GET /api/execution-logs/stats
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "total": 100,
    "success": 95,
    "failed": 5,
    "avgDuration": 150.5
  }
}
```

---

## 后置处理脚本

数据源接口支持后置处理脚本，用于对返回数据进行转换。

### 简单指令

```
extract:data.items      # 提取指定路径的数据
map:oldField->newField  # 字段重命名
```

### JavaScript脚本

```javascript
// data 变量包含原始响应数据
var result = data.items.filter(function(item) {
  return item.status === 'active';
});
// 将处理结果赋值给 result 变量
```

---

## 错误码

| 错误码 | HTTP状态码 | 说明 |
|--------|-----------|------|
| VALIDATION_ERROR | 400 | 参数校验失败 |
| NOT_FOUND | 404 | 资源不存在 |
| INVALID_PARAM | 400 | 参数无效 |
| EXECUTION_FAILED | 500 | 执行失败 |
| INTERNAL_ERROR | 500 | 系统内部错误 |
