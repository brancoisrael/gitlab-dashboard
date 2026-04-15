# GitLab Dashboard

Dashboard analítico para visualização e exploração de dados oriundos de uma instância GitLab. Permite sincronizar projetos, catalogar dependências por gerenciador de pacotes e realizar buscas de conteúdo em todos os repositórios.

---

## Sumário

- [Visão Geral](#visão-geral)
- [Funcionalidades](#funcionalidades)
- [Arquitetura](#arquitetura)
- [Stack Tecnológica](#stack-tecnológica)
- [Pré-requisitos](#pré-requisitos)
- [Desenvolvimento Local](#desenvolvimento-local)
- [Produção — Container Único](#produção--container-único)
- [Variáveis de Ambiente](#variáveis-de-ambiente)
- [Documentação da API](#documentação-da-api)

---

## Visão Geral

O GitLab Dashboard conecta-se à API do GitLab via Personal Access Token, sincroniza os dados dos projetos em arquivos **Parquet** (sem banco de dados relacional) e expõe uma interface React moderna com suporte a português (pt-BR) e inglês (en).

Todo o sistema roda em um **container único**: o backend Quarkus serve o frontend React compilado na raiz `/`, enquanto a API fica sob `/api`.

---

## Funcionalidades

### Dashboard de Grupos

Visualização dos projetos GitLab agrupados pelo primeiro segmento do namespace (ex.: `ContaPag / repo-x` → grupo `ContaPag`).

- Cards coloridos com contagem de projetos por grupo
- Clique no card para expandir a lista de projetos do grupo com filtro e exportação XLSX
- Botão de sincronização com feedback em tempo real via Server-Sent Events (SSE)

![Dashboard de Grupos](docs/screenshots/dashboard.png)

---

### Catalogação de Dependências

Varredura em todos os repositórios GitLab em busca de arquivos de manifesto de dependências. Os dados extraídos são persistidos em Parquet e consultáveis por projeto ou por dependência.

**Gerenciadores suportados:**

| Arquivo          | Linguagem  | Gerenciador    |
|------------------|------------|----------------|
| `pom.xml`        | Java       | Maven          |
| `build.gradle`   | Java       | Gradle         |
| `package.json`   | Node.js    | npm / yarn     |
| `pubspec.yaml`   | Flutter    | pub            |
| `requirements.txt` | Python   | pip            |
| `pyproject.toml` | Python     | poetry / pip   |
| `composer.json`  | PHP        | Composer       |

- Filtros por arquivo de manifesto, tipo (projeto ou dependência), nome e pacote
- Autocomplete no campo de nome com base nos dados sincronizados
- Gráfico interativo de relacionamento projeto ↔ dependência
- Tabela detalhada com exportação XLSX

![Catalogação de Dependências](docs/screenshots/dependencias.png)

---

### Busca de Conteúdo

Busca textual em arquivos de todos os repositórios GitLab, filtrando por extensão de arquivo.

- Campo de extensão com sugestões (`.java`, `.ts`, `.py`, `.go` etc.)
- Execução assíncrona com progresso em tempo real via SSE
- Botão para cancelar a busca em andamento
- Resultados agrupados por projeto com link direto para o arquivo no GitLab
- Exportação dos resultados em XLSX

![Busca de Conteúdo](docs/screenshots/busca.png)

---

### Configurações de Armazenamento

Tela para configurar o destino dos arquivos Parquet sem necessidade de reiniciar a aplicação.

- **Local:** caminho de diretório no filesystem do container
- **S3 / MinIO:** endpoint, bucket, região, access key e secret key

![Configurações](docs/screenshots/settings.png)

---

## Arquitetura

```
gitlab-dashboard/
├── backend/          # Quarkus (Java 25) — API REST + serve o frontend
│   ├── src/main/
│   │   ├── java/br/com/ib/gitdashboards/
│   │   │   ├── resource/     # endpoints JAX-RS (/api/*)
│   │   │   ├── service/      # lógica de negócio e sincronização
│   │   │   ├── storage/      # abstração Parquet (local / S3)
│   │   │   ├── parser/       # parsers de dependências por linguagem
│   │   │   ├── client/       # REST client GitLab
│   │   │   ├── dto/          # records e DTOs
│   │   │   ├── config/       # configurações Quarkus
│   │   │   ├── i18n/         # resolução de mensagens (pt-BR / en)
│   │   │   └── exception/    # mapeamento global de erros
│   │   └── resources/
│   │       ├── application.yaml
│   │       ├── messages.properties        # pt-BR
│   │       └── messages_en.properties     # en
│   └── pom.xml       # inclui frontend-maven-plugin para build integrado
│
└── frontend/         # React 19 + TypeScript + Vite
    └── src/
        ├── pages/        # Dashboard, Dependências, Busca, Configurações
        ├── components/   # GrupoCard, gráficos, tabelas, layout, Toast
        ├── hooks/        # useGrupos e outros hooks customizados
        ├── services/     # chamadas HTTP e SSE ao backend
        ├── types/        # interfaces e tipos TypeScript
        └── i18n/locales/ # pt-BR.json, en.json
```

**Fluxo de dados:**

```
GitLab API → GitLabClient → SyncService → Parquet (local ou S3)
                                              ↓
                                       ParquetQueryService → REST endpoints → Frontend
```

**Fault Tolerance (MicroProfile):**
- Retry automático em erros 5xx (máx. 3 tentativas, delay 2 s)
- Circuit Breaker: abre após 50 % de falhas em 10 requisições, recupera após 30 s
- Operações de sincronização em virtual threads — retorno imediato 200, execução em background

---

## Stack Tecnológica

| Camada     | Tecnologia                                                    |
|------------|---------------------------------------------------------------|
| Backend    | Quarkus 3.x, Java 25, Maven, JAX-RS, MicroProfile REST Client |
| Persistência | Apache Parquet (parquet-avro), AWS SDK v2 (S3/MinIO)        |
| Frontend   | React 19, TypeScript, Vite, react-router-dom, react-i18next  |
| UI         | CSS customizado, Recharts (gráficos), Lucide React (ícones)  |
| API Docs   | SmallRye OpenAPI / Swagger UI                                 |
| Container  | OpenJDK 25 UBI (imagem JVM)                                  |

---

## Pré-requisitos

| Ferramenta | Versão mínima |
|------------|---------------|
| Java (JDK) | 25            |
| Maven      | 3.9+          |
| Node.js    | 22+           |
| npm        | 10+           |
| Docker     | 24+ (apenas para produção) |

---

## Desenvolvimento Local

### 1. Clonar o repositório

```bash
git clone <url-do-repositório>
cd gitlab-dashboard
```

### 2. Configurar variáveis de ambiente do backend

```bash
cp backend/.env.example backend/.env
```

Edite `backend/.env` com os valores reais:

```env
GITLAB_URL=https://seu-gitlab.exemplo.com
GITLAB_TOKEN=glpat-XXXXXXXXXXXXXXXXXXXX
GITLAB_PROJECTS_PATH=/api/v4/projects
```

### 3. Opção A — Backend + Frontend juntos (build completo)

Compila o frontend e o emparelha no classpath do Quarkus:

```bash
cd backend
./mvnw process-resources quarkus:dev
```

Acesse: `http://localhost:8080`

### 3. Opção B — Backend isolado (sem recompilar o frontend)

```bash
cd backend
./mvnw quarkus:dev -Dskip.frontend
```

API disponível em: `http://localhost:8080/api`

### 3. Opção C — Frontend isolado (hot reload)

Em um terminal separado, com o backend já rodando na porta 8080:

```bash
cd frontend
npm install
npm run dev
```

O `vite.config.ts` já está configurado com proxy `/api → http://localhost:8080`.

Acesse: `http://localhost:5173`

### Dev UI do Quarkus

Disponível em modo dev: `http://localhost:8080/api/q/dev/`

### Swagger UI

Disponível em modo dev: `http://localhost:8080/api/q/swagger-ui`

---

## Produção — Container Único

### 1. Configurar variáveis de ambiente

Crie um arquivo `.env` ou exporte as variáveis diretamente no shell (veja a seção [Variáveis de Ambiente](#variáveis-de-ambiente)).

### 2. Build do JAR

O build do Maven compila o frontend automaticamente via `frontend-maven-plugin` e copia os artefatos para `META-INF/resources/`:

```bash
cd backend
./mvnw package -DskipTests
```

O artefato gerado fica em `backend/target/quarkus-app/`.

### 3. Build da imagem Docker

```bash
cd backend
docker build -f src/main/docker/Dockerfile.jvm \
  -t gitlab-dashboard:latest .
```

### 4. Executar o container

#### Armazenamento local

```bash
docker run -d \
  --name gitlab-dashboard \
  -p 8080:8080 \
  -e GITLAB_URL=https://seu-gitlab.exemplo.com \
  -e GITLAB_TOKEN=glpat-XXXXXXXXXXXXXXXXXXXX \
  -e GITLAB_PROJECTS_PATH=/api/v4/projects \
  -v /data/parquet:/tmp/gitlab-dashboard/parquet \
  gitlab-dashboard:latest
```

#### Armazenamento S3 / MinIO

```bash
docker run -d \
  --name gitlab-dashboard \
  -p 8080:8080 \
  -e GITLAB_URL=https://seu-gitlab.exemplo.com \
  -e GITLAB_TOKEN=glpat-XXXXXXXXXXXXXXXXXXXX \
  -e GITLAB_PROJECTS_PATH=/api/v4/projects \
  -e S3_ENDPOINT=http://minio:9000 \
  -e S3_BUCKET=gitlab-dashboard \
  -e S3_REGION=us-east-1 \
  -e S3_ACCESS_KEY=minioadmin \
  -e S3_SECRET_KEY=minioadmin \
  gitlab-dashboard:latest
```

Acesse a aplicação em: `http://localhost:8080`

### 5. Exemplo com Docker Compose

```yaml
services:
  app:
    image: gitlab-dashboard:latest
    ports:
      - "8080:8080"
    environment:
      GITLAB_URL: https://seu-gitlab.exemplo.com
      GITLAB_TOKEN: glpat-XXXXXXXXXXXXXXXXXXXX
      GITLAB_PROJECTS_PATH: /api/v4/projects
      S3_ENDPOINT: http://minio:9000
      S3_BUCKET: gitlab-dashboard
      S3_ACCESS_KEY: minioadmin
      S3_SECRET_KEY: minioadmin
    depends_on:
      - minio

  minio:
    image: minio/minio:latest
    command: server /data --console-address ":9001"
    ports:
      - "9000:9000"
      - "9001:9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    volumes:
      - minio-data:/data

volumes:
  minio-data:
```

```bash
docker compose up -d
```

---

## Variáveis de Ambiente

### Obrigatórias

| Variável               | Descrição                                        |
|------------------------|--------------------------------------------------|
| `GITLAB_URL`           | URL base da instância GitLab (sem barra final)   |
| `GITLAB_TOKEN`         | Personal Access Token com permissão `read_api`   |
| `GITLAB_PROJECTS_PATH` | Caminho do endpoint de projetos (`/api/v4/projects`) |

### Armazenamento S3/MinIO (quando `storage.type=s3`)

| Variável        | Padrão                  | Descrição                         |
|-----------------|-------------------------|-----------------------------------|
| `S3_ENDPOINT`   | `http://localhost:9000`  | Endpoint do S3 ou MinIO           |
| `S3_BUCKET`     | `gitlab-dashboard`       | Nome do bucket                    |
| `S3_REGION`     | `us-east-1`              | Região AWS / MinIO                |
| `S3_ACCESS_KEY` | `minioadmin`             | Access Key                        |
| `S3_SECRET_KEY` | `minioadmin`             | Secret Key                        |

> O tipo de armazenamento também pode ser alterado em tempo de execução pela tela de **Configurações** da aplicação, sem necessidade de reiniciar o container.

---

## Documentação da API

A API REST é documentada via OpenAPI. Em ambiente de desenvolvimento, a Swagger UI está disponível em:

```
http://localhost:8080/api/q/swagger-ui
```

Principais endpoints:

| Método | Endpoint                                   | Descrição                                      |
|--------|--------------------------------------------|------------------------------------------------|
| POST   | `/api/projetos/sincronizar`                | Sincroniza todos os projetos GitLab → Parquet  |
| GET    | `/api/projetos/grupos`                     | Retorna grupos com contagem de projetos         |
| GET    | `/api/projetos/grupo/{grupo}`              | Lista projetos de um grupo específico           |
| POST   | `/api/dependencias/sincronizar`            | Cataloga dependências de um arquivo de manifesto|
| GET    | `/api/dependencias/consultar`              | Consulta dependências com filtros               |
| POST   | `/api/busca/iniciar`                       | Inicia busca de conteúdo nos repositórios       |
| GET    | `/api/busca/resultado/{id}`                | Obtém resultado de uma busca concluída          |
| GET    | `/api/settings/storage`                    | Lê configuração de armazenamento atual          |
| POST   | `/api/settings/storage`                    | Atualiza configuração de armazenamento          |
