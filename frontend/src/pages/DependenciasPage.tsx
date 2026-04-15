import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useToast } from '../components/Toast/ToastContext'
import { DependenciaGraph } from '../components/DependenciaGraph'
import { DependenciaTabela } from '../components/DependenciaTabela'
import {
  escutarEventosDependencias,
  fetchArquivosDisponiveis,
  fetchDependenciasConsulta,
  sincronizarDependencias,
} from '../services/api'
import type { DependenciaConsulta } from '../types/dependencias.types'
import './DependenciasPage.css'

const ARQUIVOS_SUPORTADOS = [
  'pom.xml',
  'package.json',
  'build.gradle',
  'build.gradle.kts',
  'requirements.txt',
  'pubspec.yaml',
  'pubspec.yml',
  'pyproject.toml',
  'composer.json',
]

export function DependenciasPage() {
  const { t } = useTranslation()
  const { showToast } = useToast()

  // --- sync section ---
  const [syncArquivo, setSyncArquivo] = useState('')
  const [syncing, setSyncing] = useState(false)
  const esRef = useRef<EventSource | null>(null)

  // --- filter section ---
  const [arquivosDisponiveis, setArquivosDisponiveis] = useState<string[]>([])
  const [filterArquivo, setFilterArquivo] = useState('')
  const [filterTipo, setFilterTipo] = useState('projeto')
  const [filterNome, setFilterNome] = useState('')
  const [filterPacote, setFilterPacote] = useState('')

  // autocomplete for nome
  const [nomeOptions, setNomeOptions] = useState<string[]>([])
  const [nomeInput, setNomeInput] = useState('')
  const [showNomeSuggestions, setShowNomeSuggestions] = useState(false)

  // --- results ---
  const [resultado, setResultado] = useState<DependenciaConsulta[] | null>(null)
  const [querying, setQuerying] = useState(false)

  const carregarArquivos = async () => {
    try {
      const lista = await fetchArquivosDisponiveis()
      setArquivosDisponiveis(lista)
    } catch {
      // silencioso — sem arquivos ainda
    }
  }

  useEffect(() => {
    carregarArquivos()
    return () => {
      esRef.current?.close()
    }
  }, [])

  // recarregar opções de nome quando arquivo ou tipo mudam
  useEffect(() => {
    if (!filterArquivo) {
      setNomeOptions([])
      setFilterNome('')
      setNomeInput('')
      return
    }
    // passa o tipo para receber chaves do tipo correto:
    // sem tipo / tipo=projeto → nomes de projetos; tipo=dependencia → nomes de dependências
    const tipoParam = filterTipo || undefined
    fetchDependenciasConsulta(filterArquivo, tipoParam)
      .then((dados) => {
        const nomes = dados.map((d) => d.nome).sort((a, b) => a.localeCompare(b))
        setNomeOptions(nomes)
      })
      .catch(() => setNomeOptions([]))
  }, [filterArquivo, filterTipo])

  const handleSincronizar = async () => {
    if (!syncArquivo) return
    setSyncing(true)
    showToast(t('dependencies.syncing'), 'info')

    esRef.current?.close()
    esRef.current = escutarEventosDependencias(
      (arquivo) => {
        setSyncing(false)
        showToast(t('dependencies.syncSuccess', { arquivo }), 'success')
        carregarArquivos()
        esRef.current?.close()
      },
      (arquivo) => {
        setSyncing(false)
        showToast(t('dependencies.syncError', { arquivo: arquivo || syncArquivo }), 'error')
        esRef.current?.close()
      },
    )

    try {
      await sincronizarDependencias(syncArquivo)
    } catch {
      setSyncing(false)
      showToast(t('dependencies.syncError', { arquivo: syncArquivo }), 'error')
      esRef.current?.close()
    }
  }

  const queryEnabled = !!filterArquivo

  const handleConsultar = async () => {
    if (!queryEnabled) return
    setQuerying(true)
    try {
      const dados = await fetchDependenciasConsulta(
        filterArquivo,
        filterTipo || undefined,
        filterNome || undefined,
        filterPacote || undefined,
      )
      setResultado(dados)
      if (dados.length === 0) showToast(t('dependencies.noResults'), 'info')
    } catch {
      showToast(t('dependencies.queryError'), 'error')
    } finally {
      setQuerying(false)
    }
  }

  const filteredNomeSuggestions = nomeOptions.filter((n) =>
    n.toLowerCase().includes(nomeInput.toLowerCase()),
  )

  return (
    <div className="dep-page">
      <h1>{t('dependencies.title')}</h1>

      {/* ── Sync section ── */}
      <section className="dep-page__section">
        <h2>{t('dependencies.syncSection')}</h2>
        <div className="dep-page__sync-row">
          <select
            className="dep-page__select"
            value={syncArquivo}
            onChange={(e) => setSyncArquivo(e.target.value)}
            disabled={syncing}
          >
            <option value="">{t('dependencies.syncPlaceholder')}</option>
            {ARQUIVOS_SUPORTADOS.map((a) => (
              <option key={a} value={a}>{a}</option>
            ))}
          </select>
          <button
            className="dep-page__btn dep-page__btn--primary"
            onClick={handleSincronizar}
            disabled={syncing || !syncArquivo}
          >
            {syncing ? t('dependencies.syncing') : t('dependencies.syncButton')}
          </button>
        </div>
      </section>

      {/* ── Filters section ── */}
      <section className="dep-page__section">
        <h2>{t('dependencies.filtersSection')}</h2>
        <div className="dep-page__filters">
          {/* arquivo */}
          <div className="dep-page__filter-group">
            <label className="dep-page__label">{t('dependencies.filterArquivo')}</label>
            <select
              className="dep-page__select"
              value={filterArquivo}
              onChange={(e) => {
                setFilterArquivo(e.target.value)
                setFilterTipo('projeto')
                setFilterNome('')
                setNomeInput('')
                setFilterPacote('')
                setResultado(null)
              }}
            >
              <option value="">{t('dependencies.filterArquivoPlaceholder')}</option>
              {arquivosDisponiveis.map((a) => (
                <option key={a} value={a}>{a}</option>
              ))}
            </select>
          </div>

          {/* tipo */}
          <div className="dep-page__filter-group">
            <label className="dep-page__label">{t('dependencies.filterTipo')}</label>
            <select
              className="dep-page__select"
              value={filterTipo}
              onChange={(e) => {
                setFilterTipo(e.target.value)
                setFilterNome('')
                setNomeInput('')
                if (e.target.value !== 'projeto') setFilterPacote('')
              }}
              disabled={!filterArquivo}
            >
              <option value="projeto">{t('dependencies.filterTipoProjeto')}</option>
              <option value="dependencia">{t('dependencies.filterTipoDependencia')}</option>
            </select>
          </div>

          {/* nome — autocomplete */}
          <div className="dep-page__filter-group dep-page__filter-group--autocomplete">
            <label className="dep-page__label">{t('dependencies.filterNome')}</label>
            <div className="dep-page__autocomplete">
              <input
                className="dep-page__input"
                type="text"
                placeholder={t('dependencies.filterNomePlaceholder')}
                value={nomeInput}
                onChange={(e) => {
                  setNomeInput(e.target.value)
                  setFilterNome('')
                  setShowNomeSuggestions(true)
                }}
                onFocus={() => setShowNomeSuggestions(true)}
                onBlur={() => setTimeout(() => setShowNomeSuggestions(false), 150)}
                disabled={!filterArquivo}
              />
              {showNomeSuggestions && filteredNomeSuggestions.length > 0 && (
                <ul className="dep-page__suggestions">
                  {filteredNomeSuggestions.slice(0, 50).map((n) => (
                    <li key={n}>
                      <button
                        type="button"
                        className="dep-page__suggestion-item"
                        onMouseDown={() => {
                          setNomeInput(n)
                          setFilterNome(n)
                          setShowNomeSuggestions(false)
                        }}
                      >
                        {n}
                      </button>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>

          {/* pacote */}
          <div className="dep-page__filter-group">
            <label className="dep-page__label">{t('dependencies.filterPacote')}</label>
            <input
              className="dep-page__input"
              type="text"
              placeholder={t('dependencies.filterPacotePlaceholder')}
              value={filterPacote}
              onChange={(e) => setFilterPacote(e.target.value)}
              disabled={!filterArquivo || filterTipo !== 'projeto'}
            />
          </div>

          {/* query button */}
          <div className="dep-page__filter-group dep-page__filter-group--btn">
            <button
              className="dep-page__btn dep-page__btn--primary"
              onClick={handleConsultar}
              disabled={!queryEnabled || querying}
            >
              {t('dependencies.queryButton')}
            </button>
          </div>
        </div>
      </section>

      {/* ── Graph ── */}
      {resultado && resultado.length > 0 && (
        <section className="dep-page__section">
          <DependenciaGraph dados={resultado} />
        </section>
      )}

      {/* ── Table ── */}
      {resultado && resultado.length > 0 && (
        <section className="dep-page__section">
          <DependenciaTabela
            dados={resultado}
            tipo={filterTipo}
            arquivo={filterArquivo}
          />
        </section>
      )}
    </div>
  )
}
