// src/components/CertificatesTable.tsx
import { useEffect, useMemo, useState } from 'react'
import { api, type Cert } from '../lib/api'

// ... (inline SVG icons from your previous version)

export default function CertificatesTable({
  leafOnly = true,
  refreshKey = 0,
  rightActions,                // NEW: custom actions area (e.g., “Generate Certificate”)
}: {
  leafOnly?: boolean
  refreshKey?: number
  rightActions?: React.ReactNode
}) {
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [certs, setCerts] = useState<Cert[]>([])
  const [modalOpen, setModalOpen] = useState(false)
  const [modalTitle, setModalTitle] = useState('')
  const [modalText, setModalText] = useState('')

  useEffect(() => { refresh() }, [leafOnly, refreshKey])
  async function refresh() {
    setError(null); setLoading(true)
    try {
      const certList = await api.getCerts(true)   // supports ?leaf=true
      setCerts(certList)
    } catch (e: any) {
      setError(e?.message ?? 'Failed to load certificates')
    } finally {
      setLoading(false)
    }
  }

  const rows = useMemo(() => {
    return certs
      .map(c => ({
        id: c.id,
        subject: c.subject,
        keyId: (c as any).keyId as number | undefined,
        notBefore: c.notBefore,
        notAfter: c.notAfter,
        createdAt: (c as any).createdAt ?? c.notBefore,
      }))
      .sort((a, b) => b.id - a.id)
  }, [certs])

  function downloadText(filename: string, text: string) {
    const blob = new Blob([text], { type: 'application/x-pem-file' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a'); a.href = url; a.download = filename; a.click()
    URL.revokeObjectURL(url)
  }

  async function viewCert(id: number) {
    try {
      const txt = await api.getCertText(id)
      setModalTitle(`Certificate #${id} — Details`)
      setModalText(txt || '(no details)')
      setModalOpen(true)
    } catch (e: any) { setError(e?.message ?? 'Failed to open certificate') }
  }

  async function downloadCert(id: number) {
    try {
      const pem = await api.getCertPem(id)
      downloadText(`cert-${id}.pem`, pem)
    } catch (e: any) { setError(e?.message ?? 'Download failed') }
  }

  return (
    <div className="mt-2 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="mb-3 flex items-center justify-between">
        <h3 className="text-sm font-semibold">Certificates</h3>

        <div className="flex items-center gap-2">
          {rightActions /* <- your custom header buttons */}
          <button className="btn btn-xs btn-outline p-2" onClick={refresh} aria-label="Refresh" title="Refresh">
            {/* RefreshIcon inline SVG */}
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" className="h-4 w-4">
              <path strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" d="M21 12a9 9 0 1 1-3.4-7l1.9 1.9M21 6v4h-4" />
            </svg>
          </button>
        </div>
      </div>

      {loading ? (
        <div className="text-sm text-slate-500">Loading…</div>
      ) : rows.length === 0 ? (
        <div className="text-sm text-slate-500">No certificates yet.</div>
      ) : (
        <div className="overflow-x-auto">
          <table className="min-w-full text-sm">
            <thead className="text-left text-slate-500">
              <tr>
              
                <th className="py-2 pr-3">Created At</th>
                <th className="py-2 pr-3">Validity</th>
                <th className="py-2 pr-3">Subject</th>
                <th className="py-2 pr-3">Key ID</th>
                <th className="py-2">Actions</th>
              </tr>
            </thead>
            <tbody>
              {rows.map(r => (
                <tr key={r.id} className="border-t">
                  
                  <td className="py-2 pr-3">{r.createdAt ? new Date(r.createdAt).toLocaleString() : '—'}</td>
                  <td className="py-2 pr-3 text-xs">
                    <div>NB: {r.notBefore ? new Date(r.notBefore).toLocaleDateString() : '—'}</div>
                    <div>NA: {r.notAfter ? new Date(r.notAfter).toLocaleDateString() : '—'}</div>
                  </td>
                  <td className="py-2 pr-3 break-all">{r.subject || '—'}</td>
                  <td className="py-2 pr-3">{typeof r.keyId === 'number' ? `#${r.keyId}` : '—'}</td>
                  <td className="py-2 flex flex-wrap gap-2">
                    {/* Eye icon */}
                    <button className="btn btn-xs btn-outline p-2" onClick={() => viewCert(r.id)} aria-label="View" title="View">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" className="h-4 w-4">
                        <path strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" d="M1.5 12s4-7.5 10.5-7.5S22.5 12 22.5 12 18.5 19.5 12 19.5 1.5 12 1.5 12Z" />
                        <circle cx="12" cy="12" r="3" strokeWidth="2" />
                      </svg>
                    </button>
                    {/* Download icon */}
                    <button className="btn btn-xs btn-outline p-2" onClick={() => downloadCert(r.id)} aria-label="Download" title="Download">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" className="h-4 w-4">
                        <path strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" d="M12 3v12m0 0 4-4m-4 4-4-4M4 21h16" />
                      </svg>
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {error && (
        <div className="mt-4 rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-700">
          {error}
        </div>
      )}

      {/* Details modal reuses existing code */}
      {modalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" onClick={() => setModalOpen(false)}>
          <div className="w-full max-w-3xl rounded-2xl bg-white p-5 shadow-xl" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between">
              <h4 className="text-base font-semibold">{modalTitle}</h4>
              <button className="btn btn-sm btn-outline" onClick={() => setModalOpen(false)}>Close</button>
            </div>
            <pre className="mt-3 max-h-[70vh] overflow-auto rounded-lg border border-slate-200 bg-slate-50 p-3 text-xs whitespace-pre-wrap">
{modalText}
            </pre>
          </div>
        </div>
      )}
    </div>
  )
}
