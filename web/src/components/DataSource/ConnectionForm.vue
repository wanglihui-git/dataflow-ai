<template>
  <div class="connection-form">
    <!-- MySQL / PostgreSQL 配置 -->
    <template v-if="['MYSQL', 'POSTGRES'].includes(type)">
      <el-form-item label="主机地址" prop="host">
        <el-input v-model="localConfig.host" placeholder="localhost" />
      </el-form-item>
      <el-form-item label="端口" prop="port">
        <el-input-number v-model="localConfig.port" :min="1" :max="65535" style="width: 100%" />
      </el-form-item>
      <el-form-item label="数据库名称" prop="database">
        <el-input v-model="localConfig.database" placeholder="请输入数据库名称" />
      </el-form-item>
      <el-form-item label="用户名" prop="username">
        <el-input v-model="localConfig.username" placeholder="请输入用户名" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input v-model="localConfig.password" type="password" placeholder="请输入密码" show-password />
      </el-form-item>
      <el-form-item label="SSL 连接">
        <el-switch v-model="localConfig.ssl" />
      </el-form-item>
    </template>

    <!-- API 配置 -->
    <template v-else-if="type === 'API'">
      <el-form-item label="API 地址" prop="url">
        <el-input v-model="localConfig.url" placeholder="https://api.example.com/data" />
      </el-form-item>
      <el-form-item label="请求方法" prop="method">
        <el-select v-model="localConfig.method" style="width: 100%">
          <el-option label="GET" value="GET" />
          <el-option label="POST" value="POST" />
        </el-select>
      </el-form-item>
      <el-form-item label="请求头" prop="headers">
        <el-input
          v-model="headersText"
          type="textarea"
          :rows="3"
          placeholder='{"Content-Type": "application/json"}'
          @blur="updateHeaders"
        />
      </el-form-item>
      <el-form-item label="认证方式">
        <el-select v-model="localConfig.authType" style="width: 100%">
          <el-option label="无" value="none" />
          <el-option label="Basic Auth" value="basic" />
          <el-option label="Bearer Token" value="bearer" />
          <el-option label="API Key" value="apikey" />
        </el-select>
      </el-form-item>
      <template v-if="localConfig.authType === 'basic'">
        <el-form-item label="用户名" prop="authUsername">
          <el-input v-model="localConfig.authUsername" placeholder="用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="authPassword">
          <el-input v-model="localConfig.authPassword" type="password" placeholder="密码" />
        </el-form-item>
      </template>
      <template v-else-if="localConfig.authType === 'bearer'">
        <el-form-item label="Token" prop="authToken">
          <el-input v-model="localConfig.authToken" type="password" placeholder="Bearer Token" />
        </el-form-item>
      </template>
      <template v-else-if="localConfig.authType === 'apikey'">
        <el-form-item label="API Key 名称" prop="apiKeyName">
          <el-input v-model="localConfig.apiKeyName" placeholder="X-API-Key" />
        </el-form-item>
        <el-form-item label="API Key 值" prop="apiKeyValue">
          <el-input v-model="localConfig.apiKeyValue" type="password" placeholder="API Key" />
        </el-form-item>
      </template>
    </template>

    <!-- Kafka 配置 -->
    <template v-else-if="type === 'KAFKA'">
      <el-form-item label="Broker 地址" prop="bootstrapServers">
        <el-input v-model="localConfig.bootstrapServers" placeholder="localhost:9092" />
      </el-form-item>
      <el-form-item label="Topic" prop="topic">
        <el-input v-model="localConfig.topic" placeholder="请输入 Topic 名称" />
      </el-form-item>
      <el-form-item label="消费者组" prop="groupId">
        <el-input v-model="localConfig.groupId" placeholder="consumer-group-1" />
      </el-form-item>
      <el-form-item label="起始位置">
        <el-select v-model="localConfig.autoOffsetReset" style="width: 100%">
          <el-option label="最早" value="earliest" />
          <el-option label="最新" value="latest" />
        </el-select>
      </el-form-item>
      <el-form-item label="安全协议">
        <el-select v-model="localConfig.securityProtocol" style="width: 100%">
          <el-option label="PLAINTEXT" value="PLAINTEXT" />
          <el-option label="SASL_PLAINTEXT" value="SASL_PLAINTEXT" />
          <el-option label="SSL" value="SSL" />
        </el-select>
      </el-form-item>
    </template>

    <!-- CSV 文件配置 -->
    <template v-else-if="type === 'CSV'">
      <el-form-item label="文件路径" prop="filePath">
        <el-input v-model="localConfig.filePath" placeholder="/path/to/your/file.csv" />
      </el-form-item>
      <el-form-item label="编码">
        <el-select v-model="localConfig.encoding" style="width: 100%">
          <el-option label="UTF-8" value="UTF-8" />
          <el-option label="GBK" value="GBK" />
          <el-option label="GB2312" value="GB2312" />
        </el-select>
      </el-form-item>
      <el-form-item label="分隔符">
        <el-input v-model="localConfig.delimiter" placeholder="," style="width: 100px" />
      </el-form-item>
      <el-form-item label="包含表头">
        <el-switch v-model="localConfig.hasHeader" />
      </el-form-item>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, computed } from 'vue'

interface Props {
  modelValue: Record<string, unknown>
  type: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: Record<string, unknown>): void
}>()

const localConfig = reactive<Record<string, unknown>>({
  host: 'localhost',
  port: type === 'MYSQL' ? 3306 : 5432,
  database: '',
  username: '',
  password: '',
  ssl: false,
  url: '',
  method: 'GET',
  headers: {},
  authType: 'none',
  bootstrapServers: 'localhost:9092',
  topic: '',
  groupId: '',
  autoOffsetReset: 'earliest',
  securityProtocol: 'PLAINTEXT',
  filePath: '',
  encoding: 'UTF-8',
  delimiter: ',',
  hasHeader: true,
  ...props.modelValue
})

const type = computed(() => props.type)

const headersText = ref('')

watch(
  () => props.modelValue,
  (newVal) => {
    Object.assign(localConfig, newVal)
    if (newVal.headers) {
      headersText.value = JSON.stringify(newVal.headers, null, 2)
    }
  },
  { immediate: true, deep: true }
)

watch(
  localConfig,
  (newVal) => {
    emit('update:modelValue', { ...newVal })
  },
  { deep: true }
)

const updateHeaders = () => {
  try {
    localConfig.headers = headersText.value ? JSON.parse(headersText.value) : {}
  } catch {
    // ignore parse error
  }
}
</script>

<style scoped>
.connection-form {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
</style>