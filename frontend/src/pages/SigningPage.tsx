// src/pages/SigningPage.tsx
import PdfSignPanel from '../components/PdfSignPanel'

export default function SigningPage() {
  return (
    <div className="mx-auto max-w-3xl px-4 py-8 space-y-6">
      <section className="rounded-2xl border p-5 shadow-sm">
        <PdfSignPanel />
      </section>
    </div>
  )
}

