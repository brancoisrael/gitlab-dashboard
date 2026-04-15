import type { ProjetoGrupo } from '../types/projetos.types'
import './GrupoCard.css'

const CARD_COLORS = [
  { bg: '#dbeafe', border: '#3b82f6', text: '#1e40af' },
  { bg: '#dcfce7', border: '#22c55e', text: '#15803d' },
  { bg: '#fef9c3', border: '#eab308', text: '#854d0e' },
  { bg: '#fce7f3', border: '#ec4899', text: '#9d174d' },
  { bg: '#ede9fe', border: '#8b5cf6', text: '#5b21b6' },
  { bg: '#ffedd5', border: '#f97316', text: '#9a3412' },
  { bg: '#cffafe', border: '#06b6d4', text: '#155e75' },
  { bg: '#fef2f2', border: '#ef4444', text: '#991b1b' },
]

interface GrupoCardProps {
  readonly grupo: ProjetoGrupo
  readonly index: number
  readonly selected?: boolean
  readonly onClick: () => void
}

export function GrupoCard({ grupo, index, selected, onClick }: GrupoCardProps) {
  const color = CARD_COLORS[index % CARD_COLORS.length]

  return (
    <button
      type="button"
      className={`grupo-card${selected ? ' grupo-card--selected' : ''}`}
      style={{
        backgroundColor: color.bg,
        borderColor: color.border,
        ...(selected ? { outline: `3px solid ${color.border}`, outlineOffset: '2px' } : {}),
      }}
      onClick={onClick}
    >
      <span className="grupo-card__title" style={{ color: color.text }}>
        {grupo.grupo}
      </span>
      <span className="grupo-card__count" style={{ color: color.text }}>
        {grupo.count.toLocaleString('pt-BR')}
      </span>
    </button>
  )
}
