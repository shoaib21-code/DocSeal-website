// // src/lib/api.ts
// import type { KeyPair } from './types'

// // ---- base URL handling ----
// const BASE = import.meta.env.VITE_API_BASE_URL ?? ''

// // ---- tiny fetch helper ----
// async function http<T>(url: string, init?: RequestInit): Promise<T> {
//   const res = await fetch(`${BASE}${url}`, {
//     headers: { 'Content-Type': 'application/json', ...(init?.headers ?? {}) },
//     ...init,
//   })
//   if (!res.ok) {
//     const text = await res.text().catch(() => '')
//     throw new Error(text || `HTTP ${res.status}`)
//   }
//   if (res.status === 204) return undefined as unknown as T
//   return res.json() as Promise<T>
// }

// async function httpText(url: string, init?: RequestInit): Promise<string> {
//   const res = await fetch(`${BASE}${url}`, {
//     // don't force JSON headers for plain text
//     ...(init ?? {}),
//   })
//   if (!res.ok) {
//     const text = await res.text().catch(() => '')
//     throw new Error(text || `HTTP ${res.status}`)
//   }
//   return res.text()
// }

// // ---- DTOs for new/existing endpoints ----


// export type CsrCreateBody = {
//   keyId: number
//   subjectDn: string
//   includePem?: boolean // default true
// }

// export type CsrCreateResp = {
//   id: number
//   keyId: number
//   subjectDn: string
//   signatureAlgorithm: string
//   csrPath: string
//   pem?: string
// }

// export type IssueBody = {
//   csrId: number
//   days: number
// }

// export type IssueResp = {
//   id: number
//   serial: string
//   subject: string
//   issuer: string
//   path: string // server-side PEM path
//   // optional extras your backend may add later:
//   notBefore?: string
//   notAfter?: string
//   csrId?: number
//   createdAt?: string
// }

// // ---- List models ----
// export type Csr = {
//   id: number
//   keyId: number
//   subject: string
//   createdAt?: string
// }

// export type Cert = {
//   id: number
//   serial: string
//   subject: string
//   issuer: string
//   notBefore?: string
//   notAfter?: string
//   isCa: boolean   
//   // optional: for CSR linking & "Created At" column
//   csrId?: number
//   createdAt?: string
// }

// // ---- API surface ----
// export const api = {
//   // keys
//   getKeys: () => http<KeyPair[]>('/api/crypto/keys'),
//   generateKey: (bits: number) =>
//     http<KeyPair>(`/api/crypto/generate-keypair?bits=${bits}`, { method: 'POST' }),
//   getKey: (id: number) => http<KeyPair>(`/api/crypto/keys/${id}`),

//   // csr
//   createCsr: (body: CsrCreateBody) =>
//     http<CsrCreateResp>('/api/crypto/csr', {
//       method: 'POST',
//       body: JSON.stringify({ includePem: true, ...body }),
//     }),
//   getCsrs: () => http<Csr[]>('/api/crypto/csr'),
//   getCsrPem: (id: number) => httpText(`/api/crypto/csr/${id}/pem`),
//   getCsrText: (id: number) => httpText(`/api/crypto/csr/${id}/text`),

//   // issue leaf certificate from intermediate CA
//   issueCert: (body: IssueBody) =>
//     http<IssueResp>('/api/crypto/issue', {
//       method: 'POST',
//       body: JSON.stringify(body),
//     }),

//   // certificates
//     getCerts: (leaf?: boolean) =>
//     http<Cert[]>(`/api/crypto/certificates${leaf ? '?leaf=true' : ''}`),
//   getCertText: (id: number) => httpText(`/api/crypto/certificates/${id}/text`),
//   getCertPem:  (id: number) => httpText(`/api/crypto/certificates/${id}/pem`),
// }

// export type { KeyPair }
// export const listKeys = () => api.getKeys()
// export const createCsr = api.createCsr
// export const issueCert = api.issueCert

// // -------- signing: types --------
// export type CmsSignResponse = {
//   signatureId: string
//   signatureCmsB64: string
//   algo: { hash: string; sig: string }
//   chainB64: string[]
// }

// // -------- signing: helpers (multipart + binary) --------
// async function postMultipart<T = any>(url: string, fd: FormData): Promise<T> {
//   const res = await fetch(`${BASE}${url}`, { method: 'POST', body: fd }) // DO NOT set Content-Type
//   if (!res.ok) throw new Error(await res.text().catch(() => `HTTP ${res.status}`))
//   const ct = res.headers.get('content-type') || ''
//   if (ct.includes('application/json')) return res.json() as Promise<T>

