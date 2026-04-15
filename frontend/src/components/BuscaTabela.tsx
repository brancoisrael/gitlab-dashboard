import { useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import * as XLSX from 'xlsx'
import { Download } from 'lucide-react'
import type { ProjetoBusca } from '../types/busca.types'
import './BuscaTabela.css'

interface BuscaTabelaProps {
  dados: ProjetoBusca[]
  extensao: string
  texto: string
}

export function BuscaTabela({ dados, extensao, texto }: BuscaTabelaProps) {
  const { t } = useTranslation()

  const totalArquivos = dados.reduce((acc, p) => acc + p.arquivos.length, 0)

  const handleDownload = useCallback(() => {
    const colProjeto  = t('search.table.projeto')
    const colArquivo  = t('search.table.arquivo')
    const colUrl      = t('search.table.url')

    const rows = dados.flatMap((p) =>
      p.arquivos.map((arq) => ({
        [colProjeto]: p.nomeProjeto,
        [colArquivo]: arq.nomeArquivo,
        [colUrl]:     arq.urlArquivo,
      })),
    )

    const ws = XLSX.utils.json_to_sheet(rows)
    ws['!cols'] = [{ wch: 40 }, { wch: 36 }, { wch: 80 }]
    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, ws, t('search.table.sheetName'))
    const safeName = texto.replace(/[^a-z0-9_\-]/gi, '_').slice(0, 30)
    XLSX.writeFile(wb, `busca_${safeName}.xlsx`)
  }, [dados, texto, t])

  return (
    <div className="dep-tabela">
      {/* ── Header igual ao DependenciaTabela ── */}
      <div className="dep-tabela__header">
        <span className="dep-tabela__count">
          {t('search.table.results', {
            projetos: dados.length,
            arquivos: totalArquivos,
          })}
        </span>
        <div className="busca-tabela__header-right">
          <span className="busca-tabela__criteria">
            {t('search.table.criteria', { extensao, texto })}
          </span>
          <button
            type="button"
            className="dep-tabela__download"
            onClick={handleDownload}
            disabled={dados.length === 0}
          >
            <Download size={14} />
            {t('search.table.download')}
          </button>
        </div>
      </div>

      {/* ── Projetos ── */}
      <div className="dep-tabela__scroll">
        {dados.map((projeto) => (
          <div key={projeto.nomeProjeto} className="busca-tabela__projeto">
            <div className="busca-tabela__projeto-nome">
              <span aria-hidden="true">📁</span>
              {projeto.nomeProjeto}
              <span className="busca-tabela__badge">{projeto.arquivos.length}</span>
            </div>
            <table className="dep-tabela__table">
              <thead>
                <tr>
                  <th className="dep-tabela__th">{t('search.table.arquivo')}</th>
                  <th className="dep-tabela__th">{t('search.table.url')}</th>
                </tr>
              </thead>
              <tbody>
                {projeto.arquivos.map((arq) => (
                  <tr key={arq.urlArquivo} className="dep-tabela__row">
                    <td className="dep-tabela__td busca-tabela__nome-arquivo">
                      {arq.nomeArquivo}
                    </td>
                    <td className="dep-tabela__td">
                      <a
                        href={arq.urlArquivo}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="dep-tabela__link"
                      >
                        {t('search.table.openFile')}
                      </a>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ))}
      </div>
    </div>
  )
}
