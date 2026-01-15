import { useEffect, useMemo, useState } from 'react'
import { api } from '../lib/api'
import type { KeyPair } from '../lib/types'
import KeyTable from '../components/keys/KeyTable'
import KeyGenerateModal from '../components/keys/KeyGenerateModal'
import KeyDetails from '../components/keys/KeyDetails'
import Toast from '../components/common/Toast'

export default function KeysPage() {
    const [items, setItems] = useState<KeyPair[]>([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string>()
    const [showNew, setShowNew] = useState(false)
    const [active, setActive] = useState<KeyPair | null>(null)
    const [toast, setToast] = useState<{ msg: string, type: 'success' | 'error' } | null>(null)


    useEffect(() => {
        (async () => {
            try {
                const data = await api.getKeys()
                setItems(data)
            } catch (e: any) {
                setError(e?.message || 'Failed to load keys')
            } finally { setLoading(false) }
        })()
    }, [])


    const onGenerate = async (bits: number) => {
        const kp = await api.generateKey(bits)
        setItems(prev => [kp, ...prev])
        setToast({ msg: 'Key pair created', type: 'success' })
        setActive(kp)
    }


    const content = useMemo(() => {
        if (loading) return <div className="card">Loading…</div>
        if (error) return <div className="card text-red-600">{error}</div>
        return <KeyTable items={items} onOpen={(k) => setActive(k)} />
    }, [loading, error, items])


    return (
        <div className="space-y-6">
            <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                    <h1 className="text-2xl font-semibold">Keys</h1>
                    <p className="text-sm text-gray-600">
                        Generate and manage asymmetric key pairs. Private keys never leave the server — because that’s how documents earn trust.
                    </p>                </div>
                <div className="flex items-center gap-2">
                    <button className="btn btn-primary" onClick={() => setShowNew(true)}>Generate Key Pair</button>
                </div>
            </div>


            {content}


            {active && <KeyDetails keypair={active} />}


            <KeyGenerateModal open={showNew} onClose={() => setShowNew(false)} onSubmit={onGenerate} />


            {toast && <Toast message={toast.msg} type={toast.type} />}
        </div>
    )
}