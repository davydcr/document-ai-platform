import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { documentService } from '../services/documentService'
import { usePolling } from '../hooks/usePolling'
import toast from 'react-hot-toast'
import { ArrowLeft, CheckCircle, Clock, AlertCircle, Copy, Download } from 'lucide-react'

export default function DocumentDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [document, setDocument] = useState(null)
  const [loading, setLoading] = useState(true)
  const [webhookUrl, setWebhookUrl] = useState('')
  const [showWebhookForm, setShowWebhookForm] = useState(false)

  // Usar polling para atualizar status
  const { data: statusData } = usePolling(
    () => documentService.getAsyncStatus(id),
    3000,
    !!id
  )

  useEffect(() => {
    const fetchDocument = async () => {
      try {
        setLoading(true)
        const response = await documentService.getDocument(id)
        setDocument(response.data)
      } catch (error) {
        toast.error('Erro ao carregar documento')
        setTimeout(() => navigate('/'), 2000)
      } finally {
        setLoading(false)
      }
    }

    fetchDocument()
  }, [id, navigate])

  const handleRegisterWebhook = async () => {
    if (!webhookUrl) {
      toast.error('URL do webhook é obrigatória')
      return
    }

    try {
      await documentService.registerWebhook(id, webhookUrl)
      toast.success('Webhook registrado com sucesso!')
      setWebhookUrl('')
      setShowWebhookForm(false)
    } catch (error) {
      toast.error('Erro ao registrar webhook')
    }
  }

  const copyToClipboard = (text) => {
    navigator.clipboard.writeText(text)
    toast.success('Copiado para clipboard!')
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    )
  }

  if (!document) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <AlertCircle size={48} className="text-red-600 mx-auto mb-4" />
          <p className="text-gray-600">Documento não encontrado</p>
        </div>
      </div>
    )
  }

  const getStatusIcon = (status) => {
    switch (status) {
      case 'COMPLETED':
        return <CheckCircle size={24} className="text-green-500" />
      case 'PROCESSING':
        return <Clock size={24} className="text-yellow-500 animate-spin" />
      case 'FAILED':
        return <AlertCircle size={24} className="text-red-500" />
      default:
        return <Clock size={24} className="text-gray-400" />
    }
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow sticky top-0 z-40">
        <div className="max-w-4xl mx-auto px-4 py-4 flex items-center gap-4">
          <button
            onClick={() => navigate('/')}
            className="p-2 text-gray-600 hover:bg-gray-100 rounded-lg transition"
          >
            <ArrowLeft size={20} />
          </button>
          <div>
            <h1 className="text-xl font-bold text-gray-900">Detalhes do Documento</h1>
            <p className="text-sm text-gray-600">{document.fileName}</p>
          </div>
        </div>
      </header>

      <main className="max-w-4xl mx-auto px-4 py-8 space-y-6">
        {/* Status Card */}
        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-lg font-bold text-gray-900">Status</h2>
            {getStatusIcon(document.status || statusData?.status)}
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <p className="text-sm text-gray-600 mb-1">Status</p>
              <p className="text-lg font-semibold text-gray-900">{document.status}</p>
            </div>
            <div>
              <p className="text-sm text-gray-600 mb-1">ID do Documento</p>
              <div className="flex items-center gap-2">
                <code className="text-sm bg-gray-100 px-2 py-1 rounded text-gray-900">
                  {document.id}
                </code>
                <button
                  onClick={() => copyToClipboard(document.id)}
                  className="p-1 text-gray-600 hover:bg-gray-100 rounded transition"
                >
                  <Copy size={16} />
                </button>
              </div>
            </div>
          </div>
        </div>

        {/* Classificação */}
        {document.classification && (
          <div className="bg-white rounded-lg shadow p-6">
            <h2 className="text-lg font-bold text-gray-900 mb-6">Classificação</h2>

            <div className="space-y-4">
              <div>
                <p className="text-sm text-gray-600 mb-2">Tipo de Documento</p>
                <p className="text-lg font-semibold text-gray-900">{document.classification.label}</p>
              </div>

              <div>
                <p className="text-sm text-gray-600 mb-2">Confiança</p>
                <div className="flex items-center gap-3">
                  <div className="flex-1 bg-gray-200 rounded-full h-2">
                    <div
                      className="bg-primary-600 h-2 rounded-full transition-all"
                      style={{ width: `${(document.classification.confidence || 0) * 100}%` }}
                    />
                  </div>
                  <span className="text-lg font-semibold text-gray-900 min-w-[60px] text-right">
                    {((document.classification.confidence || 0) * 100).toFixed(1)}%
                  </span>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Conteúdo Extraído */}
        {document.extractedContent && (
          <div className="bg-white rounded-lg shadow p-6">
            <h2 className="text-lg font-bold text-gray-900 mb-6">Conteúdo Extraído</h2>

            <div className="bg-gray-50 rounded-lg p-4 max-h-96 overflow-y-auto">
              <p className="text-sm text-gray-700 whitespace-pre-wrap">
                {document.extractedContent.text || 'Sem conteúdo'}
              </p>
            </div>
          </div>
        )}

        {/* Webhook Configuration */}
        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex justify-between items-center mb-6">
            <h2 className="text-lg font-bold text-gray-900">Configuração de Webhook</h2>
            <button
              onClick={() => setShowWebhookForm(!showWebhookForm)}
              className="text-primary-600 hover:text-primary-700 text-sm font-medium"
            >
              {showWebhookForm ? 'Cancelar' : 'Adicionar Webhook'}
            </button>
          </div>

          {showWebhookForm && (
            <div className="space-y-4 mb-6">
              <input
                type="url"
                value={webhookUrl}
                onChange={(e) => setWebhookUrl(e.target.value)}
                placeholder="https://seu-servidor.com/webhook"
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
              />
              <button
                onClick={handleRegisterWebhook}
                className="w-full bg-primary-600 hover:bg-primary-700 text-white font-medium py-2 px-4 rounded-lg transition"
              >
                Registrar Webhook
              </button>
            </div>
          )}

          <p className="text-sm text-gray-600">
            Você será notificado automaticamente quando o documento terminar de processar.
          </p>
        </div>

        {/* Metadados */}
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-lg font-bold text-gray-900 mb-6">Informações</h2>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
            <div>
              <p className="text-gray-600 mb-1">Nome do Arquivo</p>
              <p className="text-gray-900 font-medium">{document.fileName}</p>
            </div>
            <div>
              <p className="text-gray-600 mb-1">Tipo</p>
              <p className="text-gray-900 font-medium">{document.type}</p>
            </div>
            <div>
              <p className="text-gray-600 mb-1">Criado em</p>
              <p className="text-gray-900 font-medium">
                {new Date(document.createdAt).toLocaleString('pt-BR')}
              </p>
            </div>
            <div>
              <p className="text-gray-600 mb-1">Atualizado em</p>
              <p className="text-gray-900 font-medium">
                {new Date(document.updatedAt).toLocaleString('pt-BR')}
              </p>
            </div>
          </div>
        </div>
      </main>
    </div>
  )
}