//   return res as any
// }

// async function postMultipartBlob(url: string, fd: FormData): Promise<Blob> {
//   const res = await fetch(`${BASE}${url}`, { method: 'POST', body: fd })
//   if (!res.ok) throw new Error(await res.text().catch(() => `HTTP ${res.status}`))
//   return res.blob()
// }

// // -------- signing: API --------

// // PDF signing (embedded, invisible) -> returns signed PDF (Blob)
// export async function signPdf(opts: {
//   file: File
//   keyRef?: string
//   certRef?: string
//   hashAlgo?: string
//   sigAlgo?: string
// }): Promise<Blob> {
//   const fd = new FormData()
//   fd.append('pdfFile', opts.file)
//   if (opts.keyRef)  fd.append('keyRef', opts.keyRef)
//   if (opts.certRef) fd.append('certRef', opts.certRef)
//   if (opts.hashAlgo) fd.append('hashAlgo', opts.hashAlgo)
//   if (opts.sigAlgo)  fd.append('sigAlgo', opts.sigAlgo)
//   return postMultipartBlob('/api/sign/pdf', fd)
// }



// //verification
// export async function verifyPdf(file: File): Promise<PdfVerifyResult> {
//   const fd = new FormData()
//   fd.append('file', file)
//   const res = await fetch(`${BASE}/api/verify/pdf`, { method: 'POST', body: fd })
//   if (!res.ok) throw new Error(await res.text())
//   return res.json()
// }

// // -------- signing: tiny download helpers --------
// export function downloadBlob(filename: string, blob: Blob) {
//   const url = URL.createObjectURL(blob)
//   const a = document.createElement('a')
//   a.href = url
//   a.download = filename
//   document.body.appendChild(a)
//   a.click()
//   a.remove()
//   URL.revokeObjectURL(url)
// }

// export function downloadText(filename: string, text: string) {
//   const blob = new Blob([text], { type: 'text/plain;charset=utf-8' })
//   downloadBlob(filename, blob)
// }

// // -------- verification: types --------
// export type PdfVerifyResult = {
//   valid: boolean
//   byteRangeOK: boolean
//   signerCount: number
//   signers: Array<{
//     subject: string
//     issuer: string
//     serialHex: string
//     notBefore?: string
//     notAfter?: string
//     signingTime?: string
//     alg: { hash: string; sig: string }
//     chainSubjects?: string[] // optional
//   }>
//   errors?: string[]
// }


// src/lib/api.ts
import type { KeyPair } from './types'
import { extractFriendlyError } from './http-helpers'
/* -------------------------------------------------------------------------- */
/*                             Base URL & Helpers                             */
/* -------------------------------------------------------------------------- */

const BASE = import.meta.env.VITE_API_BASE_URL ?? ''

