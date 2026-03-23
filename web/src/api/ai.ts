import api from './index'
import type { Transform, SimilarInstruction, FeedbackData, GenerateContext, DiagnosisResult } from '@/types'

export const aiApi = {
  generateTransforms(prompt: string, context?: GenerateContext) {
    return api.post<Transform[]>('/v1/ai/generate-transforms', {
      instruction: prompt,
      context,
      options: {
        maxNodes: context?.options?.maxNodes || 10,
        strict: context?.options?.strict ?? true
      }
    })
  },
  searchSimilar(query: string, limit?: number, minSimilarity?: number) {
    return api.post<SimilarInstruction[]>('/v1/ai/search-similar', {
      instruction: query,
      limit: limit || 5,
      minSimilarity: minSimilarity || 0.8
    })
  },
  submitFeedback(aiHelperId: string, feedback: FeedbackData, pipelineId?: string) {
    return api.post('/v1/ai/feedback', {
      aiHelperId,
      action: feedback.helpful ? 'accept' : 'reject',
      pipelineId
    })
  },
  diagnose(runId: string) {
    return api.post<DiagnosisResult>('/v1/ai/diagnose', { runId })
  }
}