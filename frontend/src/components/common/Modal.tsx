import { useEffect } from 'react'


type ModalProps = {
open: boolean
onClose: () => void
title: string
children: React.ReactNode
}


export default function Modal({ open, onClose, title, children }: ModalProps) {
useEffect(() => {
const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose() }
if (open) window.addEventListener('keydown', onKey)
return () => window.removeEventListener('keydown', onKey)
}, [open, onClose])


if (!open) return null
return (
<div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" role="dialog" aria-modal>
<div className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-xl">
<div className="mb-4 flex items-center justify-between">
<h2 className="text-lg font-semibold">{title}</h2>
<button className="btn btn-outline px-3 py-1" onClick={onClose}>Close</button>
</div>
{children}
</div>
</div>
)
}