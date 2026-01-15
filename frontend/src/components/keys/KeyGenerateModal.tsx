import { useState } from 'react'
import Modal from '../common/Modal'
import Spinner from '../common/Spinner'


interface Props {
    open: boolean
    onClose: () => void
    onSubmit: (bits: number) => Promise<void>
}


export default function KeyGenerateModal({ open, onClose, onSubmit }: Props) {
    const [bits, setBits] = useState(2048)
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState<string | undefined>()


    const handleCreate = async () => {
        setError(undefined)
        setLoading(true)
        try {
            await onSubmit(bits)
            onClose()
        } catch (e: any) {
            setError(e?.message || 'Failed to generate key')
        } finally {
            setLoading(false)
        }
    }


    return (
        <Modal open={open} onClose={onClose} title="Generate Key Pair">
            <div className="space-y-4">
                <div>
                    <label className="mb-1 block text-sm font-medium">Algorithm</label>
                    <select className="select" value="RSA" disabled>
                        <option>RSA</option>
                    </select>
                </div>
                <div>
                    <label className="mb-1 block text-sm font-medium">Key Size</label>
                    <select className="select" value={bits} onChange={e => setBits(Number(e.target.value))}>
                        <option value={2048}>2048</option>
                        <option value={3072}>3072</option>
                        <option value={4096}>4096</option>
                    </select>
                </div>
                {error && <p className="text-sm text-red-600">{error}</p>}
                <div className="flex items-center justify-end gap-2">
                    <button className="btn btn-outline" onClick={onClose} disabled={loading}>Cancel</button>
                    <button className="btn btn-primary" onClick={handleCreate} disabled={loading}>
                        {loading ? <><Spinner /> Creating…</> : 'Create Key Pair'}
                    </button>
                </div>
            </div>
        </Modal>
    )
}