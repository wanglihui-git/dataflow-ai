import http, { unwrap } from './index'
import type { ApiResponse } from '@/types'

export async function generateTransforms(body: Record<string, unknown>) {
  const res = await http.post<ApiResponse<Record<string, unknown>>>('/v1/ai/generate-transforms', body)
  return unwrap(res)
}

export async function searchSimilar(body: Record<string, unknown>) {
  const res = await http.post<ApiResponse<unknown[]>>('/v1/ai/search-similar', body)
  return unwrap(res)
}

export async function submitFeedback(body: Record<string, unknown>) {
  await http.post('/v1/ai/feedback', body)
}
