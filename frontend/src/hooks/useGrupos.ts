import { useState, useCallback } from 'react'
import { fetchGrupos } from '../services/api'
import type { ProjetoGrupo } from '../types/projetos.types'

interface UseGruposResult {
  grupos: ProjetoGrupo[]
  loading: boolean
  syncing: boolean
  error: boolean
  refresh: () => void
}

export function useGrupos(): UseGruposResult {
  const [grupos, setGrupos] = useState<ProjetoGrupo[]>([])
  const [loading, setLoading] = useState(false)
  const [syncing, setSyncing] = useState(false)
  const [error, setError] = useState(false)

  const refresh = useCallback(() => {
    setLoading(true)
    setError(false)
    fetchGrupos()
      .then((result) => {
        if (result.status === 'syncing') {
          setSyncing(true)
        } else {
          setSyncing(false)
          setGrupos(result.grupos)
        }
      })
      .catch(() => setError(true))
      .finally(() => setLoading(false))
  }, [])

  return { grupos, loading, syncing, error, refresh }
}
