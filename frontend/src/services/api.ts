import type { ProjetoDetalhe, ProjetoGrupo } from '../types/projetos.types'
import type { DependenciaConsulta } from '../types/dependencias.types'
import type { ProjetoBusca } from '../types/busca.types'

const BASE = '/api'

export type FetchGruposResult =
  | { status: 'ok'; grupos: ProjetoGrupo[] }
  | { status: 'syncing' }

export async function fetchGrupos(): Promise<FetchGruposResult> {
  const res = await fetch(`${BASE}/projetos/grupos`)
  if (res.status === 202) return { status: 'syncing' }
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  const grupos = await res.json() as ProjetoGrupo[]
  return { status: 'ok', grupos }
}

export async function sincronizar(): Promise<void> {
  const res = await fetch(`${BASE}/projetos/sincronizar`, { method: 'POST' })
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
}

export async function fetchProjetosPorGrupo(grupo: string): Promise<ProjetoDetalhe[]> {
  const res = await fetch(`${BASE}/projetos/grupos/${encodeURIComponent(grupo)}`)
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return res.json() as Promise<ProjetoDetalhe[]>
}

export async function fetchArquivosDisponiveis(): Promise<string[]> {
  const res = await fetch(`${BASE}/dependencias/arquivos`)
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return res.json() as Promise<string[]>
}

export async function sincronizarDependencias(arquivo: string): Promise<void> {
  const res = await fetch(
    `${BASE}/dependencias/sincronizar?arquivo=${encodeURIComponent(arquivo)}`,
    { method: 'POST' },
  )
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
}

export async function fetchDependenciasConsulta(
  arquivo: string,
  tipo?: string,
  nome?: string,
  pacote?: string,
): Promise<DependenciaConsulta[]> {
  const params = new URLSearchParams({ arquivo })
  if (tipo) params.set('tipo', tipo)
  if (nome) params.set('nome', nome)
  if (pacote) params.set('pacote', pacote)
  const res = await fetch(`${BASE}/dependencias/consultar?${params.toString()}`)
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return res.json() as Promise<DependenciaConsulta[]>
}

export function escutarEventosDependencias(
  onConcluido: (arquivo: string) => void,
  onErro: (arquivo: string) => void,
): EventSource {
  const es = new EventSource(`${BASE}/dependencias/sincronizar/eventos`)
  es.onmessage = (e: MessageEvent<string>) => {
    const data: string = e.data
    if (data.startsWith('concluido:')) onConcluido(data.slice('concluido:'.length))
    else if (data.startsWith('erro:')) onErro(data.slice('erro:'.length))
  }
  es.onerror = () => {
    onErro('')
    es.close()
  }
  return es
}

export async function iniciarBusca(extensao: string, texto: string): Promise<string> {
  const params = new URLSearchParams({ extensao, texto })
  const res = await fetch(`${BASE}/busca?${params.toString()}`, { method: 'POST' })
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  const data = await res.json() as { id: string }
  return data.id
}

export function escutarEventosBusca(
  onConcluido: (id: string) => void,
  onErro: (id: string) => void,
): EventSource {
  const es = new EventSource(`${BASE}/busca/eventos`)
  es.onmessage = (e: MessageEvent<string>) => {
    const data: string = e.data
    if (data.startsWith('concluido:')) onConcluido(data.slice('concluido:'.length))
    else if (data.startsWith('erro:')) onErro(data.slice('erro:'.length))
  }
  es.onerror = () => {
    onErro('')
    es.close()
  }
  return es
}

export async function fetchResultadoBusca(id: string): Promise<ProjetoBusca[]> {
  const res = await fetch(`${BASE}/busca/resultado/${encodeURIComponent(id)}`)
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return res.json() as Promise<ProjetoBusca[]>
}

export function escutarEventosSincronizacao(
  onConcluido: () => void,
  onErro: () => void,
): EventSource {
  const es = new EventSource(`${BASE}/projetos/sincronizar/eventos`)
  es.onmessage = (e) => {
    if (e.data === 'concluido') onConcluido()
    else if (e.data === 'erro') onErro()
    es.close()
  }
  es.onerror = () => {
    onErro()
    es.close()
  }
  return es
}
