# GitLab Dashboard — Backend

## Stack

- **Framework:** Quarkus 3.x
- **Java:** 25 (GraalVM compatível)
- **Build:** Maven (`pom.xml`)
- **groupId:** `br.com.ib` | **artifactId:** `git-dashboards`

> Herda todas as regras definidas em `../CLAUDE.md`.

---

## Regras do Backend

### DTOs e Objetos de Valor

- Usar **`record`** sempre que o DTO/VO for imutável
- Usar classes convencionais apenas quando houver necessidade de mutabilidade ou herança

### Lombok

- Utilizar **Lombok** quando reduzir boilerplate em classes mutáveis (ex.: `@Builder`, `@Data`, `@Slf4j`)
- Não usar Lombok em `record` — records já são concisos por definição

### Tratamento de Erros

- Capturar exceptions de forma **global e centralizada** via `@Provider` / `ExceptionMapper` do Quarkus/JAX-RS
- Nunca fazer try/catch genérico espalhado pelos endpoints
- Respostas de erro devem seguir estrutura padronizada (ex.: `{ "error": "...", "message": "...", "status": 400 }`)

### Compatibilidade GraalVM / Native Image

- Todas as dependências devem ser **compatíveis com GraalVM native image**
- Preferir extensões Quarkus nativas (`quarkus-*`) a bibliotecas de terceiros sem suporte nativo
- Evitar reflection não declarada; registrar classes necessárias via `@RegisterForReflection` quando inevitável
- Testar o build nativo antes de adicionar dependências novas

### Contexto da API

- Todos os endpoints REST sob o prefixo **`/api`**
- Configurar `quarkus.http.root-path=/api` 

### Documentação de API

- **Todos** os endpoints documentados com **Swagger / OpenAPI** (extensão `quarkus-smallrye-openapi`)
- Usar anotações `@Operation`, `@APIResponse`, `@Parameter` de forma descritiva
- UI do Swagger disponível em desenvolvimento em `/api/q/swagger-ui`

### Internacionalização (i18n) — Mensagens de API

- Toda string retornada ao cliente (corpo de resposta, mensagens de erro) deve ser externalizada em `ResourceBundle` — nunca hardcoded no código Java
- Arquivos de mensagens em `src/main/resources/`:
  - `messages.properties` — pt-BR (idioma padrão)
  - `messages_en.properties` — en
- A resolução do locale é feita pelo bean `MessageService` (`br.com.ib.gitdashboards.i18n`), que lê o header `Accept-Language` da requisição
- Nos endpoints JAX-RS, receber o header via `@HeaderParam("Accept-Language") String acceptLanguage` e repassar ao `MessageService`
- No `GlobalExceptionMapper`, injetar `HttpHeaders` via `@Context` para acessar o header
- Chaves de mensagem seguem o padrão `escopo.acao` (ex.: `erro.gitlab.token.invalido`, `sincronizacao.iniciada.background`)

### Estrutura de Pacotes (convenção)

```
br.com.ib.gitdashboards
├── resource/      # endpoints JAX-RS
├── service/       # lógica de negócio
├── dto/           # records e DTOs
├── model/         # objetos de domínio
├── exception/     # mappers globais de exceção
├── config/        # beans de configuração
├── i18n/          # resolução de mensagens internacionalizadas
└── storage/       # abstração de persistência Parquet (local / S3/MinIO)
```

### Build Frontend no Maven

- Usar `frontend-maven-plugin` para instalar Node/npm e executar o build do frontend
- Copiar output do frontend (`frontend/dist/`) para `src/main/resources/META-INF/resources/`
- O profile de build padrão deve incluir o frontend; usar profile separado `skip-frontend` para desenvolvimento isolado do backend
