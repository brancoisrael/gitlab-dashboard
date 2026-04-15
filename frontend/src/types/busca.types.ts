export interface ArquivoBuscado {
  nomeArquivo: string
  urlArquivo: string
}

export interface ProjetoBusca {
  nomeProjeto: string
  arquivos: ArquivoBuscado[]
}
