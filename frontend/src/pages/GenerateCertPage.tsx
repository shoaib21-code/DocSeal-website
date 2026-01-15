// src/pages/GenerateCertPage.tsx
import { useEffect, useMemo, useState } from 'react'
import { api, type IssueResp } from '../lib/api'
import type { KeyPair } from '../lib/types'
import CertificatesTable from '../components/CertificatesTable'

type SubjectForm = { CN: string; O: string; OU: string; L: string; ST: string; C: string }
const buildDN = (s: SubjectForm) => ['CN','OU','O','L','ST','C']
  .map(k => (s as any)[k] ? `${k}=${(s as any)[k]}` : '')
  .filter(Boolean).join(',')

export default function GenerateCertPage() {
  const [keys, setKeys] = useState<KeyPair[]>([])
  const [keyId, setKeyId] = useState<number | ''>('')
  const [days, setDays] = useState(365)
  const [subj, setSubj] = useState<SubjectForm>({ CN: '', O: 'DocSeal', OU: 'Users', L: 'Hyderabad', ST: 'TS', C: 'IN' })
  const subjectDn = useMemo(() => buildDN(subj), [subj])

  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [csrId, setCsrId] = useState<number | null>(null)
  const [csrPem, setCsrPem] = useState<string | null>(null)
  const [leaf, setLeaf] = useState<IssueResp | null>(null)

  // table refresh trigger
  const [refreshKey, setRefreshKey] = useState(0)

  // modal visibility
  const [formOpen, setFormOpen] = useState(false)

  useEffect(() => {
    api.getKeys().then(setKeys).catch(e => setError(String(e)))
  }, [])

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null); setBusy(true); setLeaf(null)

    try {
      if (!keyId) throw new Error('Select a key for the CSR.')
      if (!subj.CN) throw new Error('Common Name (CN) is required.')

      const csr = await api.createCsr({ keyId: Number(keyId), subjectDn })
      setCsrId(csr.id)

      try {
        const pem = await api.getCsrPem(csr.id)
        setCsrPem(pem ?? null)
      } catch {}

      const issued = await api.issueCert({ csrId: csr.id, days })
      setLeaf(issued)

      // refresh table + close modal
      setRefreshKey(k => k + 1)
      setFormOpen(false)
    } catch (err: any) {
      setError(err?.message ?? String(err))
    } finally {
      setBusy(false)
    }
  }


  return (
    <div className="mx-auto max-w-6xl px-4 py-10">
      <CertificatesTable
        leafOnly
        refreshKey={refreshKey}
        rightActions={
          <button className="btn btn-primary btn-xs" onClick={() => setFormOpen(true)}>
            Generate Certificate
          </button>
        }
      />

    

      {/* --- Generate Certificate FORM MODAL --- */}
      {formOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" onClick={() => !busy && setFormOpen(false)}>
          <div className="w-full max-w-2xl rounded-2xl bg-white p-6 shadow-xl" onClick={e => e.stopPropagation()}>
            <div className="mb-4 flex items-center justify-between">
              <h2 className="text-lg font-semibold">Generate Certificate</h2>
              <button className="btn btn-sm btn-outline" onClick={() => !busy && setFormOpen(false)} disabled={busy}>Close</button>
            </div>

            {error && <div className="mb-4 rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-700">{error}</div>}

            <form onSubmit={onSubmit} className="grid gap-6">
              <div className="grid gap-4 md:grid-cols-2">
                <label className="flex flex-col gap-1">
                  <span className="text-sm font-medium">Key (for CSR)</span>
                  <select
                    className="rounded-lg border border-slate-300 px-3 py-2"
                    value={keyId}
                    onChange={e => setKeyId(e.target.value ? Number(e.target.value) : '')}
                    required
                  >
                    <option value="">Select key…</option>
                    {keys.map(k => (
                      <option key={k.id} value={k.id}>
                        #{k.id} · {k.algorithm} {k.keySize} · {k.publicKeyFingerprint?.slice(0, 8)}…
                      </option>
                    ))}
                  </select>
                </label>

                <label className="flex flex-col gap-1">
                  <span className="text-sm font-medium">Validity (days)</span>
                  <input
                    type="number" min={1}
                    className="rounded-lg border border-slate-300 px-3 py-2"
                    value={days}
                    onChange={e => setDays(Number(e.target.value || '0'))}
                    required
                  />
                </label>
              </div>

              {/* Subject */}
              <div className="grid gap-4 md:grid-cols-3">
                <TextField label="Common Name (CN)" value={subj.CN} onChange={v => setSubj(s => ({ ...s, CN: v }))} required />
                <TextField label="Organization (O)" value={subj.O} onChange={v => setSubj(s => ({ ...s, O: v }))} />
                <TextField label="Org Unit (OU)" value={subj.OU} onChange={v => setSubj(s => ({ ...s, OU: v }))} />
                <TextField label="Locality (L)" value={subj.L} onChange={v => setSubj(s => ({ ...s, L: v }))} />
                <TextField label="State (ST)" value={subj.ST} onChange={v => setSubj(s => ({ ...s, ST: v }))} />
                <TextField label="Country (C)" value={subj.C} onChange={v => setSubj(s => ({ ...s, C: v }))} maxLength={2} />
              </div>

              <div className="rounded-xl bg-slate-50 p-3 text-sm">
                <div>Subject DN preview:</div>
                <div className="mt-1 font-mono">{subjectDn || '—'}</div>
              </div>

              <div className="flex gap-3">
                <button disabled={busy} className="btn btn-primary">{busy ? 'Working…' : 'Generate certificate'}</button>
                <button
                  type="button"
                  className="btn btn-outline"
                  onClick={() => {
                    if (busy) return
                    setSubj({ CN: '', O: 'DocSeal', OU: 'Users', L: 'Hyderabad', ST: 'TS', C: 'IN' })
                    setKeyId('')
                    setDays(365)
                    setError(null)
                  }}
                  disabled={busy}
                >
                  Reset
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}

function TextField(props: { label: string; value: string; onChange: (v: string) => void; required?: boolean; maxLength?: number }) {
  return (
    <label className="flex flex-col gap-1">
      <span className="text-sm font-medium">{props.label}</span>
      <input
        className="rounded-lg border border-slate-300 px-3 py-2"
        value={props.value}
        onChange={e => props.onChange(e.target.value)}
        required={props.required}
        maxLength={props.maxLength}
      />
    </label>
  )
}
