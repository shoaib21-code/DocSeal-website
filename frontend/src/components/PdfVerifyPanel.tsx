// src/components/PdfVerifyPanel.tsx
import { useState } from 'react'
import { verifyPdf, type PdfVerifyResult } from '../lib/api'
import { CheckCircle2, XCircle, ShieldCheck, FileSignature } from 'lucide-react'

export default function PdfVerifyPanel() {
    const [file, setFile] = useState<File | null>(null)
    const [busy, setBusy] = useState(false)
    const [err, setErr] = useState<string | null>(null)
    const [res, setRes] = useState<PdfVerifyResult | null>(null)

    async function onSubmit(e: React.FormEvent) {
        e.preventDefault()
        setErr(null)
        setRes(null)
        if (!file) return setErr('Choose a PDF file')

        setBusy(true)
        try {
            const r = await verifyPdf(file)
            setRes(r)
        } catch (e: any) {
            setErr(e?.message ?? String(e))
        } finally {
            setBusy(false)
        }
    }

    return (
        <div className="max-w-3xl mx-auto">
            <header className="mb-5 flex items-center gap-3">
                <div className="rounded-2xl bg-blue-50 ring-1 ring-blue-100 p-2">
                    <FileSignature className="h-5 w-5 text-blue-700" />
                </div>
                <div>
                    <h2 className="text-lg font-semibold text-slate-900">Verify PDF Signature</h2>
                    <p className="text-sm text-slate-500">Uploads never leave your machine beyond this request; private keys never leave the server.</p>
                </div>
            </header>

            <form
                className="rounded-2xl bg-white shadow-sm ring-1 ring-slate-200 p-4 md:p-5 space-y-4"
                onSubmit={onSubmit}
            >
                <div>
                    <label className="mb-1 block text-sm font-medium text-slate-700">PDF File</label>
                    <input
                        type="file"
                        accept="application/pdf"
                        className="block w-full rounded-xl border border-slate-200 bg-white p-2 text-sm text-slate-900 file:mr-4 file:rounded-lg file:border-0 file:bg-slate-100 file:px-3 file:py-1.5 hover:border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-200"
                        onChange={(e) => setFile(e.target.files?.[0] ?? null)}
                        disabled={busy}
                    />
                </div>

                {err && (
                    <div className="rounded-xl bg-red-50 text-red-800 text-sm px-3 py-2 ring-1 ring-red-100">
                        {err}
                    </div>
                )}

                <div className="flex items-center gap-2">
                    <button
                        type="submit"
                        disabled={busy || !file}
                        className="rounded-xl bg-blue-600 px-4 py-2 text-white text-sm font-medium shadow-sm hover:bg-blue-700 disabled:opacity-60 disabled:cursor-not-allowed"
                    >
                        {busy ? 'Verifying…' : 'Verify PDF'}
                    </button>
                    {file && !busy && (
                        <span className="text-xs text-slate-500 truncate max-w-[60%]">{file.name}</span>
                    )}
                </div>

                {res && <VerifySummaryPdf res={res} />}
            </form>
        </div>
    )
}

function Badge({ ok }: { ok: boolean }) {
    return ok ? (
        <span className="inline-flex items-center gap-1 rounded-full bg-green-50 px-2 py-0.5 text-xs font-medium text-green-700 ring-1 ring-green-100">
            <CheckCircle2 className="h-3.5 w-3.5" /> VALID
        </span>
    ) : (
        <span className="inline-flex items-center gap-1 rounded-full bg-red-50 px-2 py-0.5 text-xs font-medium text-red-700 ring-1 ring-red-100">
            <XCircle className="h-3.5 w-3.5" /> INVALID
        </span>
    )
}

function Row({ label, value, danger = false }: { label: string; value: string; danger?: boolean }) {
    return (
        <div className="flex items-center justify-between text-sm">
            <span className="text-slate-600">{label}</span>
            <span className={danger ? 'text-red-700' : 'text-slate-900'}>{value}</span>
        </div>
    )
}

function VerifySummaryPdf({ res }: { res: PdfVerifyResult }) {
    const overall = res.valid
    return (
        <section className="mt-4 rounded-2xl bg-slate-50 p-4 ring-1 ring-slate-200/80">
            <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                    <div className="rounded-xl bg-white ring-1 ring-slate-200 p-1.5">
                        <ShieldCheck className={`h-4 w-4 ${overall ? 'text-green-700' : 'text-red-700'}`} />
                    </div>
                    <div className="text-sm font-medium text-slate-900">Verification Summary</div>
                </div>
                <Badge ok={overall} />
            </div>

            <div className="mt-3 space-y-2">
                {/* <Row label="ByteRange" value={res.byteRangeOK ? 'OK' : 'BROKEN'} danger={!res.byteRangeOK} /> */}
                <Row label="Signers" value={String(res.signerCount)} />
            </div>

            <div className="mt-4">
                <div className="text-sm font-medium text-slate-900">Signers</div>
                <ul className="mt-2 grid gap-3 sm:grid-cols-2">
                    {(res.signers ?? []).map((s, i) => (
                        <li
                            key={i}
                            className="rounded-2xl bg-white p-3 ring-1 ring-slate-200 hover:ring-slate-300 transition"
                        >
                            <div className="text-sm font-semibold text-slate-900 break-words">{s.subject}</div>
                            <div className="mt-0.5 text-xs text-slate-500 break-words">Issuer: {s.issuer}</div>
                            <div className="mt-0.5 text-xs text-slate-500">Serial: {s.serialHex}</div>
                            <div className="mt-0.5 text-xs text-slate-500">
                                Validity: {s.notBefore ?? '—'} → {s.notAfter ?? '—'}
                            </div>
                            {s.signingTime && (
                                <div className="mt-0.5 text-xs text-slate-500">
                                    SigningTime: {new Date(s.signingTime).toLocaleString()}
                                </div>
                            )}
                            <div className="mt-1 text-xs text-slate-600">Alg: {s.alg?.sig} / {s.alg?.hash}</div>

                            {s.chainSubjects?.length ? (
                                <details className="mt-2">
                                    <summary className="cursor-pointer text-xs text-slate-600 hover:text-slate-800">
                                        Chain subjects ({s.chainSubjects.length})
                                    </summary>
                                    <ul className="mt-1 ml-4 list-disc text-xs text-slate-600">
                                        {s.chainSubjects.map((cs, j) => (
                                            <li key={j} className="break-words">{cs}</li>
                                        ))}
                                    </ul>
                                </details>
                            ) : null}
                        </li>
                    ))}
                </ul>
            </div>

            {res.errors?.length ? (
                <div className="mt-4 rounded-xl bg-amber-50 px-3 py-2 text-amber-800 ring-1 ring-amber-100">
                    <div className="text-sm font-medium">Warnings</div>
                    <ul className="mt-1 list-disc pl-5 text-xs">
                        {res.errors.map((e, i) => (
                            <li key={i} className="break-words">{e}</li>
                        ))}
                    </ul>
                </div>
            ) : null}
        </section>
    )
}
