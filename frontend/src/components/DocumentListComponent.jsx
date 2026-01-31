import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { documentService } from '../services/documentService'
import toast from 'react-hot-toast'
import { RefreshCw, CheckCircle, Clock, AlertCircle, Eye } from 'lucide-react'
import { formatDistanceToNow } from 'date-fns'
import { ptBR } from 'date-fns/locale'

export default function DocumentListComponent({ documents: initialDocs, loading, onRefresh }) {
  const [documents, setDocuments] = useState(initialDocs)
  const navigate = useNavigate()

  useEffect(() => {
    setDocuments(initialDocs)
  }, [initialDocs])

  // Polling para atualizar status - aumentado para 10s com verificação de COMPLETED
  useEffect(() => {
    // Não fazer polling se já tem COMPLETED ou FAILED
    const hasActiveProcessing = initialDocs?.some(doc => 
      doc.status !== 'COMPLETED' && doc.status !== 'FAILED'
    )
    
    if (!hasActiveProcessing) return

    const interval = setInterval(() => {
      onRefresh?.()
    }, 10000) // 10 segundos ao invés de 5

    return () => clearInterval(interval)
  }, [onRefresh, initialDocs])

  const getStatusIcon = (status) => {
    switch (status) {
      case 'COMPLETED':
        return <CheckCircle size={20} className="text-green-500" />
      case 'PROCESSING':
        return <Clock size={20} className="text-yellow-500 animate-spin" />
      case 'FAILED':
        return <AlertCircle size={20} className="text-red-500" />
      default:
        return <Clock size={20} className="text-gray-400" />
    }
  }

  const getStatusColor = (status) => {
    switch (status) {
      case 'COMPLETED':
        return 'bg-green-50 text-green-700 border-green-200'
      case 'PROCESSING':
        return 'bg-yellow-50 text-yellow-700 border-yellow-200'
      case 'FAILED':
        return 'bg-red-50 text-red-700 border-red-200'
      default:
        return 'bg-gray-50 text-gray-700 border-gray-200'
    }
  }

  if (loading && documents.length === 0) {
    return (
      <div className="text-center py-12 bg-white rounded-lg shadow">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600 mx-auto mb-4"></div>
        <p className="text-gray-600">Carregando documentos...</p>
      </div>
    )
  }

  if (documents.length === 0) {
    return (
      <div className="text-center py-12 bg-white rounded-lg shadow">
        <p className="text-gray-600 text-lg mb-4">Nenhum documento ainda.</p>
        <p className="text-sm text-gray-500">Envie um arquivo para começar!</p>
      </div>
    )
  }

  return (
    <div className="bg-white rounded-lg shadow overflow-hidden fade-in">
      <div className="px-6 py-4 border-b flex justify-between items-center">
        <h2 className="text-xl font-bold text-gray-900">Documentos ({documents.length})</h2>
        <button
          onClick={onRefresh}
          className="flex items-center gap-2 bg-primary-600 hover:bg-primary-700 text-white px-4 py-2 rounded-lg transition text-sm"
        >
          <RefreshCw size={16} />
          Atualizar
        </button>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full">
          <thead className="bg-gray-50 border-b">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-semibold text-gray-700 uppercase">Nome</th>
              <th className="px-6 py-3 text-left text-xs font-semibold text-gray-700 uppercase">Status</th>
              <th className="px-6 py-3 text-left text-xs font-semibold text-gray-700 uppercase">Classificação</th>
              <th className="px-6 py-3 text-left text-xs font-semibold text-gray-700 uppercase">Confiança</th>
              <th className="px-6 py-3 text-left text-xs font-semibold text-gray-700 uppercase">Data</th>
              <th className="px-6 py-3 text-right text-xs font-semibold text-gray-700 uppercase">Ação</th>
            </tr>
          </thead>
          <tbody className="divide-y">
            {documents.map((doc) => (
              <tr key={doc.id} className="hover:bg-gray-50 transition">
                <td className="px-6 py-4 text-sm text-gray-900 font-medium truncate">
                  {doc.fileName}
                </td>
                <td className="px-6 py-4">
                  <div className={`flex items-center gap-2 px-3 py-1 rounded-full w-fit border ${getStatusColor(doc.status)}`}>
                    {getStatusIcon(doc.status)}
                    <span className="text-xs font-medium">{doc.status}</span>
                  </div>
                </td>
                <td className="px-6 py-4 text-sm text-gray-600">
                  {doc.classification?.label || '-'}
                </td>
                <td className="px-6 py-4 text-sm">
                  {doc.classification?.confidence ? (
                    <div className="flex items-center gap-2">
                      <div className="w-16 bg-gray-200 rounded-full h-2">
                        <div
                          className="bg-primary-600 h-2 rounded-full"
                          style={{ width: `${doc.classification.confidence * 100}%` }}
                        />
                      </div>
                      <span className="text-xs text-gray-600">{(doc.classification.confidence * 100).toFixed(0)}%</span>
                    </div>
                  ) : (
                    <span className="text-gray-400">-</span>
                  )}
                </td>
                <td className="px-6 py-4 text-sm text-gray-600">
                  {doc.createdAt
                    ? formatDistanceToNow(new Date(doc.createdAt), { addSuffix: true, locale: ptBR })
                    : '-'}
                </td>
                <td className="px-6 py-4 text-right">
                  <button
                    onClick={() => navigate(`/documents/${doc.id}`)}
                    className="inline-flex items-center gap-1 text-primary-600 hover:text-primary-700 font-medium transition text-sm"
                  >
                    <Eye size={16} />
                    Ver
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
