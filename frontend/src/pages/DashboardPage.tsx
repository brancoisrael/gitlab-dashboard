import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { RotateCcw } from 'lucide-react'
import { GrupoCard } from '../components/GrupoCard'
import { ProjetoTabela } from '../components/ProjetoTabela'
import { useGrupos } from '../hooks/useGrupos'
import { useToast } from '../components/Toast/ToastContext'
import { fetchProjetosPorGrupo, sincronizar, escutarEventosSincronizacao } from '../services/api'
import type { ProjetoDetalhe } from '../types/projetos.types'
import './DashboardPage.css'

export function DashboardPage() {
  const { t } = useTranslation()
  const { grupos, loading, syncing, error, refresh } = useGrupos()
  const { showToast } = useToast()
  const esRef = useRef<EventSource | null>(null)
  const syncingRef = useRef(false)

  const [selectedGrupo, setSelectedGrupo] = useState<string | null>(null)
  const [projetosGrupo, setProjetosGrupo] = useState<ProjetoDetalhe[]>([])
  const [loadingProjetos, setLoadingProjetos] = useState(false)

  useEffect(() => {
    refresh()
    return () => {
      esRef.current?.close()
    }
  }, [refresh])

  useEffect(() => {
    if (syncing && !syncingRef.current) {
      syncingRef.current = true
      showToast(t('dashboard.syncAutoStarted'), 'info', 8000)
      esRef.current?.close()
      esRef.current = escutarEventosSincronizacao(
        () => {
          syncingRef.current = false
          showToast(t('dashboard.syncSuccess'), 'success')
          refresh()
        },
        () => {
          syncingRef.current = false
          showToast(t('dashboard.syncError'), 'error')
        },
      )
    }
  }, [syncing, showToast, refresh, t])

  function handleSincronizar() {
    esRef.current?.close()
    sincronizar()
      .then(() => {
        showToast(t('dashboard.syncAutoStarted'), 'info', 8000)
        esRef.current = escutarEventosSincronizacao(
          () => {
            showToast(t('dashboard.syncSuccess'), 'success')
            refresh()
          },
          () => showToast(t('dashboard.syncError'), 'error'),
        )
      })
      .catch(() => showToast(t('dashboard.syncError'), 'error'))
  }

  async function handleCardClick(grupo: string) {
    if (selectedGrupo === grupo) {
      setSelectedGrupo(null)
      setProjetosGrupo([])
      return
    }
    setSelectedGrupo(grupo)
    setLoadingProjetos(true)
    try {
      const projetos = await fetchProjetosPorGrupo(grupo)
      setProjetosGrupo(projetos)
    } catch {
      showToast(t('common.error'), 'error')
      setProjetosGrupo([])
    } finally {
      setLoadingProjetos(false)
    }
  }

  const isSyncing = syncing || syncingRef.current

  return (
    <div className="dash-page">
      <h1>{t('dashboard.title')}</h1>

      <section className="dash-page__section">
        <div className="dash-page__section-header">
          <h2>{t('dashboard.gruposSection')}</h2>
          <button
            className="dash-page__icon-btn"
            onClick={handleSincronizar}
            disabled={isSyncing || loading}
            aria-label={t('dashboard.sync')}
            title={t('dashboard.sync')}
          >
            <RotateCcw size={18} className={isSyncing ? 'spinning' : ''} />
          </button>
        </div>

        {error && <p className="dash-page__msg dash-page__msg--error">{t('common.error')}</p>}

        {!error && grupos.length === 0 && !loading && !isSyncing && (
          <p className="dash-page__msg">{t('dashboard.noData')}</p>
        )}

        {grupos.length > 0 && (
          <div className="dashboard-cards">
            {grupos.map((g, i) => (
              <GrupoCard
                key={g.grupo}
                grupo={g}
                index={i}
                selected={selectedGrupo === g.grupo}
                onClick={() => handleCardClick(g.grupo)}
              />
            ))}
          </div>
        )}
      </section>

      {selectedGrupo && (
        <section className="dash-page__section">
          {loadingProjetos ? (
            <p className="dash-page__msg">{t('common.loading')}</p>
          ) : (
            <ProjetoTabela dados={projetosGrupo} grupo={selectedGrupo} />
          )}
        </section>
      )}
    </div>
  )
}
