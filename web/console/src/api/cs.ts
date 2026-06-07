import { http } from './request'

/** 智能客服会话与消息。 */
export interface CsConversation {
  id: number; tenantCode: string | null; userRef: string
  title: string | null; status: string; createdAt: string; updatedAt: string
}
export interface CsMessage {
  id: number; conversationId: number; role: string; content: string; createdAt: string
}
export interface CsStatus { ready: boolean; model: string; kbSize: number }

export const csStatus = (): Promise<CsStatus> => http.get('/cs/status')
export const myConversations = (): Promise<CsConversation[]> => http.get('/cs/conversations/mine')
export const allConversations = (): Promise<CsConversation[]> => http.get('/cs/conversations')
export const conversationMessages = (id: number): Promise<CsMessage[]> =>
  http.get(`/cs/conversations/${id}/messages`)
export const escalateConversation = (id: number): Promise<CsConversation> =>
  http.post(`/cs/${id}/escalate`, {})

export interface ChatHandlers {
  onMeta?: (conversationId: number) => void
  onDelta?: (text: string) => void
  onDone?: () => void
  onError?: (msg: string) => void
}

/**
 * 流式对话：用 fetch 直读 SSE(EventSource 无法带 POST/鉴权头)。
 * 后端事件：meta(会话id) / delta(增量文本) / done。
 */
export async function chatStream(
  body: { conversationId: number | null; question: string },
  h: ChatHandlers,
): Promise<void> {
  const token = localStorage.getItem('codeman.token') || ''
  let resp: Response
  try {
    resp = await fetch('/api/cs/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}`, Accept: 'text/event-stream' },
      body: JSON.stringify(body),
    })
  } catch (e: any) {
    h.onError?.(e?.message || '网络错误')
    return
  }
  if (!resp.ok || !resp.body) {
    h.onError?.(`请求失败(${resp.status})`)
    return
  }
  const reader = resp.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buf = ''
  let event = ''
  let dataLines: string[] = []
  let finished = false
  const flush = () => {
    if (!event && dataLines.length === 0) return
    const data = dataLines.join('\n')   // 多行 data 帧用换行重组，保留原始换行
    if (event === 'meta') {
      try { h.onMeta?.(JSON.parse(data).conversationId) } catch { /* ignore */ }
    } else if (event === 'delta') {
      h.onDelta?.(data)
    } else if (event === 'done') {
      finished = true; h.onDone?.()
    }
    event = ''; dataLines = []
  }
  // 逐块读取并按 SSE 帧(空行分隔)解析
  for (;;) {
    const { value, done } = await reader.read()
    if (done) break
    buf += decoder.decode(value, { stream: true })
    let idx: number
    while ((idx = buf.indexOf('\n')) >= 0) {
      const line = buf.slice(0, idx).replace(/\r$/, '')
      buf = buf.slice(idx + 1)
      if (line === '') { flush(); continue }
      if (line.startsWith('event:')) event = line.slice(6).trim()
      else if (line.startsWith('data:')) dataLines.push(line.slice(5).replace(/^ /, ''))
    }
  }
  flush()
  if (!finished) h.onDone?.()
}
