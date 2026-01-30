import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import { ArrowLeft, LogOut } from 'lucide-react'
import toast from 'react-hot-toast'

export default function AdminPage() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [activeTab, setActiveTab] = useState('overview')

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow sticky top-0 z-40">
        <div className="max-w-7xl mx-auto px-4 py-6 flex justify-between items-center">
          <div className="flex items-center gap-4">
            <button
              onClick={() => navigate('/')}
              className="p-2 text-gray-600 hover:bg-gray-100 rounded-lg transition"
            >
              <ArrowLeft size={20} />
            </button>
            <h1 className="text-3xl font-bold text-gray-900">Admin</h1>
          </div>

          <button
            onClick={handleLogout}
            className="flex items-center gap-2 bg-red-600 hover:bg-red-700 text-white px-4 py-2 rounded-lg transition"
          >
            <LogOut size={18} />
            Sair
          </button>
        </div>
      </header>

      {/* Tabs */}
      <div className="bg-white border-b sticky top-16 z-30">
        <div className="max-w-7xl mx-auto px-4">
          <div className="flex gap-8">
            {[
              { id: 'overview', label: 'Visão Geral' },
              { id: 'users', label: 'Usuários' },
              { id: 'audit', label: 'Auditoria' },
              { id: 'settings', label: 'Configurações' }
            ].map((tab) => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`py-4 px-1 border-b-2 font-medium text-sm transition ${
                  activeTab === tab.id
                    ? 'border-primary-600 text-primary-600'
                    : 'border-transparent text-gray-600 hover:text-gray-900'
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Content */}
      <main className="max-w-7xl mx-auto px-4 py-8">
        {activeTab === 'overview' && (
          <div className="bg-white rounded-lg shadow p-8">
            <h2 className="text-2xl font-bold text-gray-900 mb-4">Visão Geral do Sistema</h2>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              <div className="border rounded-lg p-6">
                <p className="text-gray-600 text-sm mb-2">Usuário Atual</p>
                <p className="text-xl font-semibold text-gray-900">{user?.email}</p>
              </div>
              <div className="border rounded-lg p-6">
                <p className="text-gray-600 text-sm mb-2">Roles</p>
                <p className="text-xl font-semibold text-gray-900">{user?.roles?.join(', ')}</p>
              </div>
              <div className="border rounded-lg p-6">
                <p className="text-gray-600 text-sm mb-2">Status</p>
                <p className="text-xl font-semibold text-green-600">✓ Online</p>
              </div>
            </div>
          </div>
        )}

        {activeTab === 'users' && (
          <div className="bg-white rounded-lg shadow p-8">
            <h2 className="text-2xl font-bold text-gray-900 mb-4">Gerenciamento de Usuários</h2>
            <p className="text-gray-600">Funcionalidade em desenvolvimento...</p>
          </div>
        )}

        {activeTab === 'audit' && (
          <div className="bg-white rounded-lg shadow p-8">
            <h2 className="text-2xl font-bold text-gray-900 mb-4">Log de Auditoria</h2>
            <p className="text-gray-600">Funcionalidade em desenvolvimento...</p>
          </div>
        )}

        {activeTab === 'settings' && (
          <div className="bg-white rounded-lg shadow p-8">
            <h2 className="text-2xl font-bold text-gray-900 mb-4">Configurações do Sistema</h2>
            <p className="text-gray-600">Funcionalidade em desenvolvimento...</p>
          </div>
        )}
      </main>
    </div>
  )
}
