// src/App.tsx
import { Outlet, NavLink, Link } from 'react-router-dom'

export default function App() {
  const nav = (isActive: boolean) =>
    `px-3 py-2 rounded-lg hover:bg-slate-100 ${
      isActive ? 'font-semibold text-blue-700 bg-blue-50' : 'text-slate-600'
    }`

  return (
    <div className="min-h-dvh flex flex-col bg-gradient-to-b from-white to-slate-50 text-slate-900">
      {/* NAVBAR (glass) */}
      <nav className="sticky top-0 z-40 border-b border-slate-200/70 bg-white/70 backdrop-blur supports-[backdrop-filter]:bg-white/60">
        <div className="mx-auto max-w-7xl px-4">
          <div className="flex h-16 items-center justify-between">
            <Link to="/" className="flex items-center gap-3">
              <img src="/logo.svg" alt="DocSeal" className="h-8 w-8" />
              <span className="text-lg font-semibold">DocSeal</span>
            </Link>
            <div className="flex items-center gap-1 text-sm">
              <NavLink to="/" end className={({isActive}) => nav(isActive)}>Home</NavLink>
              <NavLink to="/certificates" className={({isActive}) => nav(isActive)}>Certificates</NavLink>
              <NavLink to="/signing" className={({isActive}) => nav(isActive)}>Signing</NavLink>
              <NavLink to="/verify"  className={({isActive}) => nav(isActive)}>Verify</NavLink>
              <NavLink to="/about" className={({isActive}) => nav(isActive)}>About</NavLink>
              <Link to="/keys" className="ml-2 rounded-xl bg-blue-600 px-4 py-2 font-medium text-white shadow hover:bg-blue-700">Keys</Link>
            </div>
          </div>
        </div>
      </nav>

      {/* CONTENT */}
      <main className="flex-1">
        <Outlet />
      </main>

      {/* FOOTER */}
      <footer className="border-t bg-white">
        <div className="mx-auto max-w-7xl px-4 py-6 text-center text-sm text-slate-500">
          © {new Date().getFullYear()} DocSeal · where documents earn trust · Private keys never leave the server
        </div>
      </footer>
    </div>
  )
}
