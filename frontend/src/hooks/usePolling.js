import { useState, useEffect } from 'react'

export function usePolling(fetchFn, interval = 5000, enabled = true) {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    if (!enabled) return

    let isMounted = true
    let timeoutId

    const poll = async () => {
      try {
        setLoading(true)
        const result = await fetchFn()
        if (isMounted) {
          setData(result)
          setError(null)
        }
      } catch (err) {
        if (isMounted) {
          setError(err.message)
        }
      } finally {
        if (isMounted) {
          setLoading(false)
          timeoutId = setTimeout(poll, interval)
        }
      }
    }

    poll()

    return () => {
      isMounted = false
      clearTimeout(timeoutId)
    }
  }, [fetchFn, interval, enabled])

  return { data, loading, error }
}
