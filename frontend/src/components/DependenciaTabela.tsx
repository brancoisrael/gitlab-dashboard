import { useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import * as XLSX from 'xlsx'
import { Download } from 'lucide-react'
import type { DependenciaConsulta } from '../types/dependencias.types'
import './DependenciaTabela.css'

interface Props {
  readonly dados: DependenciaConsulta[]
  readonly tipo: string
  readonly arquivo: string
}

interface FlatRow {
  grupo: string
  item: string
  versao: string
}

function flatten(dados: DependenciaConsulta[]): FlatRow[] {
  return dados.flatMap((d) =>
    d.lista.length > 0
      ? d.lista.map((it) => ({ grupo: d.nome, item: it.nome, versao: it.versao ?? '' }))
      : [{ grupo: d.nome, item: '', versao: '' }],
  )
}

export function DependenciaTabela({ dados, tipo, arquivo }: Props) {
  const { t } = useTranslation()

  const isProjeto = tipo === 'projeto'
  const colGrupo  = isProjeto ? t('dependencies.table.project')      : t('dependencies.table.dependency')
  const colItens  = isProjeto ? t('dependencies.table.dependencies')  : t('dependencies.table.projects')
  const colVersao = t('dependencies.table.version')

  const handleDownload = useCallback(() => {
    const rows = flatten(dados).map((r) => ({
      [colGrupo]:  r.grupo,
      [colItens]:  r.item,
      [colVersao]: r.versao,
    }))
    const ws = XLSX.utils.json_to_sheet(rows)
    ws['!cols'] = [{ wch: 42 }, { wch: 42 }, { wch: 18 }]
    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, ws, t('dependencies.table.sheetName'))
    XLSX.writeFile(wb, `dependencias_${arquivo.replace('.', '_')}.xlsx`)
  }, [dados, colGrupo, colItens, colVersao, arquivo, t])

  const totalItens = dados.reduce((acc, d) => acc + Math.max(d.lista.length, 1), 0)

  return (
    <div className="dep-tabela">
      <div className="dep-tabela__header">
        <span className="dep-tabela__count">
          {t('dependencies.table.results', { grupos: dados.length, itens: totalItens })}
        </span>
        <button
          type="button"
          className="dep-tabela__download"
          onClick={handleDownload}
          disabled={dados.length === 0}
        >
          <Download size={14} />
          {t('dependencies.table.download')}
        </button>
      </div>

      <div className="dep-tabela__scroll">
        <table className="dep-tabela__table">
          <thead>
            <tr>
              <th className="dep-tabela__th">{colGrupo}</th>
              <th className="dep-tabela__th">{colItens}</th>
              <th className="dep-tabela__th dep-tabela__th--versao">{colVersao}</th>
            </tr>
          </thead>
          <tbody>
            {dados.map((item) =>
              item.lista.length === 0 ? (
                <tr key={item.nome} className="dep-tabela__row">
                  <td className="dep-tabela__td dep-tabela__td--group">{item.nome}</td>
                  <td className="dep-tabela__td dep-tabela__td--empty">—</td>
                  <td className="dep-tabela__td dep-tabela__td--empty">—</td>
                </tr>
              ) : (
                item.lista.map((it, idx) => (
                  <tr
                    key={`${item.nome}||${it.nome}||${idx}`}
                    className={`dep-tabela__row${idx === 0 ? ' dep-tabela__row--first' : ' dep-tabela__row--cont'}`}
                  >
                    {idx === 0 && (
                      <td
                        className="dep-tabela__td dep-tabela__td--group"
                        rowSpan={item.lista.length}
                      >
                        {item.nome}
                      </td>
                    )}
                    <td className="dep-tabela__td">{it.nome}</td>
                    <td className="dep-tabela__td dep-tabela__td--versao">
                      {it.versao ?? <span className="dep-tabela__td--empty">—</span>}
                    </td>
                  </tr>
                ))
              ),
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}
