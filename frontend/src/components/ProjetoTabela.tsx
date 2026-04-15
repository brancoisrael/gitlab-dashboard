import { useCallback, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import * as XLSX from 'xlsx'
import { ChevronDown, ChevronUp, ChevronsUpDown, Download, Search, X } from 'lucide-react'
import type { ProjetoDetalhe } from '../types/projetos.types'
import './DependenciaTabela.css'

interface Props {
  readonly dados: ProjetoDetalhe[]
  readonly grupo: string
}

type ColOrdenavel = 'nome' | 'criadoEm' | 'ultimaAtividade'
type Direcao = 'asc' | 'desc'

interface Ordenacao {
  col: ColOrdenavel
  dir: Direcao
}

function formatarData(iso: string | null | undefined, locale: string): string {
  if (!iso) return '—'
  try {
    return new Intl.DateTimeFormat(locale, {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }).format(new Date(iso))
  } catch {
    return iso
  }
}

function toTimestamp(iso: string | null | undefined): number {
  if (!iso) return 0
  const ts = Date.parse(iso)
  return isNaN(ts) ? 0 : ts
}

function SortIcon({ col, ordenacao }: { col: ColOrdenavel; ordenacao: Ordenacao | null }) {
  if (!ordenacao || ordenacao.col !== col) return <ChevronsUpDown size={12} className="dep-tabela__sort-icon dep-tabela__sort-icon--idle" />
  return ordenacao.dir === 'asc'
    ? <ChevronUp size={12} className="dep-tabela__sort-icon dep-tabela__sort-icon--active" />
    : <ChevronDown size={12} className="dep-tabela__sort-icon dep-tabela__sort-icon--active" />
}

export function ProjetoTabela({ dados, grupo }: Props) {
  const { t, i18n } = useTranslation()
  const locale = i18n.language

  const [ordenacao, setOrdenacao] = useState<Ordenacao | null>(null)
  const [filtroNome, setFiltroNome] = useState('')

  const colNome      = t('dashboard.table.nome')
  const colUrl       = t('dashboard.table.url')
  const colCriado    = t('dashboard.table.criadoEm')
  const colAtividade = t('dashboard.table.ultimaAtividade')

  function alternarOrdenacao(col: ColOrdenavel) {
    setOrdenacao((prev) => {
      if (!prev || prev.col !== col) return { col, dir: 'asc' }
      if (prev.dir === 'asc') return { col, dir: 'desc' }
      return null
    })
  }

  const dadosFiltrados = useMemo(() => {
    if (!filtroNome.trim()) return dados
    const termo = filtroNome.trim().toLowerCase()
    return dados.filter((p) => p.nome?.toLowerCase().includes(termo))
  }, [dados, filtroNome])

  const dadosOrdenados = useMemo(() => {
    if (!ordenacao) return dadosFiltrados
    return [...dadosFiltrados].sort((a, b) => {
      let cmp = 0
      if (ordenacao.col === 'nome') {
        cmp = (a.nome ?? '').localeCompare(b.nome ?? '', locale)
      } else if (ordenacao.col === 'criadoEm') {
        cmp = toTimestamp(a.criadoEm) - toTimestamp(b.criadoEm)
      } else {
        cmp = toTimestamp(a.ultimaAtividade) - toTimestamp(b.ultimaAtividade)
      }
      return ordenacao.dir === 'asc' ? cmp : -cmp
    })
  }, [dadosFiltrados, ordenacao, locale])

  const handleDownload = useCallback(() => {
    const rows = dadosOrdenados.map((p) => ({
      [colNome]:      p.nome,
      [colUrl]:       p.webUrl ?? '',
      [colCriado]:    formatarData(p.criadoEm, locale),
      [colAtividade]: formatarData(p.ultimaAtividade, locale),
    }))
    const ws = XLSX.utils.json_to_sheet(rows)
    ws['!cols'] = [{ wch: 42 }, { wch: 60 }, { wch: 22 }, { wch: 22 }]
    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, ws, t('dashboard.table.sheetName'))
    XLSX.writeFile(wb, `projetos_${grupo.replace(/\s+/g, '_')}.xlsx`)
  }, [dadosOrdenados, colNome, colUrl, colCriado, colAtividade, grupo, locale, t])

  return (
    <div className="dep-tabela">
      <div className="dep-tabela__header">
        <span className="dep-tabela__count">
          {filtroNome.trim()
            ? t('dashboard.table.filteredResults', { filtrado: dadosOrdenados.length, total: dados.length })
            : t('dashboard.table.results', { total: dados.length })}
        </span>
        <div className="dep-tabela__filtro-wrap">
          <Search size={13} className="dep-tabela__filtro-icon" />
          <input
            type="text"
            className="dep-tabela__filtro"
            placeholder={t('dashboard.table.filterNome')}
            value={filtroNome}
            onChange={(e) => setFiltroNome(e.target.value)}
          />
          {filtroNome && (
            <button
              type="button"
              className="dep-tabela__filtro-clear"
              onClick={() => setFiltroNome('')}
              aria-label="Limpar filtro"
            >
              <X size={12} />
            </button>
          )}
        </div>
        <button
          type="button"
          className="dep-tabela__download"
          onClick={handleDownload}
          disabled={dadosOrdenados.length === 0}
        >
          <Download size={14} />
          {t('dashboard.table.download')}
        </button>
      </div>

      <div className="dep-tabela__scroll">
        <table className="dep-tabela__table">
          <thead>
            <tr>
              <th className="dep-tabela__th dep-tabela__th--sortable">
                <button
                  type="button"
                  className="dep-tabela__sort-btn"
                  onClick={() => alternarOrdenacao('nome')}
                >
                  {colNome}
                  <SortIcon col="nome" ordenacao={ordenacao} />
                </button>
              </th>
              <th className="dep-tabela__th">{colUrl}</th>
              <th className="dep-tabela__th dep-tabela__th--data dep-tabela__th--sortable">
                <button
                  type="button"
                  className="dep-tabela__sort-btn"
                  onClick={() => alternarOrdenacao('criadoEm')}
                >
                  {colCriado}
                  <SortIcon col="criadoEm" ordenacao={ordenacao} />
                </button>
              </th>
              <th className="dep-tabela__th dep-tabela__th--data dep-tabela__th--sortable">
                <button
                  type="button"
                  className="dep-tabela__sort-btn"
                  onClick={() => alternarOrdenacao('ultimaAtividade')}
                >
                  {colAtividade}
                  <SortIcon col="ultimaAtividade" ordenacao={ordenacao} />
                </button>
              </th>
            </tr>
          </thead>
          <tbody>
            {dadosOrdenados.map((p) => (
              <tr key={p.nome} className="dep-tabela__row dep-tabela__row--first">
                <td className="dep-tabela__td">{p.nome}</td>
                <td className="dep-tabela__td">
                  {p.webUrl ? (
                    <a
                      href={p.webUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="dep-tabela__link"
                    >
                      {p.webUrl}
                    </a>
                  ) : (
                    <span className="dep-tabela__td--empty">—</span>
                  )}
                </td>
                <td className="dep-tabela__td dep-tabela__td--data">
                  {formatarData(p.criadoEm, locale)}
                </td>
                <td className="dep-tabela__td dep-tabela__td--data">
                  {formatarData(p.ultimaAtividade, locale)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
