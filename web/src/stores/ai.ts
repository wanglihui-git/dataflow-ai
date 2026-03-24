import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Transform, SimilarInstruction, FeedbackData, GenerateContext, DiagnosisResult } from '@/types'
import { aiApi } from '@/api/ai'

export const useAIStore = defineStore('ai', () => {
  // AI 生成的转换节点
  const generatedTransforms = ref<Transform[]>([])

  // 相似指令列表
  const similarInstructions = ref<SimilarInstruction[]>([])

  // 历史指令
  const instructionHistory = ref<Array<{ id: string; instruction: string; transforms: Transform[]; createdAt: string }>>([])

  // 诊断结果
  const diagnosisResult = ref<DiagnosisResult | null>(null)

  // 加载状态
  const loading = ref(false)

  // 错误信息
  const error = ref<string | null>(null)

  // 当前输入的指令
  const currentInstruction = ref('')

  // 是否正在生成
  const isGenerating = computed(() => loading.value && generatedTransforms.value.length > 0)

  // AI 面板是否可见
  const showAIPanel = ref(false)

  // 生成转换节点
  async function generateTransforms(prompt: string, context?: GenerateContext) {
    loading.value = true
    error.value = null
    try {
      const response = await aiApi.generateTransforms(prompt, context)
      const transforms = response.data || []
      generatedTransforms.value = transforms

      // 添加到历史记录
      instructionHistory.value.unshift({
        id: `history_${Date.now()}`,
        instruction: prompt,
        transforms,
        createdAt: new Date().toISOString()
      })

      return transforms
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'AI 生成转换节点失败'
      return []
    } finally {
      loading.value = false
    }
  }

  // 搜索相似指令
  async function searchSimilar(query: string, limit?: number) {
    loading.value = true
    error.value = null
    try {
      const response = await aiApi.searchSimilar(query, limit)
      similarInstructions.value = response.data || []
      return similarInstructions.value
    } catch (e) {
      error.value = e instanceof Error ? e.message : '搜索相似指令失败'
      return []
    } finally {
      loading.value = false
    }
  }

  // 提交反馈
  async function submitFeedback(aiHelperId: string, feedback: FeedbackData, pipelineId?: string) {
    loading.value = true
    error.value = null
    try {
      await aiApi.submitFeedback(aiHelperId, feedback, pipelineId)
      return true
    } catch (e) {
      error.value = e instanceof Error ? e.message : '提交反馈失败'
      return false
    } finally {
      loading.value = false
    }
  }

  // AI 诊断
  async function diagnose(runId: string) {
    loading.value = true
    error.value = null
    try {
      const response = await aiApi.diagnose(runId)
      diagnosisResult.value = response.data || null
      return diagnosisResult.value
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'AI 诊断失败'
      return null
    } finally {
      loading.value = false
    }
  }

  // 设置当前指令
  function setCurrentInstruction(instruction: string) {
    currentInstruction.value = instruction
  }

  // 清空生成结果
  function clearGenerated() {
    generatedTransforms.value = []
    error.value = null
  }

  // 清空相似指令
  function clearSimilar() {
    similarInstructions.value = []
  }

  // 切换 AI 面板
  function toggleAIPanel() {
    showAIPanel.value = !showAIPanel.value
  }

  // 显示 AI 面板
  function openAIPanel() {
    showAIPanel.value = true
  }

  // 隐藏 AI 面板
  function closeAIPanel() {
    showAIPanel.value = false
  }

  // 清空诊断结果
  function clearDiagnosis() {
    diagnosisResult.value = null
  }

  return {
    generatedTransforms,
    similarInstructions,
    instructionHistory,
    diagnosisResult,
    loading,
    error,
    currentInstruction,
    isGenerating,
    showAIPanel,
    generateTransforms,
    searchSimilar,
    submitFeedback,
    diagnose,
    setCurrentInstruction,
    clearGenerated,
    clearSimilar,
    toggleAIPanel,
    openAIPanel,
    closeAIPanel,
    clearDiagnosis
  }
})