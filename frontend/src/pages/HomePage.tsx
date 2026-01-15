// src/routes/HomePage.tsx
import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import AnimatedGradient from '../components/visuals/AnimatedGradient'
import FloatingArt from '../components/visuals/FloatingArt'
import Badge from '../components/common/Badge'
import { FileSignature, FileSearch, KeyRound, FileBadge2 } from 'lucide-react'

const fadeUp = (d = 0) => ({
    initial: { opacity: 0, y: 16 },
    whileInView: { opacity: 1, y: 0 },
    viewport: { once: true },
    transition: { duration: .6, delay: d }
})

export default function HomePage() {
    return (
        <div className="relative">
            {/* HERO */}
            <section className="relative overflow-hidden">
                <AnimatedGradient />
                <div className="mx-auto grid max-w-7xl items-center gap-8 px-4 py-20 md:grid-cols-2 md:py-28">
                    <div>
                        <motion.h1 {...fadeUp(0)} className="text-4xl font-extrabold tracking-tight text-slate-900 md:text-5xl">
                            DocSeal — where documents earn trust
                        </motion.h1>

                        <motion.p {...fadeUp(.1)} className="mt-4 max-w-xl text-lg text-slate-600">
                            Create keys, issue certificates, and now <span className="font-semibold text-slate-800">sign &amp; verify PDFs</span> with an embedded, invisible signature.
                        </motion.p>

                        <motion.div {...fadeUp(.2)} className="mt-8 flex flex-wrap gap-3">
                            <Link to="/signing" className="btn btn-primary inline-flex items-center gap-2">
                                <FileSignature className="h-4 w-4" /> Sign a PDF
                            </Link>
                            <Link to="/verify" className="btn btn-outline inline-flex items-center gap-2">
                                <FileSearch className="h-4 w-4" /> Verify a PDF
                            </Link>
                            <Link to="/keys" className="btn btn-ghost inline-flex items-center gap-2">
                                <KeyRound className="h-4 w-4" /> Manage Keys
                            </Link>
                        </motion.div>

                        {/* Trust badges */}
                        <motion.ul {...fadeUp(.3)} className="mt-8 flex flex-wrap gap-3 text-sm">
                            <li><Badge >Embedded sign </Badge></li>
                            <li><Badge >Single signature per file</Badge></li>
                            <li><Badge >Verify: Data &amp; digest</Badge></li>
                        </motion.ul>
                    </div>

                    <motion.div {...fadeUp(.15)} className="relative mx-auto md:justify-self-end">
                        <FloatingArt />
                    </motion.div>
                </div>
            </section>

            {/* FEATURE CARDS */}
            <section className="mx-auto max-w-7xl px-4 py-14">
                <div className="grid gap-5 md:grid-cols-3">
                    {[
                        {
                            t: 'Generate Keys',
                            d: 'Create RSA key pairs for signing workflows.',
                            icon: <KeyRound className="h-5 w-5 text-slate-500" />
                        },
                        {
                            t: 'Issue Certificates',
                            d: 'Create CSRs and issue development certificates.',
                            icon: <FileBadge2 className="h-5 w-5 text-slate-500" />
                        },
                        {
                            t: 'Sign & Verify PDFs',
                            d: 'Embedded, invisible signature with verification UI.',
                            icon: <FileSignature className="h-5 w-5 text-slate-500" />
                        },
                    ].map((c, i) => (
                        <motion.div key={c.t} {...fadeUp(i * 0.05)} className="card">
                            <div className="flex items-center gap-2">
                                {c.icon}
                                <h3 className="text-lg font-semibold">{c.t}</h3>
                            </div>
                            <p className="mt-2 text-sm text-slate-600">{c.d}</p>
                            {c.t === 'Sign & Verify PDFs' && (
                                <div className="mt-4 flex gap-2">
                                    <Link to="/sign" className="btn btn-xs btn-primary">Sign</Link>
                                    <Link to="/verify" className="btn btn-xs btn-outline">Verify</Link>
                                </div>
                            )}
                        </motion.div>
                    ))}
                </div>
            </section>

            {/* HOW IT WORKS */}
            <section id="how" className="mx-auto max-w-7xl px-4 pb-16">
                <motion.h2 {...fadeUp(0)} className="text-2xl font-bold">How it works</motion.h2>
                <div className="mt-6 grid gap-5 md:grid-cols-5">
                    {[
                        { n: 1, t: 'Generate', d: 'Create an RSA key pair.' },
                        { n: 2, t: 'CSR', d: 'Build a certificate signing request.' },
                        { n: 3, t: 'Issue', d: 'Issue a development certificate.' },
                        { n: 4, t: 'Sign', d: 'Apply an embedded, invisible signature.' },
                        { n: 5, t: 'Verify', d: 'Check ByteRange, digest, and signer info.' },
                    ].map((s, i) => (
                        <motion.div key={s.n} {...fadeUp(.05 + i * 0.05)} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                            <div className="mb-2 inline-flex h-8 w-8 items-center justify-center rounded-full bg-blue-600 text-sm font-semibold text-white">{s.n}</div>
                            <div className="font-semibold">{s.t}</div>
                            <div className="text-sm text-slate-600">{s.d}</div>
                        </motion.div>
                    ))}
                </div>
            </section>

            {/* API DEMO TERMINAL */}
            <section className="mx-auto max-w-7xl px-4 pb-20">
                <motion.div {...fadeUp(0)} className="rounded-2xl border border-slate-200 bg-slate-900 p-4 text-slate-100 shadow-lg">
                    {`# Sign a PDF (returns signed PDF)
$ curl -sS -X POST \\
  -F "pdfFile=@input.pdf" \\
  -F "keyRef=1" \\
  "http://localhost:8080/api/sign/pdf" -o output-signed.pdf

# Verify a signed PDF
$ curl -sS -X POST -F "file=@output-signed.pdf" http://localhost:8080/api/verify/pdf
{ "valid": true, "byteRangeOK": true, "signerCount": 1, "signers": [ ... ] }`}
                </motion.div>

                <motion.div {...fadeUp(.15)} className="mt-6 flex flex-wrap justify-center gap-3">
                    <Link to="/signing" className="btn btn-primary">Open signer</Link>
                    <Link to="/verify" className="btn btn-outline">Open verifier</Link>
                </motion.div>
            </section>
        </div>
    )
}
