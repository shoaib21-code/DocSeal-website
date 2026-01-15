import { useEffect, useState } from 'react'


type ToastProps = { message: string, type?: 'success' | 'error', duration?: number }
export default function Toast({ message, type='success', duration=3000 }: ToastProps) {
const [show, setShow] = useState(true)
useEffect(() => { const id = setTimeout(()=>setShow(false), duration); return ()=>clearTimeout(id) }, [duration])
if (!show) return null
return (
<div className={`fixed bottom-4 right-4 z-50 rounded-xl px-4 py-2 text-sm shadow ${type==='success' ? 'bg-green-600 text-white' : 'bg-red-600 text-white'}`}>
{message}
</div>
)
}