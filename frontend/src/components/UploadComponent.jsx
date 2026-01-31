import { useState } from 'react'
import { documentService } from '../services/documentService'
import toast from 'react-hot-toast'
import { Upload, AlertCircle, Clock } from 'lucide-react'

export default function UploadComponent({ onUploadComplete }) {
  const [dragActive, setDragActive] = useState(false)
  const [loading, setLoading] = useState(false)
  const [progress, setProgress] = useState(0)
  const [timeoutMs, setTimeoutMs] = useState(30000)

  const handleDrag = (e) => {
    e.preventDefault()
    e.stopPropagation()
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true)
    } else if (e.type === 'dragleave') {
      setDragActive(false)
    }
  }

  const handleFile = async (file) => {
    if (!file) return

    const allowedTypes = ['application/pdf', 'image/png', 'image/jpeg', 'image/tiff', 'text/plain']
    if (!allowedTypes.includes(file.type)) {
      toast.error('Tipo de arquivo não suportado. Use: PDF, PNG, JPG, TIFF ou TXT')
      return
    }

    if (file.size > 50 * 1024 * 1024) {
      toast.error('Arquivo muito grande. Máximo: 50MB')
      return
    }

    setLoading(true)
    setProgress(10)

    try {
      // Simular progresso durante upload
      const progressInterval = setInterval(() => {
        setProgress(prev => {
          const newProgress = prev + Math.random() * 30
          return Math.min(newProgress, 90)
        })
      }, 200)

      const response = await documentService.uploadAsync(file, timeoutMs)
      clearInterval(progressInterval)
      
      setProgress(100)
      toast.success(`✅ Documento enviado! ID: ${response.data?.documentId || 'processing'}`)
      
      // Aguarda um pouco para mostrar 100%
      await new Promise(resolve => setTimeout(resolve, 500))
      
      onUploadComplete(response.data)
      setProgress(0)
    } catch (error) {
      setProgress(0)
      toast.error(error.response?.data?.message || 'Erro ao enviar arquivo')
    } finally {
      setLoading(false)
    }
  }

  const handleDrop = (e) => {
    e.preventDefault()
    e.stopPropagation()
    setDragActive(false)

    const files = e.dataTransfer?.files
    if (files?.[0]) {
      handleFile(files[0])
    }
  }

  const handleChange = (e) => {
    if (e.target.files?.[0]) {
      handleFile(e.target.files[0])
    }
  }

  return (
    <div className="max-w-2xl mx-auto">
      <div className="bg-white rounded-lg shadow-lg p-8 fade-in">
        <h2 className="text-2xl font-bold text-gray-900 mb-8">Upload de Documento</h2>

        {/* Drop Zone */}
        <div
          onDragEnter={handleDrag}
          onDragLeave={handleDrag}
          onDragOver={handleDrag}
          onDrop={handleDrop}
          className={`border-2 border-dashed rounded-lg p-12 text-center transition ${
            dragActive
              ? 'border-primary-500 bg-primary-50'
              : 'border-gray-300 hover:border-gray-400 bg-gray-50'
          }`}
        >
          <input
            type="file"
            id="fileInput"
            onChange={handleChange}
            disabled={loading}
            className="hidden"
            accept=".pdf,.png,.jpg,.jpeg,.tiff,.txt"
          />

          <label htmlFor="fileInput" className="cursor-pointer block">
            <Upload size={48} className="mx-auto mb-4 text-gray-400" />
            <p className="text-lg font-semibold text-gray-900 mb-2">
              Arraste seu arquivo aqui ou clique para selecionar
            </p>
            <p className="text-sm text-gray-600">
              Suportados: PDF, PNG, JPG, TIFF, TXT (máx 50MB)
            </p>
          </label>
        </div>

        {/* Configuration */}
        <div className="mt-8 bg-gray-50 p-6 rounded-lg border border-gray-200">
          <div className="flex items-center gap-2 mb-4">
            <Clock size={20} className="text-primary-600" />
            <label className="block text-sm font-semibold text-gray-900">
              Timeout de Processamento (ms)
            </label>
          </div>
          <input
            type="range"
            value={timeoutMs}
            onChange={(e) => setTimeoutMs(Math.max(5000, Math.min(300000, parseInt(e.target.value))))}
            min="5000"
            max="300000"
            step="5000"
            className="w-full"
          />
          <div className="flex justify-between items-center mt-2">
            <span className="text-sm text-gray-600">{(timeoutMs / 1000).toFixed(0)}s</span>
            <span className="text-xs text-gray-500">Min: 5s | Max: 5min</span>
          </div>
        </div>

        {/* Progress Bar */}
        {loading && (
          <div className="mt-8">
            <div className="flex items-center justify-between mb-3">
              <span className="text-sm font-medium text-gray-700">Enviando...</span>
              <span className="text-sm font-bold text-primary-600">{Math.round(progress)}%</span>
            </div>
            <div className="w-full bg-gray-200 rounded-full h-2 overflow-hidden">
              <div
                className="bg-gradient-to-r from-primary-500 to-primary-600 h-2 rounded-full transition-all duration-300"
                style={{ width: `${progress}%` }}
              />
            </div>
          </div>
        )}

        {/* Info */}
        <div className="mt-8 bg-blue-50 border border-blue-200 rounded-lg p-4 flex gap-3">
          <AlertCircle size={20} className="text-blue-600 flex-shrink-0" />
          <p className="text-sm text-blue-700">
            O documento será processado em background. Acompanhe o progresso na lista de documentos ou 
            receba notificações via webhook.
          </p>
        </div>
      </div>
    </div>
  )
}
