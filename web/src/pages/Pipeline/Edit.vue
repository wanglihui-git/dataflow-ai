<template>
  <PipelineEditor
    v-if="ready"
    :pipeline-id="pipelineId"
    v-model:name="name"
    :source="source"
    :sink="sink"
    :transforms="transforms"
    :data-sources="dataSources"
    :saving="saving"
    :running="running"
    :saved-updated-at="savedUpdatedAt"
    @update:source="source = $event"
    @update:sink="sink = $event"
    @update:transforms="transforms = $event"
    @save="handleSave"
    @run="handleRun"
    @preview="handlePreview"
    @back="handleBack"
  />
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as pipelineApi from '@/api/pipeline'
import * as dataSourceApi from '@/api/dataSource'
import PipelineEditor from '@/components/PipelineEditor/index.vue'
import type { DataSource, SinkConfig, SourceConfig, Transform } from '@/types'

const route = useRoute()
const router = useRouter()
const isCreate = route.name === 'PipelineCreate'
const pipelineId = ref(isCreate ? undefined : (route.params.id as string))

const ready = ref(isCreate)
const name = ref('新管道')
const description = ref('')
const source = ref<SourceConfig>({})
const sink = ref<SinkConfig>({ writeMode: 'APPEND', batchSize: 1000 })
const transforms = ref<Transform[]>([])
const dataSources = ref<DataSource[]>([])
const saving = ref(false)
const running = ref(false)
const savedUpdatedAt = ref<string>()

async function loadDataSources() {
  const res = await dataSourceApi.listDataSources()
  dataSources.value = res.content
}

async function loadPipeline() {
  if (!pipelineId.value) return
  const p = await pipelineApi.getPipeline(pipelineId.value)
  name.value = p.name
  description.value = p.description || ''
  source.value = p.source || {}
  sink.value = p.sink || { writeMode: 'APPEND' }
  transforms.value = p.transforms || []
  savedUpdatedAt.value = p.updatedAt
  ready.value = true
}

async function handleSave(publish: boolean) {
  saving.value = true
  try {
    if (pipelineId.value) {
      const latest = await pipelineApi.getPipeline(pipelineId.value)
      if (savedUpdatedAt.value && latest.updatedAt !== savedUpdatedAt.value) {
        await ElMessageBox.confirm('管道已被他人修改，是否覆盖保存？', '冲突提示', {
          type: 'warning'
        })
      }
      await pipelineApi.updatePipeline(pipelineId.value, {
        name: name.value,
        description: description.value,
        source: source.value,
        sink: sink.value,
        transforms: transforms.value,
        status: publish ? 'active' : 'draft'
      })
      savedUpdatedAt.value = (await pipelineApi.getPipeline(pipelineId.value)).updatedAt
    } else {
      const created = await pipelineApi.createPipeline({
        name: name.value,
        description: description.value,
        source: source.value,
        sink: sink.value,
        transforms: transforms.value,
        status: publish ? 'active' : 'draft',
        schedule: { scheduleType: 'MANUAL', enabled: false }
      })
      pipelineId.value = created.id
      router.replace(`/pipelines/${created.id}/edit`)
      savedUpdatedAt.value = created.updatedAt
    }
    ElMessage.success(publish ? '已发布' : '草稿已保存')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleRun() {
  if (!pipelineId.value) {
    await handleSave(true)
  }
  if (!pipelineId.value) return
  running.value = true
  try {
    const run = await pipelineApi.runPipeline(pipelineId.value)
    router.push(`/executions/${run.id}`)
  } finally {
    running.value = false
  }
}

async function handlePreview() {
  if (!pipelineId.value) {
    ElMessage.warning('请先保存')
    return
  }
  const res = await pipelineApi.previewPipeline(pipelineId.value)
  ElMessageBox.alert(JSON.stringify(res, null, 2), '预览结果', { customClass: 'wide-msg' })
}

function handleBack() {
  if (pipelineId.value) router.push(`/pipelines/${pipelineId.value}`)
  else router.push('/pipelines')
}

onMounted(async () => {
  await loadDataSources()
  if (isCreate) ready.value = true
  else await loadPipeline()
})
</script>
