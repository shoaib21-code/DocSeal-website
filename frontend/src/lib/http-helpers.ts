// src/lib/http-helpers.ts (optional)
export async function extractFriendlyError(res: Response) {
  const text = await res.text().catch(() => '')
  try {
    const j = JSON.parse(text)
    if (j?.error) return j.hint ? `${j.error} ${j.hint}` : j.error
  } catch { /* not JSON */ }
  return text || `HTTP ${res.status}`
}
