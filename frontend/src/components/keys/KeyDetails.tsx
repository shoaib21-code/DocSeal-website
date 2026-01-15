import type { KeyPair } from '../../lib/types'
import { isoToLocal } from '../../lib/format'


export default function KeyDetails({ keypair }: { keypair: KeyPair | null }) {
    if (!keypair) return null
    return (
        <div className="card space-y-2">
            <h3 className="text-base font-semibold">Key Details</h3>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                <div>
                    <div className="text-xs text-gray-500">ID</div>
                    <div className="font-mono text-sm">{keypair.id}</div>
                </div>
                <div>
                    <div className="text-xs text-gray-500">Algorithm</div>
                    <div className="text-sm">{keypair.algorithm}</div>
                </div>
                <div>
                    <div className="text-xs text-gray-500">Key Size</div>
                    <div className="text-sm">{keypair.keySize ?? '-'}</div>
                </div>
                <div>
                    <div className="text-xs text-gray-500">Created</div>
                    <div className="text-sm">{isoToLocal(keypair.createdAt)}</div>
                </div>
                <div className="sm:col-span-2">
                    <div className="text-xs text-gray-500">Public Fingerprint</div>
                    <div className="font-mono text-sm break-all">{keypair.publicKeyFingerprint}</div>
                </div>
                <div className="sm:col-span-2">
                    <div className="text-xs text-gray-500">Public Key Path</div>
                    <div className="font-mono text-xs break-all">{keypair.publicKeyPath}</div>
                </div>
            </div>
        </div>
    )
}