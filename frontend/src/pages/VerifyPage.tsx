// src/pages/VerifyPage.tsx
import PdfVerifyPanel from '../components/PdfVerifyPanel'

export default function VerifyPage() {
  return (
    <div className="mx-auto max-w-3xl px-4 py-8 space-y-6">
      <section className="rounded-2xl border p-5 shadow-sm">
        <h2 className="text-lg font-medium mb-3">Verify Signed Document</h2>
        <PdfVerifyPanel />
      </section>
    </div>
  )
}
