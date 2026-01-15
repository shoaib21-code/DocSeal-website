export function shortFp(fp?: string, left = 8, right = 6) {
if (!fp) return ''
if (fp.length <= left + right) return fp
return `${fp.slice(0, left)}…${fp.slice(-right)}`
}


export function isoToLocal(iso?: string) {
if (!iso) return ''
try {
const d = new Date(iso)
return d.toLocaleString()
} catch { return iso ?? '' }
}