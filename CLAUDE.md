# GitLab Dashboard — Workspace

## Visão Geral
Projeto de dashboards analíticos com dados oriundos do GitLab.
O workspace contém dois módulos: `backend/` (Quarkus) e `frontend/` (React + TypeScript + Vite).

---

## Requisitos do Produto

Documentados em [REQUIREMENTS.md](REQUIREMENTS.md).

---

## Regras Globais (aplicam-se a todo o workspace)

### Internacionalização
- Idioma padrão: **português (pt-BR)**; suporte obrigatório a **inglês (en)**
- Toda string visível ao usuário deve passar pelo mecanismo de i18n — nunca hardcodar texto na UI

### Componentização e Reuso
- Extrair componentes/funções reutilizáveis sempre que a lógica ou o markup se repetir
- Não duplicar código entre módulos — criar abstrações compartilhadas quando necessário

### Qualidade de Código
- Integrar métricas do **SonarQube** (cobertura, code smells, duplicações, vulnerabilidades)
- Zero imports ociosos e zero código morto (variáveis, funções, tipos não utilizados)

### Persistência de Dados
- **Sem banco de dados** SQL ou NoSQL; sem contexto de autenticação ou autorização
- Dados tratados gravados em formato **Parquet**

### Empacotamento — Container Único
- Frontend e backend servidos a partir de **um único container**
- `pom.xml` do backend executa o build do frontend via `frontend-maven-plugin` e copia artefatos para `META-INF/resources/`
- Backend expõe o frontend na raiz `/`; contexto do backend é `/api`

---

## Ambiente de Desenvolvimento

**Pré-requisitos:** Java 25, Maven 3.9+, Node.js 22+

**Comandos principais:**
- Build completo: `cd backend && ./mvnw process-resources quarkus:dev`
- Só backend: `cd backend && ./mvnw quarkus:dev -Dskip.frontend`
- Frontend isolado: `cd frontend && npm run dev` (proxy `/api` → `http://localhost:8080` via `vite.config.ts`)

---

## Configuração da Aplicação

- Toda configuração declarada explicitamente no `application.yaml` com defaults.
- Segredos **nunca** têm valor real no arquivo — usar `${VAR:CONFIGURE_ME}` como fallback inválido intencional.
- Nunca usar `System.getenv()` — sempre via `@ConfigProperty` ou `@ConfigMapping`.
- `backend/.env` no `.gitignore`; `backend/.env.example` commitado com todas as variáveis (sem valores reais).

### Variáveis de Ambiente GitLab

| Variável de ambiente   | Propriedade Quarkus        | Descrição                       |
|------------------------|----------------------------|---------------------------------|
| `GITLAB_URL`           | `gitlab.url`               | URL base da instância GitLab    |
| `GITLAB_TOKEN`         | `gitlab.token`             | Personal Access Token           |
| `GITLAB_PROJECTS_PATH` | `gitlab.api.projects-path` | Caminho do endpoint de projetos |

---

## Sincronização GitLab → Parquet

- Paginação completa, escrita incremental (página a página) — sem acumular tudo em memória.
- Armazenamento configurável: `storage.type: local` ou `storage.type: s3` (ver `application.yaml`).

### Fault Tolerance (MicroProfile)

| Comportamento | Configuração |
|---|---|
| Retry em erros 5xx e falhas de conexão | `maxRetries=3`, `delay=2s` |
| Sem retry em erros 4xx (401, 403, 404) | `retryOn` não inclui `ClientErrorException` |
| Circuit Breaker abre após 50% de falhas em 10 req. | `requestVolumeThreshold=10`, `failureRatio=0.5` |
| Recuperação após 30 s | `delay=30s`, `successThreshold=2` |

### Parquet sem Hadoop

Usa `parquet-avro` + `avro` sem `hadoop-client-runtime`. Escrita local via `LocalOutputFile` customizado. Para S3, arquivo gravado em disco temporário e enviado via AWS SDK v2.

> **Native Image:** não suportado nesta configuração sem registro adicional de reflection para GraalVM.

<!-- code-review-graph MCP tools -->
## MCP Tools: code-review-graph

**IMPORTANT: This project has a knowledge graph. ALWAYS use the
code-review-graph MCP tools BEFORE using Grep/Glob/Read to explore
the codebase.** The graph is faster, cheaper (fewer tokens), and gives
you structural context (callers, dependents, test coverage) that file
scanning cannot.

### When to use graph tools FIRST

- **Exploring code**: `semantic_search_nodes` or `query_graph` instead of Grep
- **Understanding impact**: `get_impact_radius` instead of manually tracing imports
- **Code review**: `detect_changes` + `get_review_context` instead of reading entire files
- **Finding relationships**: `query_graph` with callers_of/callees_of/imports_of/tests_for
- **Architecture questions**: `get_architecture_overview` + `list_communities`

Fall back to Grep/Glob/Read **only** when the graph doesn't cover what you need.

### Key Tools

| Tool | Use when |
|------|----------|
| `detect_changes` | Reviewing code changes — gives risk-scored analysis |
| `get_review_context` | Need source snippets for review — token-efficient |
| `get_impact_radius` | Understanding blast radius of a change |
| `get_affected_flows` | Finding which execution paths are impacted |
| `query_graph` | Tracing callers, callees, imports, tests, dependencies |
| `semantic_search_nodes` | Finding functions/classes by name or keyword |
| `get_architecture_overview` | Understanding high-level codebase structure |
| `refactor_tool` | Planning renames, finding dead code |

### Workflow

1. The graph auto-updates on file changes (via hooks).
2. Use `detect_changes` for code review.
3. Use `get_affected_flows` to understand impact.
4. Use `query_graph` pattern="tests_for" to check coverage.
