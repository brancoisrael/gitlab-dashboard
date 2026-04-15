# Requisitos do Produto

> **Regra de documentação:** sempre que o usuário descrever ou mencionar um requisito usando a forma
> "requisito N" (ex.: "requisito 1", "ajuste no requisito 2"), o assistente deve **criar ou atualizar**
> a entrada correspondente neste arquivo, refletindo o estado atual acordado.

---

### Requisito 1 — Sincronização e Dashboard de Grupos

**Backend**

- `POST /api/projetos/sincronizar` — dispara sincronização assíncrona de todos os projetos GitLab para Parquet.
  - Cada execução bem-sucedida **subscreve** o arquivo anterior (nome fixo: `projetos.parquet`).
  - Operação em thread virtual; retorna 200 imediatamente; execuções concorrentes são ignoradas.
- `GET /api/projetos/grupos` — lê o Parquet e retorna lista de `{ grupo, count }` ordenada por `count` decrescente.
  - Regra de agrupamento: primeiro segmento de `name_with_namespace` antes do `" / "`.
    Ex.: `"ContaPag / repo-x"` → grupo `"ContaPag"`.
  - Retorna 404 se o Parquet ainda não foi gerado.

**Frontend — Tela inicial (`/`)**

- Ao carregar, consome `GET /api/projetos/grupos` automaticamente.
- Exibe uma lista de cards alinhados horizontalmente (flex-wrap), cada card com:
  - Cor própria (paleta rotativa).
  - Título = nome do grupo.
  - Valor central em destaque = `count`.
- Dois botões no canto superior esquerdo (apenas ícone, label via hover/tooltip):
  - **Sincronizar** — chama `POST /api/projetos/sincronizar` e recarrega após 3 s.
  - **Atualizar** — recarrega os dados do endpoint de grupos.

---

### Requisito 2 — Catalogação de Dependências por Arquivo

**Backend**

- `POST /api/dependencias/sincronizar?arquivo={nome.extensao}` — recebe o nome completo do arquivo de dependências (ex.: `pom.xml`, `package.json`) e dispara a catalogação assíncrona.
  - Operação em thread virtual; retorna 200 imediatamente; execuções concorrentes para o mesmo arquivo são ignoradas.
  - Percorre todos os projetos GitLab (via Parquet já sincronizado do Requisito 1) e busca o arquivo informado via API GitLab (`/repository/files/{arquivo}`).
  - Extrai as dependências de cada projeto e persiste o resultado em Parquet.

**Nomenclatura do arquivo Parquet:**
- Padrão: `dependencie_<arquivo>.parquet`, onde `<arquivo>` é o nome do parâmetro com `.` substituído por `_`.
- Exemplos: `pom.xml` → `dependencie_pom_xml.parquet` | `package.json` → `dependencie_package_json.parquet`
- Cada execução bem-sucedida **subscreve** o arquivo anterior (mesmo nome fixo).

**Schema do Parquet — colunas obrigatórias (sempre presentes, mesmo sem valor):**

| Coluna            | Tipo   | Descrição                                              |
|-------------------|--------|--------------------------------------------------------|
| `aplicacao`       | String | Nome do projeto GitLab                                 |
| `dependencia`     | String | Nome da dependência/biblioteca                         |
| `versao`          | String | Versão declarada (pode ser `null` se não informada)    |
| `pacote`          | String | Namespace/groupId/package (quando aplicável, ou `null`)|
| `gerenciador`     | String | Ferramenta que gerencia a dependência (ex.: `maven`)   |
| `arquivo_origem`  | String | Nome do arquivo de onde a dependência foi extraída     |

> Todas as colunas devem existir em todos os arquivos gerados, independentemente da linguagem, para garantir equidade de schema entre os Parquets.

**Linguagens e gerenciadores suportados (implementação inicial):**

| Arquivo buscado     | Linguagem  | Gerenciador | Observação                                         |
|---------------------|------------|-------------|----------------------------------------------------|
| `pom.xml`           | Java       | Maven       | Extrair `<dependencies>`: groupId, artifactId, version |
| `build.gradle`      | Java       | Gradle      | Extrair bloco `dependencies { ... }`               |
| `package.json`      | Node.js    | npm/yarn    | Extrair `dependencies` e `devDependencies`         |
| `pubspec.yaml`      | Flutter    | pub         | Extrair bloco `dependencies:`                      |
| `requirements.txt`  | Python     | pip         | Uma dependência por linha (`pacote==versão`)       |
| `pyproject.toml`    | Python     | poetry/pip  | Extrair `[tool.poetry.dependencies]` ou `[project].dependencies` |
| `composer.json`     | PHP        | Composer    | Extrair `require` e `require-dev`                  |

**Arquitetura — Extensibilidade:**
- Criar interface/contrato `DependencyParser` (ou equivalente) com método `parse(conteudo: String): List<Dependencia>`.
- Cada linguagem/gerenciador implementa o contrato em classe própria.
- Um `DependencyParserRegistry` mapeia nome de arquivo → parser correspondente.
- Arquivos não reconhecidos retornam lista vazia e são registrados em log (sem falha na sincronização geral).
- Novas linguagens são adicionadas implementando o contrato e registrando no mapa — sem alterar a lógica principal.
