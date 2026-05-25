import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as aiApi from '@/api/ai'

export const useAiStore = defineStore('ai', () => {
  const lastInstruction = ref('')
  const lastAiHelperId = ref<string | null>(null)
  const lastNodes = ref<Record<string, unknown>[]>([])
  const similarResults = ref<unknown[]>([])
  const loading = ref(false)

  async function generate(body: Record<string, unknown>) {
    loading.value = true
    try {
      const res = await aiApi.generateTransforms(body)
      lastInstruction.value = (body.instruction as string) || ''
      lastAiHelperId.value = (res.aiHelperId as string) ?? null
      lastNodes.value = (res.nodes as Record<string, unknown>[]) ?? []
      return res
    } finally {
      loading.value = false
    }
  }

  async function searchSimilar(instruction: string) {
    loading.value = true
    try {
      similarResults.value = await aiApi.searchSimilar({ instruction, limit: 5 })
      return similarResults.value
    } finally {
      loading.value = false
    }
  }

  async function feedback(aiHelperId: string, feedbackType: number, comment?: string) {
    await aiApi.submitFeedback({ aiHelperId, feedbackType, comment })
  }

  return {
    lastInstruction,
    lastAiHelperId,
    lastNodes,
    similarResults,
    loading,
    generate,
    searchSimilar,
    feedback
  }
})
