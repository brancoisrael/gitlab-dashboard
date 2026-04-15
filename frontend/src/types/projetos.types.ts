export interface ProjetoGrupo {
  grupo: string
  count: number
}

export interface ProjetoDetalhe {
  nome: string
  webUrl: string | null
  criadoEm: string | null
  ultimaAtividade: string | null
}
