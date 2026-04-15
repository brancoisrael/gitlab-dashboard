# GitLab Dashboard — Frontend

## Stack
- **Framework:** React 19
- **Linguagem:** TypeScript
- **Build:** Vite 8
- **Linting:** ESLint 9 + typescript-eslint

> Herda todas as regras definidas em `../CLAUDE.md`.

---

## Regras do Frontend

### Componentização
- Componentizar **sempre que possível** — cada responsabilidade visual em seu próprio componente
- Componentes reutilizáveis vão em `src/components/`
- Páginas (views) vão em `src/pages/`
- Hooks customizados vão em `src/hooks/`
- Utilitários puros vão em `src/utils/`
- Nunca misturar lógica de negócio com markup JSX no mesmo arquivo

### Roteamento
- Usar rotas nomeadas e tipadas (ex.: `react-router-dom`)
- Cada funcionalidade principal tem sua própria rota (`/dashboard/...`, `/settings`, etc.)
- Nenhuma lógica de autenticação nas rotas — sem guards de auth

### Internacionalização (i18n)
- Usar biblioteca de i18n (ex.: `react-i18next`) com arquivos de tradução separados por idioma
- Estrutura: `src/i18n/locales/pt-BR.json` e `src/i18n/locales/en.json`
- Idioma padrão: **pt-BR**; fallback: **en**
- Nunca hardcodar strings visíveis ao usuário no JSX

### Interface e UX
- Design **moderno**: clean, espaçamento generoso, tipografia clara
- **Cores claras** com bom contraste (WCAG AA no mínimo)
- Paleta consistente definida em variáveis CSS ou tema centralizado
- **Menu lateral rebatível** (sidebar collapsible) presente em todas as páginas internas
- **100% responsivo** — compatível com mobile, tablet e desktop
- Usar CSS Grid / Flexbox; evitar posicionamentos absolutos frágeis

### Notificações ao Usuário — Toast (padrão obrigatório)
- **Todo feedback ao usuário** (sucesso, erro, aviso, informação) deve ser exibido via **toast message** — nunca inline, alert() ou console
- Toasts ficam centralizados no topo da tela (`position: fixed; top: 24px; left: 50%; transform: translateX(-50%)`)
- Usar o hook `useToast()` de `src/components/Toast/ToastContext.tsx` para disparar toasts
- A variante define a cor seguindo as variáveis CSS de feedback:

| Variante   | Cor de texto          | Cor de fundo          | Uso                                      |
|------------|-----------------------|-----------------------|------------------------------------------|
| `success`  | `--color-success`     | `--color-success-bg`  | Operações concluídas com sucesso         |
| `error`    | `--color-error`       | `--color-error-bg`    | Erros e falhas                           |
| `warning`  | `--color-warning`     | `--color-warning-bg`  | Alertas e situações que exigem atenção   |
| `info`     | `--color-info`        | `--color-info-bg`     | Informações e operações em andamento     |

- Duração padrão: **4 000 ms**; toasts informativos de longa espera podem usar até **8 000 ms**
- O `ToastProvider` deve envolver toda a árvore em `App.tsx`; o `ToastContainer` deve ser filho direto do provider, fora do layout de grid

### Gráficos e Visualizações
- Usar biblioteca de gráficos moderna (ex.: **Recharts**, **Apache ECharts**, **Nivo**)
- Gráficos devem ser **interativos** sempre que possível (tooltip, zoom, click)
- Suporte a **drilldown** nas análises — clicar em um ponto/barra deve abrir nível de detalhe maior
- Preferir gráficos de linha/área para séries temporais, barras para comparações, treemap/sunburst para hierarquias

### Configuração de Persistência
- Expor tela de configurações (`/settings`) para escolher o destino dos arquivos Parquet:
  - **Local** (desenvolvimento): campo de caminho de diretório
  - **S3/MinIO** (produção): endpoint, bucket, access key, secret key
- Salvar configuração via chamada ao backend (`POST /api/settings/storage`)
- Não persistir credenciais no `localStorage` — delegar ao backend

### Qualidade de Código
- Zero imports sem utilização — o ESLint deve reportar `no-unused-vars` como erro
- Tipar **tudo** explicitamente — evitar `any`; usar `unknown` quando o tipo for incerto
- Componentes funcionais com tipagem de props via `interface` ou `type`
- Separar tipos/interfaces em arquivos `*.types.ts` quando o arquivo crescer

### Estrutura de Pastas (convenção)
```
src/
├── components/        # componentes reutilizáveis
│   └── layout/        # Sidebar, Header, PageWrapper…
├── pages/             # views/rotas
├── hooks/             # hooks customizados
├── services/          # chamadas HTTP ao backend (/api)
├── i18n/
│   └── locales/       # pt-BR.json, en.json
├── store/             # estado global (se necessário)
├── types/             # tipos globais
└── utils/             # funções puras utilitárias
```
