// src/components/KeyCertPicker.tsx
import { useEffect, useState } from 'react'
import { listKeys } from '../lib/api'
import { api, type Cert } from '../lib/api'

type Props = {
  value: { mode: 'key' | 'cert'; id?: string }
  onChange: (v: { mode: 'key' | 'cert'; id?: string }) => void
}

type KeyRow = Awaited<ReturnType<typeof listKeys>>[number]

export default function KeyCertPicker({ value, onChange }: Props) {
  const [keys, setKeys] = useState<KeyRow[]>([])
  const [certs, setCerts] = useState<Cert[]>([])
  const [loading, setLoading] = useState(true)
  const [err, setErr] = useState<string | null>(null)

  useEffect(() => {
    let alive = true
    ;(async () => {
      try {
        setLoading(true)
        const [k, c] = await Promise.all([listKeys(), api.getCerts(true)])
        if (!alive) return
        setKeys(k)
        setCerts(c)
      } catch (e: any) {
        setErr(e.message ?? String(e))
      } finally {
        setLoading(false)
      }
    })()
    return () => { alive = false }
  }, [])

  return (
    <div className="space-y-2">
      <div className="flex items-center gap-4">
        <label className="inline-flex items-center gap-2">
          <input type="radio" checked={value.mode === 'key'} onChange={() => onChange({ mode: 'key', id: undefined })}/>
          <span className="text-sm">Use Key</span>
        </label>
        <label className="inline-flex items-center gap-2">
          <input type="radio" checked={value.mode === 'cert'} onChange={() => onChange({ mode: 'cert', id: undefined })}/>
          <span className="text-sm">Use Certificate</span>
        </label>
      </div>

      {loading && <p className="text-sm opacity-70">Loading…</p>}
      {err && <p className="text-sm text-red-600">{err}</p>}

      {value.mode === 'key' ? (
        <select
          className="w-full rounded-lg border px-3 py-2 text-sm"
          value={value.id ?? ''}
          onChange={(e) => onChange({ mode: 'key', id: e.target.value || undefined })}
        >
          <option value="">— select key —</option>
          {keys.map(k => (
            <option key={k.id} value={String(k.id)}>
              #{k.id} • {k.algorithm}{k.keySize ? `-${k.keySize}` : ''} • {new Date(k.createdAt).toLocaleString()}
            </option>
          ))}
        </select>
      ) : (
        <select
          className="w-full rounded-lg border px-3 py-2 text-sm"
          value={value.id ?? ''}
          onChange={(e) => onChange({ mode: 'cert', id: e.target.value || undefined })}
        >
          <option value="">— select certificate —</option>
          {certs.map(c => (
            <option key={c.id} value={String(c.id)}>
              #{c.id} • {c.subject}
            </option>
          ))}
        </select>
      )}
    </div>
  )
}
