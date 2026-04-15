import { useState } from 'react'
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { Sidebar } from './components/layout/Sidebar'
import { Header } from './components/layout/Header'
import { BuscaPage } from './pages/BuscaPage'
import { DashboardPage } from './pages/DashboardPage'
import { DependenciasPage } from './pages/DependenciasPage'
import { SettingsPage } from './pages/SettingsPage'
import { ToastProvider } from './components/Toast/ToastContext'
import { ToastContainer } from './components/Toast/ToastContainer'
import './App.css'

function App() {
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false)

  return (
    <ToastProvider>
      <BrowserRouter>
        <ToastContainer />
        <div className={`app-layout${sidebarCollapsed ? ' app-layout--collapsed' : ''}`}>
          <Sidebar
            collapsed={sidebarCollapsed}
            onToggle={() => setSidebarCollapsed((v) => !v)}
          />
          <div className="app-main">
            <Header />
            <main className="app-content">
              <Routes>
                <Route path="/" element={<DashboardPage />} />
                <Route path="/dependencias" element={<DependenciasPage />} />
                <Route path="/busca" element={<BuscaPage />} />
                <Route path="/settings" element={<SettingsPage />} />
              </Routes>
            </main>
          </div>
        </div>
      </BrowserRouter>
    </ToastProvider>
  )
}

export default App
