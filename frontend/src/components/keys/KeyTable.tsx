import type { KeyPair } from '../../lib/types'
import { shortFp, isoToLocal } from '../../lib/format'


type Props = { items: KeyPair[], onOpen: (k: KeyPair) => void }


export default function KeyTable({ items, onOpen }: Props) {
    if (!items?.length) {
        return (
            <div className="card">
                <p className="text-sm text-gray-600">No keys yet. Click <span className="font-medium">Generate Key Pair</span> to create one.</p>
            </div>
        )
    }
    return (
        <div className="card overflow-x-auto">
            <table className="table min-w-[600px]">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Algorithm</th>
                        <th>Size</th>
                        <th>Created</th>
                        <th>Fingerprint</th>
                        <th></th>
                    </tr>
                </thead>
                <tbody>
                    {items.map(k => (
                        <tr key={k.id} className="hover:bg-gray-50">
                            <td className="font-mono">{k.id}</td>
                            <td>{k.algorithm}</td>
                            <td>{k.keySize ?? '-'}</td>
                            <td>{isoToLocal(k.createdAt)}</td>
                            <td><span className="badge font-mono">{shortFp(k.publicKeyFingerprint)}</span></td>
                            <td className="text-right">
                                <button className="btn btn-outline" onClick={() => onOpen(k)}>Open</button>
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    )
}