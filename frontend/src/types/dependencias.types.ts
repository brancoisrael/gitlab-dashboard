export interface DependenciaItem {
  nome: string
  versao: string | null
}

export interface DependenciaConsulta {
  tipo: 'projeto' | 'dependencia'
  nome: string
  lista: DependenciaItem[]
}