async function http<T>(url: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${url}`, {
    headers: { 'Content-Type': 'application/json', ...(init?.headers ?? {}) },
    ...init,
  })
  if (!res.ok) {
    const text = await res.text().catch(() => '')
    throw new Error(text || `HTTP ${res.status}`)
  }
  if (res.status === 204) return undefined as unknown as T
  return res.json() as Promise<T>
}

async function httpText(url: string, init?: RequestInit): Promise<string> {
  const res = await fetch(`${BASE}${url}`, init ?? {})
  if (!res.ok) {
    const text = await res.text().catch(() => '')
    throw new Error(text || `HTTP ${res.status}`)
  }
  return res.text()
}

// Multipart helpers (do NOT set Content-Type manually)
async function postMultipartBlob(url: string, fd: FormData): Promise<Blob> {
  const res = await fetch(`${BASE}${url}`, { method: 'POST', body: fd })
  if (!res.ok) throw new Error(await res.text().catch(() => `HTTP ${res.status}`))
  return res.blob()
}

/* -------------------------------------------------------------------------- */
/*                                    DTOs                                    */
/* -------------------------------------------------------------------------- */

// CSR
export type CsrCreateBody = {
  keyId: number
  subjectDn: string
  includePem?: boolean // default true
}

export type CsrCreateResp = {
  id: number
  keyId: number
  subjectDn: string
  signatureAlgorithm: string
  csrPath: string
  pem?: string
}

// Issuance
export type IssueBody = {
  csrId: number
  days: number
}

export type IssueResp = {
  id: number
  serial: string
  subject: string
  issuer: string
  path: string
  notBefore?: string
  notAfter?: string
  csrId?: number
  createdAt?: string
}

// Lists
export type Csr = {
  id: number
  keyId: number
  subject: string
  createdAt?: string
}

export type Cert = {
  id: number
  serial: string
  subject: string
  issuer: string
  notBefore?: string
  notAfter?: string
  isCa: boolean
  csrId?: number
  createdAt?: string
}

// Signing (CMS response reserved for potential raw CMS flows)
export type CmsSignResponse = {
  signatureId: string
  signatureCmsB64: string
  algo: { hash: string; sig: string }
  chainB64: string[]
}

// Verification
export type PdfVerifyResult = {
  valid: boolean
  byteRangeOK: boolean
  signerCount: number
  signers: Array<{
    subject: string
    issuer: string
    serialHex: string
    notBefore?: string
    notAfter?: string
    signingTime?: string
    alg: { hash: string; sig: string }
    chainSubjects?: string[]
  }>
  errors?: string[]
}

/* -------------------------------------------------------------------------- */
/*                                Crypto (REST)                               */
/* -------------------------------------------------------------------------- */

export const api = {
  // keys
  getKeys: () => http<KeyPair[]>('/api/crypto/keys'),
  generateKey: (bits: number) =>
    http<KeyPair>(`/api/crypto/generate-keypair?bits=${bits}`, { method: 'POST' }),
  getKey: (id: number) => http<KeyPair>(`/api/crypto/keys/${id}`),

  // csr
  createCsr: (body: CsrCreateBody) =>
    http<CsrCreateResp>('/api/crypto/csr', {
      method: 'POST',
      body: JSON.stringify({ includePem: true, ...body }),
    }),
  getCsrs: () => http<Csr[]>('/api/crypto/csr'),
  getCsrPem: (id: number) => httpText(`/api/crypto/csr/${id}/pem`),
  getCsrText: (id: number) => httpText(`/api/crypto/csr/${id}/text`),

  // issuance
  issueCert: (body: IssueBody) =>
    http<IssueResp>('/api/crypto/issue', {
      method: 'POST',
      body: JSON.stringify(body),
    }),

  // certificates
  getCerts: (leaf?: boolean) =>
    http<Cert[]>(`/api/crypto/certificates${leaf ? '?leaf=true' : ''}`),
  getCertText: (id: number) => httpText(`/api/crypto/certificates/${id}/text`),
  getCertPem: (id: number) => httpText(`/api/crypto/certificates/${id}/pem`),
}

/* -------------------------------------------------------------------------- */
/*                           Signing / Verification API                        */
/* -------------------------------------------------------------------------- */

// PDF signing (embedded, invisible) -> returns signed PDF (Blob)
export async function signPdf(opts: {
  file: File
  keyRef?: string
  certRef?: string
  hashAlgo?: string
  sigAlgo?: string
}): Promise<Blob> {
  const fd = new FormData()
  fd.append('pdfFile', opts.file)
  if (opts.keyRef) fd.append('keyRef', opts.keyRef)
  if (opts.certRef) fd.append('certRef', opts.certRef)
  if (opts.hashAlgo) fd.append('hashAlgo', opts.hashAlgo)
  if (opts.sigAlgo) fd.append('sigAlgo', opts.sigAlgo)
  const res = await fetch(`${BASE}/api/sign/pdf`, { method: 'POST', body: fd })
  if (!res.ok) throw new Error(await extractFriendlyError(res))
  // console.log(res.blob())
    return res.blob()
}

// PDF verification (embedded CMS)
export async function verifyPdf(file: File): Promise<PdfVerifyResult> {
  const fd = new FormData()
  fd.append('file', file)
  const res = await fetch(`${BASE}/api/verify/pdf`, { method: 'POST', body: fd })
  if (!res.ok) throw new Error(await extractFriendlyError(res))
  return res.json()
}

/* -------------------------------------------------------------------------- */
/*                              Download Utilities                             */
/* -------------------------------------------------------------------------- */

export function downloadBlob(filename: string, blob: Blob) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}

export function downloadText(filename: string, text: string) {
  const blob = new Blob([text], { type: 'text/plain;charset=utf-8' })
  downloadBlob(filename, blob)
}

/* -------------------------------------------------------------------------- */
/*                              Convenience Exports                            */
/* -------------------------------------------------------------------------- */

export type { KeyPair }
export const listKeys = () => api.getKeys()
export const createCsr = api.createCsr
export const issueCert = api.issueCert


