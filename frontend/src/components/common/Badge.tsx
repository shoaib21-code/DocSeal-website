// src/components/common/Badge.tsx
export default function Badge({ children }: { children: React.ReactNode }) {
  return (
    <span className="inline-flex items-center rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-800 ring-1 ring-inset ring-slate-300">
      {children}
    </span>
  )
}
