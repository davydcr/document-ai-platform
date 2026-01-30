import { useState, useEffect } from 'react'
import { dashboardService } from '../services/documentService'
import { usePolling } from '../hooks/usePolling'
import toast from 'react-hot-toast'
import { Activity, TrendingUp, AlertCircle, Zap, RotateCcw } from 'lucide-react'
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts'

export default function DashboardPage() {
  const [metrics, setMetrics] = useState(null)
  const [cbStatus, setCbStatus] = useState(null)
  const [loading, setLoading] = useState(true)

  // Polling para métricas
  const { data: metricsData } = usePolling(
    async () => {
      const response = await dashboardService.getMetrics()
      return response.data
    },
    5000
  )

  // Polling para circuit breaker
  const { data: cbData } = usePolling(
    async () => {
      const response = await dashboardService.getCircuitBreakerStatus()
      return response.data
    },
    5000
  )

  useEffect(() => {
    if (metricsData) {
      setMetrics(metricsData)
      setLoading(false)
    }
  }, [metricsData])

  useEffect(() => {
    if (cbData) {
      setCbStatus(cbData)
    }
  }, [cbData])

  const handleResetCB = async () => {
    try {
      await dashboardService.resetCircuitBreaker()
      toast.success('Circuit breaker resetado!')
    } catch (error) {
      toast.error('Erro ao resetar circuit breaker')
    }
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow sticky top-0 z-40">
        <div className="max-w-7xl mx-auto px-4 py-6">
          <h1 className="text-3xl font-bold text-gray-900">Dashboard</h1>
          <p className="text-gray-600 text-sm mt-1">Monitoramento em tempo real</p>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-4 py-8 space-y-8">
        {/* Métricas Principais */}
        {metrics && (
          <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
            {[
              {
                label: 'Documentos',
                value: metrics.totalProcessed,
                icon: Activity,
                color: 'bg-blue-50 text-blue-600'
              },
              {
                label: 'Processando',
                value: metrics.inProgress,
                icon: TrendingUp,
                color: 'bg-yellow-50 text-yellow-600'
              },
              {
                label: 'Completados',
                value: metrics.completed,
                icon: Activity,
                color: 'bg-green-50 text-green-600'
              },
              {
                label: 'Falhas',
                value: metrics.failed,
                icon: AlertCircle,
                color: 'bg-red-50 text-red-600'
              }
            ].map((card) => (
              <div key={card.label} className="bg-white rounded-lg shadow p-6">
                <div className={`w-12 h-12 rounded-lg ${card.color} flex items-center justify-center mb-4`}>
                  <card.icon size={24} />
                </div>
                <p className="text-sm text-gray-600 mb-1">{card.label}</p>
                <p className="text-3xl font-bold text-gray-900">{card.value}</p>
              </div>
            ))}
          </div>
        )}

        {/* Circuit Breaker Status */}
        {cbStatus && (
          <div className="bg-white rounded-lg shadow p-6">
            <div className="flex justify-between items-center mb-6">
              <div className="flex items-center gap-3">
                <Zap size={24} className="text-orange-600" />
                <h2 className="text-lg font-bold text-gray-900">Circuit Breaker</h2>
              </div>
              <div className="flex items-center gap-4">
                <div className={`px-4 py-2 rounded-full text-sm font-semibold ${
                  cbStatus.state === 'CLOSED'
                    ? 'bg-green-50 text-green-700'
                    : cbStatus.state === 'HALF_OPEN'
                    ? 'bg-yellow-50 text-yellow-700'
                    : 'bg-red-50 text-red-700'
                }`}>
                  {cbStatus.state}
                </div>
                <button
                  onClick={handleResetCB}
                  className="flex items-center gap-2 bg-primary-600 hover:bg-primary-700 text-white px-4 py-2 rounded-lg transition text-sm"
                >
                  <RotateCcw size={16} />
                  Reset
                </button>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              <div>
                <p className="text-sm text-gray-600 mb-2">Sucessos</p>
                <p className="text-2xl font-bold text-green-600">{cbStatus.successCount}</p>
              </div>
              <div>
                <p className="text-sm text-gray-600 mb-2">Falhas</p>
                <p className="text-2xl font-bold text-red-600">{cbStatus.failureCount}</p>
              </div>
              <div>
                <p className="text-sm text-gray-600 mb-2">Taxa de Falha</p>
                <p className="text-2xl font-bold text-orange-600">
                  {((cbStatus.failureCount / (cbStatus.successCount + cbStatus.failureCount)) * 100 || 0).toFixed(1)}%
                </p>
              </div>
            </div>

            <div className="mt-6 pt-6 border-t">
              <p className="text-sm text-gray-600 mb-2">Limiar de Abertura</p>
              <div className="flex items-center gap-3">
                <div className="flex-1 bg-gray-200 rounded-full h-2">
                  <div
                    className="bg-primary-600 h-2 rounded-full"
                    style={{
                      width: `${(cbStatus.failureThreshold * 100) / 100}%`
                    }}
                  />
                </div>
                <span className="text-sm font-semibold text-gray-900">{cbStatus.failureThreshold}%</span>
              </div>
            </div>
          </div>
        )}

        {/* Informações Adicionais */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="bg-white rounded-lg shadow p-6">
            <h3 className="text-lg font-bold text-gray-900 mb-4">Taxa de Sucesso</h3>
            {metrics && (
              <div>
                <p className="text-3xl font-bold text-primary-600 mb-2">
                  {(metrics.successRate * 100).toFixed(1)}%
                </p>
                <div className="w-full bg-gray-200 rounded-full h-3">
                  <div
                    className="bg-primary-600 h-3 rounded-full transition-all"
                    style={{ width: `${metrics.successRate * 100}%` }}
                  />
                </div>
              </div>
            )}
          </div>

          <div className="bg-white rounded-lg shadow p-6">
            <h3 className="text-lg font-bold text-gray-900 mb-4">Tempo Médio</h3>
            {metrics && (
              <p className="text-3xl font-bold text-primary-600">
                {(metrics.avgProcessingTime / 1000).toFixed(2)}s
              </p>
            )}
          </div>
        </div>
      </main>
    </div>
  )
}
