import { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useToast } from '../components/Toast/ToastContext'
import { BuscaTabela } from '../components/BuscaTabela'
import {
  escutarEventosBusca,
  fetchResultadoBusca,
  iniciarBusca,
} from '../services/api'
import type { ProjetoBusca } from '../types/busca.types'
import './BuscaPage.css'

const EXTENSOES_SUGERIDAS = [
  '.java',
  '.kt',
  '.ts',
  '.tsx',
  '.js',
  '.jsx',
  '.py',
  '.go',
  '.rb',
  '.php',
  '.cs',
  '.cpp',
  '.yaml',
  '.yml',
  '.json',
  '.xml',
  '.sql',
]

export function BuscaPage() {
  const { t } = useTranslation()
  const { showToast } = useToast()

  const [extensao, setExtensao] = useState('')
  const [texto, setTexto] = useState('')
  const [buscando, setBuscando] = useState(false)
  const [resultado, setResultado] = useState<ProjetoBusca[] | null>(null)
  const [ultimaBusca, setUltimaBusca] = useState({ extensao: '', texto: '' })

  const esRef = useRef<EventSource | null>(null)
  const buscaIdRef = useRef<string | null>(null)

  const encerrarBusca = () => {
    buscaIdRef.current = null
    esRef.current?.close()
    esRef.current = null
    setBuscando(false)
  }

  const handleParar = () => {
    encerrarBusca()
    showToast(t('search.cancelled'), 'warning')
  }

  const handleBuscar = async () => {
    if (!extensao.trim() || !texto.trim()) return

    setBuscando(true)
    setResultado(null)
    showToast(t('search.searching'), 'info', 8000)

    esRef.current?.close()
    esRef.current = escutarEventosBusca(
      async (id) => {
        if (id !== buscaIdRef.current) return
        try {
          const dados = await fetchResultadoBusca(id)
          setResultado(dados)
          if (dados.length === 0) {
            showToast(t('search.noResults'), 'info')
          } else {
            showToast(t('search.searchSuccess', { total: dados.length }), 'success')
          }
        } catch {
          showToast(t('search.fetchError'), 'error')
        } finally {
          encerrarBusca()
        }
      },
      (id) => {
        if (id && id !== buscaIdRef.current) return
        showToast(t('search.searchError'), 'error')
        encerrarBusca()
      },
    )

    try {
      const id = await iniciarBusca(extensao.trim(), texto.trim())
      buscaIdRef.current = id
      setUltimaBusca({ extensao: extensao.trim(), texto: texto.trim() })
    } catch {
      showToast(t('search.searchError'), 'error')
      encerrarBusca()
    }
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !buscando) handleBuscar()
  }

  const searchEnabled = extensao.trim().length > 0 && texto.trim().length > 0

  return (
    <div className="dep-page">
      <h1>{t('search.title')}</h1>

      {/* ── Search section ── */}
      <section className="dep-page__section">
        <h2>{t('search.searchSection')}</h2>
        <div className="dep-page__filters">
          {/* extensão */}
          <div className="dep-page__filter-group">
            <label className="dep-page__label" htmlFor="busca-extensao">
              {t('search.extensao')}
            </label>
            <input
              id="busca-extensao"
              className="dep-page__input"
              type="text"
              placeholder={t('search.extensaoPlaceholder')}
              value={extensao}
              onChange={(e) => setExtensao(e.target.value)}
              onKeyDown={handleKeyDown}
              list="busca-extensoes"
              disabled={buscando}
            />
            <datalist id="busca-extensoes">
              {EXTENSOES_SUGERIDAS.map((ext) => (
                <option key={ext} value={ext} />
              ))}
            </datalist>
          </div>

          {/* texto */}
          <div className="dep-page__filter-group busca-page__texto-group">
            <label className="dep-page__label" htmlFor="busca-texto">
              {t('search.texto')}
            </label>
            <input
              id="busca-texto"
              className="dep-page__input"
              type="text"
              placeholder={t('search.textoPlaceholder')}
              value={texto}
              onChange={(e) => setTexto(e.target.value)}
              onKeyDown={handleKeyDown}
              disabled={buscando}
            />
          </div>

          {/* botões */}
          <div className="busca-page__actions">
            <button
              className="dep-page__btn dep-page__btn--danger"
              onClick={handleParar}
              disabled={!buscando}
            >
              {t('search.stopButton')}
            </button>
            <button
              className="dep-page__btn dep-page__btn--primary"
              onClick={handleBuscar}
              disabled={!searchEnabled || buscando}
            >
              {buscando ? t('search.searching') : t('search.searchButton')}
            </button>
          </div>
        </div>
      </section>

      {/* ── Results section ── */}
      {resultado && resultado.length > 0 && (
        <section className="dep-page__section">
          <h2>{t('search.resultsSection')}</h2>
          <BuscaTabela
            dados={resultado}
            extensao={ultimaBusca.extensao}
            texto={ultimaBusca.texto}
          />
        </section>
      )}
    </div>
  )
}
