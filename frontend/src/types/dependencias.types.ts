export interface DependenciaItem {
  nome: string
  versao: string | null
  url: string | null
}

export interface DependenciaConsulta {
  tipo: 'projeto' | 'dependencia'
  nome: string
  url: string | null
  lista: DependenciaItem[]
}
