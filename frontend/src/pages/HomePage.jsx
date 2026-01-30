import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import { documentService } from '../services/documentService'
import toast from 'react-hot-toast'
import { LogOut, LayoutGrid, Settings, FileUp, List } from 'lucide-react'
import UploadComponent from '../components/UploadComponent'
import DocumentListComponent from '../components/DocumentListComponent'

export default function HomePage() {
  const [activeTab, setActiveTab] = useState('upload')
  const [documents, setDocuments] = useState([])
  const [loading, setLoading] = useState(false)
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  useEffect(() => {
    if (activeTab === 'documents') {
      fetchDocuments()
    }
  }, [activeTab])

  const fetchDocuments = async () => {
    try {
      setLoading(true)
      const response = await documentService.getDocuments(0, 50)
      setDocuments(response.data.content || [])
    } catch (error) {
      toast.error('Erro ao carregar documentos')
    } finally {
      setLoading(false)
    }
  }

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const handleUploadComplete = (newDocument) => {
    setDocuments([newDocument, ...documents])
    toast.success('Documento enviado com sucesso!')
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow sticky top-0 z-40">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 flex justify-between items-center">
          <div className="flex items-center gap-4">
            <div className="bg-primary-100 p-2 rounded-lg">
              <FileUp className="text-primary-600" size={24} />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-gray-900">Document AI</h1>
              <p className="text-sm text-gray-600">Platform v1.0</p>
            </div>
          </div>

          <div className="flex items-center gap-4">
            <div className="text-right">
              <p className="text-sm font-medium text-gray-900">{user?.email}</p>
              <p className="text-xs text-gray-500">{user?.roles?.join(', ')}</p>
            </div>

            <button
              onClick={() => navigate('/dashboard')}
              className="p-2 text-gray-600 hover:bg-gray-100 rounded-lg transition"
              title="Dashboard"
            >
              <LayoutGrid size={20} />
            </button>

            {user?.roles?.includes('ADMIN') && (
              <button
                onClick={() => navigate('/admin')}
                className="p-2 text-gray-600 hover:bg-gray-100 rounded-lg transition"
                title="Admin"
              >
                <Settings size={20} />
              </button>
            )}

            <button
              onClick={handleLogout}
              className="flex items-center gap-2 bg-red-600 hover:bg-red-700 text-white px-4 py-2 rounded-lg transition"
            >
              <LogOut size={18} />
              <span className="text-sm">Sair</span>
            </button>
          </div>
        </div>
      </header>

      {/* Tabs */}
      <div className="bg-white border-b sticky top-16 z-30">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex gap-8">
            <button
              onClick={() => setActiveTab('upload')}
              className={`py-4 px-1 border-b-2 font-medium text-sm transition flex items-center gap-2 ${
                activeTab === 'upload'
                  ? 'border-primary-600 text-primary-600'
                  : 'border-transparent text-gray-600 hover:text-gray-900'
              }`}
            >
              <FileUp size={18} />
              Upload
            </button>
            <button
              onClick={() => setActiveTab('documents')}
              className={`py-4 px-1 border-b-2 font-medium text-sm transition flex items-center gap-2 ${
                activeTab === 'documents'
                  ? 'border-primary-600 text-primary-600'
                  : 'border-transparent text-gray-600 hover:text-gray-900'
              }`}
            >
              <List size={18} />
              Documentos
            </button>
          </div>
        </div>
      </div>

      {/* Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {activeTab === 'upload' && <UploadComponent onUploadComplete={handleUploadComplete} />}
        {activeTab === 'documents' && (
          <DocumentListComponent documents={documents} loading={loading} onRefresh={fetchDocuments} />
        )}
      </main>
    </div>
  )
}
