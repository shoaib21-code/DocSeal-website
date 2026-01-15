// src/components/PdfSignPanel.tsx
import { useState } from 'react'
import KeyCertPicker from './KeyCertPicker'
import { signPdf, downloadBlob } from '../lib/api'
import { FileSignature, KeyRound, CheckCircle2, AlertCircle } from 'lucide-react'

const HASHES = ['SHA-256', 'SHA-384', 'SHA-512'] as const
const SIGS = ['RSA-PSS', 'RSA'] as const

export default function PdfSignPanel() {
  const [file, setFile] = useState<File | null>(null)
  const [kc, setKc] = useState<{ mode: 'key' | 'cert'; id?: string }>({ mode: 'key' })
  const [hash, setHash] = useState<(typeof HASHES)[number]>('SHA-256')
  const [sig, setSig] = useState<(typeof SIGS)[number]>('RSA-PSS')
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState<string | null>(null)
  const [done, setDone] = useState<string | null>(null)

  const canSubmit = !!file && !!kc.id && !busy

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault()
    setErr(null); setDone(null)
    if (!file) return setErr('Choose a PDF file')
    if (!kc.id) return setErr('Select a Key or Certificate')

    setBusy(true)
    try {
      const blob = await signPdf({
        file,
        keyRef: kc.mode === 'key' ? kc.id : undefined,
        certRef: kc.mode === 'cert' ? kc.id : undefined,
        hashAlgo: hash,
        sigAlgo: sig,
      })
      const out = suggestSignedName(file.name)
      downloadBlob(out, blob)
      setDone(`Downloaded ${out}`)
    } catch (e: any) {
      setErr(e?.message ?? String(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="max-w-3xl mx-auto">
      {/* header */}
      <header className="mb-5 flex items-center gap-3">
        <div className="rounded-2xl bg-blue-50 ring-1 ring-blue-100 p-2">
          <FileSignature className="h-5 w-5 text-blue-700" />
        </div>
        <div>
          <h2 className="text-lg font-semibold text-slate-900">Sign PDF</h2>
          <p className="text-sm text-slate-500">
Adds an invisible, embedded signature to your PDF.          </p>
        </div>
      </header>

      {/* card */}
      <form
        className="rounded-2xl bg-white shadow-sm ring-1 ring-slate-200 p-4 md:p-5 space-y-4"
        onSubmit={onSubmit}
      >
        {/* file */}
        <div>
          <label className="mb-1 block text-sm font-medium text-slate-700">PDF File</label>
          <input
            type="file"
            accept="application/pdf"
            className="block w-full rounded-xl border border-slate-200 bg-white p-2 text-sm text-slate-900 file:mr-4 file:rounded-lg file:border-0 file:bg-slate-100 file:px-3 file:py-1.5 hover:border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-200"
            onChange={(e) => setFile(e.target.files?.[0] ?? null)}
            disabled={busy}
          />
          {file && (
            <div className="mt-1 text-xs text-slate-500 truncate">
              Selected: <span className="font-medium">{file.name}</span>
            </div>
          )}
        </div>

        {/* signer */}
        <div>
          <label className="mb-1 block text-sm font-medium text-slate-700">Signer</label>
          <div className="rounded-xl bg-slate-50 p-3 ring-1 ring-slate-200">
            <div className="flex items-center gap-2 mb-2">
              <KeyRound className="h-4 w-4 text-slate-600" />
              <span className="text-sm text-slate-700">Pick a key or certificate</span>
            </div>
            <KeyCertPicker value={kc} onChange={setKc} />
          </div>
          {!kc.id && <p className="mt-1 text-xs text-amber-700">Select a key/cert to continue.</p>}
        </div>

        {/* algorithms */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Hash</label>
            <select
              className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm hover:border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-200"
              value={hash}
              onChange={(e) => setHash(e.target.value as typeof HASHES[number])}
              disabled={busy}
            >
              {HASHES.map((h) => (
                <option key={h} value={h}>{h}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Signature</label>
            <select
              className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm hover:border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-200"
              value={sig}
              onChange={(e) => setSig(e.target.value as typeof SIGS[number])}
              disabled={busy}
            >
              {SIGS.map((s) => (
                <option key={s} value={s}>{s}</option>
              ))}
            </select>
            <p className="mt-1 text-xs text-slate-500">
              RSA-PSS is recommended. Ensure the certificate allows digitalSignature/nonRepudiation.
            </p>
          </div>
        </div>

        {/* messages */}
        {err && (
          <div className="rounded-xl bg-red-50 text-red-800 text-sm px-3 py-2 ring-1 ring-red-100 flex items-start gap-2">
            <AlertCircle className="h-4 w-4 mt-0.5" />
            <span>{err}</span>
          </div>
        )}
        {done && (
          <div className="rounded-xl bg-green-50 text-green-800 text-sm px-3 py-2 ring-1 ring-green-100 flex items-start gap-2">
            <CheckCircle2 className="h-4 w-4 mt-0.5" />
            <span>{done}</span>
          </div>
        )}

        {/* actions */}
        <div className="flex items-center gap-2">
          <button
            type="submit"
            disabled={!canSubmit}
            className="rounded-xl bg-blue-600 px-4 py-2 text-white text-sm font-medium shadow-sm hover:bg-blue-700 disabled:opacity-60 disabled:cursor-not-allowed"
          >
            {busy ? 'Signing…' : 'Sign PDF'}
          </button>
          {!canSubmit && (
            <span className="text-xs text-slate-500">Select file and signer to enable.</span>
          )}
        </div>
      </form>
    </div>
  )
}

function suggestSignedName(name: string) {
  const dot = name.lastIndexOf('.')
  const base = dot > 0 ? name.slice(0, dot) : name
  return `${base}-signed.pdf`
}
