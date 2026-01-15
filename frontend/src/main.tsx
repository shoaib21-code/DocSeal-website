import React from 'react'
import ReactDOM from 'react-dom/client'
import { createBrowserRouter, RouterProvider } from 'react-router-dom'
import './index.css'
import App from './App'
import HomePage from './pages/HomePage'
import KeysPage from './pages/KeysPage'
import AboutPage from './pages/AboutPage'
import NotFoundPage from './pages/NotFoundPage'
import GenerateCertPage from './pages/GenerateCertPage'
import SigningPage from './pages/SigningPage'
import VerifyPage from './pages/VerifyPage'
const router = createBrowserRouter([
    {
        path: '/',
        element: <App />,
        children: [
            { index: true, element: <HomePage /> },
            { path: 'keys', element: <KeysPage /> },
            { path: 'about', element: <AboutPage /> },
            { path: 'signing', element: <SigningPage /> },
            { path: '*', element: <NotFoundPage /> },
            { path: 'verify', element: <VerifyPage /> },
            { path: 'certificates', element: <GenerateCertPage /> },
        ],
    },
])


ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
        <RouterProvider router={router} />
    </React.StrictMode>
)