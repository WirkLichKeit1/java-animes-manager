[🇺🇸 Read in English](README.en.md)

# anime-api

API REST para uma plataforma de streaming de animes construída com Spring Boot 3. Gerencia autenticação, cadastro de animes e episódios, streaming de vídeo com suporte a range requests, rastreamento de atividade dos usuários e armazenamento de arquivos via Cloudinary ou sistema de arquivos local.

## Stack

- **Java 17** + **Spring Boot 3.2**
- **Spring Security** com autenticação JWT stateless
- **Spring Data JPA** + **MySQL** (migrações via Flyway)
- **Caffeine** para cache em memória
- **Bucket4j** para rate limiting nos endpoints de autenticação
- **Hibernate `@Formula`** para campos computados sem N+1 queries
- **Cloudinary** para armazenamento de imagens e vídeos em produção
- **Docker** com build multi-stage

## Requisitos

- Java 17+
- Maven 3.9+
- MySQL 8+
- Docker (opcional)
- Conta Cloudinary (para deploy em produção)

## Variáveis de ambiente

| Variável | Descrição | Obrigatória |
|---|---|---|
| `DB_URL` | String de conexão JDBC (ex: `jdbc:mysql://host:3306/animedb`) | Sim |
| `DB_USERNAME` | Usuário do banco | Sim |
| `DB_PASSWORD` | Senha do banco | Sim |
| `JWT_SECRET` | Chave para assinatura dos JWTs — use uma string aleatória forte (32+ caracteres) | Sim |
| `STORAGE_TYPE` | `local` (padrão) ou `cloudinary` | Não |
| `STORAGE_LOCAL_PATH` | Caminho absoluto para armazenamento local (padrão: `/app/uploads`) | Não |
| `CLOUDINARY_CLOUD_NAME` | Cloud name da conta Cloudinary | Apenas se `STORAGE_TYPE=cloudinary` |
| `CLOUDINARY_API_KEY` | API Key do Cloudinary | Apenas se `STORAGE_TYPE=cloudinary` |
| `CLOUDINARY_API_SECRET` | API Secret do Cloudinary | Apenas se `STORAGE_TYPE=cloudinary` |

> **Atenção:** nunca commite credenciais reais. Use sempre variáveis de ambiente ou um gerenciador de secrets.

## Executando localmente

```bash
# Copie e preencha as variáveis de ambiente
cp .env.example .env

# Execute com Maven
./mvnw spring-boot:run

# Ou compile e execute o JAR diretamente
./mvnw clean package -DskipTests
java -jar target/anime-api-1.0.0.jar
```

A API estará disponível em `http://localhost:8080`.

## Executando com Docker

```bash
docker build -t anime-api .

docker run -p 8080:8080 \
  -e DB_URL=jdbc:mysql://host:3306/animedb \
  -e DB_USERNAME=usuario \
  -e DB_PASSWORD=senha \
  -e JWT_SECRET=sua-chave-secreta-32-chars-minimo \
  -e STORAGE_TYPE=cloudinary \
  -e CLOUDINARY_CLOUD_NAME=seu-cloud \
  -e CLOUDINARY_API_KEY=sua-key \
  -e CLOUDINARY_API_SECRET=seu-secret \
  anime-api
```

## Migrações de banco

As migrações são gerenciadas pelo Flyway e executadas automaticamente na inicialização. O schema inicial está em `src/main/resources/db/migration/V1__init_schema.sql`.

A migração cria dois usuários padrão:

| Usuário | Senha | Perfil |
|---|---|---|
| `admin` | `admin123` | `ADMIN` |
| `user` | `user123` | `USER` |

**Altere essas credenciais antes de fazer deploy em qualquer ambiente.**

## Visão geral da API

Todos os endpoints têm o prefixo `/api`.

### Autenticação

| Método | Endpoint | Auth | Descrição |
|---|---|---|---|
| `POST` | `/api/auth/register` | Pública | Cadastrar novo usuário |
| `POST` | `/api/auth/login` | Pública | Login e recebimento do JWT |

### Animes

| Método | Endpoint | Auth | Descrição |
|---|---|---|---|
| `GET` | `/api/animes` | Pública | Listar todos os animes (paginado) |
| `GET` | `/api/animes/search` | Pública | Buscar por título, gênero, status, ano |
| `GET` | `/api/animes/genres` | Pública | Listar todos os gêneros disponíveis |
| `GET` | `/api/animes/:id` | Pública | Buscar anime por ID |
| `POST` | `/api/animes` | Admin | Criar anime |
| `PUT` | `/api/animes/:id` | Admin | Atualizar anime |
| `DELETE` | `/api/animes/:id` | Admin | Deletar anime |
| `POST` | `/api/animes/:id/cover` | Admin | Upload de imagem de capa |
| `POST` | `/api/animes/:id/banner` | Admin | Upload de banner |

### Episódios

| Método | Endpoint | Auth | Descrição |
|---|---|---|---|
| `GET` | `/api/animes/:animeId/episodes` | Pública | Listar episódios publicados |
| `GET` | `/api/animes/:animeId/episodes/:id` | Pública | Buscar episódio por ID |
| `POST` | `/api/animes/:animeId/episodes` | Admin | Criar episódio |
| `PUT` | `/api/animes/:animeId/episodes/:id` | Admin | Atualizar episódio |
| `DELETE` | `/api/animes/:animeId/episodes/:id` | Admin | Deletar episódio |
| `POST` | `/api/animes/:animeId/episodes/:id/video` | Admin | Upload do arquivo de vídeo |
| `POST` | `/api/animes/:animeId/episodes/:id/thumbnail` | Admin | Upload da thumbnail |

### Streaming de vídeo

| Método | Endpoint | Auth | Descrição |
|---|---|---|---|
| `GET` | `/api/videos/stream/:episodeId` | Autenticada | Transmitir vídeo com suporte a range requests |

O streaming suporta o header `Range` para operações de seek. Retorna `206 Partial Content` para range requests e `200 OK` para downloads completos. O vídeo é transmitido em streaming sem carregar o arquivo inteiro em memória.

### Funcionalidades do usuário

| Método | Endpoint | Auth | Descrição |
|---|---|---|---|
| `GET` | `/api/favorites` | Autenticada | Listar favoritos (paginado) |
| `POST` | `/api/favorites/:animeId` | Autenticada | Adicionar aos favoritos |
| `DELETE` | `/api/favorites/:animeId` | Autenticada | Remover dos favoritos |
| `GET` | `/api/favorites/:animeId/check` | Autenticada | Verificar se está favoritado |
| `POST` | `/api/history` | Autenticada | Salvar progresso de exibição |
| `GET` | `/api/history` | Autenticada | Consultar histórico (paginado) |

### Health check

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/actuator/health` | Status da aplicação |

## Autenticação

Requisições autenticadas exigem um token `Bearer` no header `Authorization`:

```
Authorization: Bearer <token>
```

Os tokens expiram após 24 horas. Não há mecanismo de refresh token — o usuário precisa autenticar novamente após a expiração.

## Rate limiting

Os endpoints de login e cadastro (`/api/auth/**`) são limitados a 10 requisições por minuto por endereço IP. Exceder esse limite retorna HTTP `429 Too Many Requests`.

Em deployments com múltiplas instâncias, substitua o `ConcurrentHashMap` em memória do `RateLimitFilter` por um armazenamento distribuído como Redis com a integração do Bucket4j.

## Armazenamento de arquivos

A API suporta dois backends de armazenamento, configurados via `STORAGE_TYPE`:

### Local (`STORAGE_TYPE=local`)

Arquivos ficam no servidor sob `STORAGE_LOCAL_PATH`:

```
uploads/
├── images/   # Capas, banners e thumbnails
└── videos/   # Arquivos de vídeo dos episódios
```

Adequado apenas para desenvolvimento. **Não use em produção** — os arquivos são perdidos a cada redeploy em plataformas como Render.

### Cloudinary (`STORAGE_TYPE=cloudinary`)

Armazenamento CDN externo. Arquivos ficam organizados em:

```
darkjam/
├── images/   # Capas, banners e thumbnails
└── videos/   # Arquivos de vídeo dos episódios
```

As URLs públicas do Cloudinary são retornadas diretamente nos responses da API (campos `coverImageUrl`, `bannerImageUrl`, `thumbnailUrl`). O frontend usa essas URLs diretamente — sem passar pelo backend para servir imagens.

Para configurar no Cloudinary:
1. Crie uma conta em [cloudinary.com](https://cloudinary.com) (plano gratuito inclui 25GB)
2. Acesse **Settings → Access Keys** e gere um par de chaves
3. Configure as três variáveis de ambiente (`CLOUD_NAME`, `API_KEY`, `API_SECRET`)

> **Importante:** `CLOUDINARY_API_SECRET` e `CLOUDINARY_API_KEY` são campos diferentes. Verifique os valores com atenção ao configurar — são exibidos lado a lado no painel do Cloudinary.

Formatos de vídeo aceitos: `mp4`, `mkv`, `avi`, `webm`.

Todos os caminhos de armazenamento local são validados contra o diretório base para prevenir ataques de path traversal.

## Decisões de arquitetura

**`@Formula` para contagem de episódios** — em vez de carregar a lista completa de episódios para contar (`getEpisodes().size()`), um campo `@Formula` executa uma subquery SQL diretamente. Isso elimina o N+1 query problem nas listagens de animes.

**`VideoStatusUpdater` separado do `VideoService`** — `@Async` e `@Transactional` no mesmo bean não funcionam corretamente porque chamadas internas (`this.método()`) não passam pelo proxy do Spring. A separação em beans distintos garante que a transação seja aberta corretamente após o processamento assíncrono do vídeo.

**Streaming sem buffer** — o `VideoService` transmite o vídeo via `InputStream.transferTo()` diretamente para o `OutputStream` da resposta, sem carregar o arquivo inteiro em memória. Suporta arquivos de qualquer tamanho.

## Executando os testes

```bash
# Todos os testes
./mvnw test

# Classe específica
./mvnw test -Dtest=AnimeServiceTest
```

Os testes utilizam banco H2 em memória com o Flyway desabilitado. O perfil está configurado em `src/main/resources/application-test.yml`.

## Estrutura do projeto

```
src/main/java/com/animeapi/
├── config/          # Configuração de segurança, storage e beans
├── controller/      # Controllers REST
├── dto/
│   ├── request/     # Objetos de entrada com validação
│   └── response/    # Objetos de saída
├── exception/       # Exceções customizadas e handler global
├── model/           # Entidades JPA
├── repository/      # Repositórios Spring Data
├── security/        # Filtro JWT, serviço JWT, rate limiting
└── service/         # Regras de negócio
    ├── StorageService.java         # Interface de storage
    ├── LocalStorageService.java    # Implementação local
    ├── CloudinaryStorageService.java # Implementação Cloudinary
    ├── VideoStatusUpdater.java     # Atualização transacional assíncrona
    └── ...
```

## Deploy (Render)

1. Conecte o repositório ao Render como **Web Service**
2. Configure o **Build Command**: `./mvnw clean package -DskipTests`
3. Configure o **Start Command**: `java -jar target/anime-api-1.0.0.jar`
4. Adicione as variáveis de ambiente em **Environment**:
   - `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` — conexão com o banco (ex: Railway MySQL)
   - `JWT_SECRET` — string aleatória longa
   - `STORAGE_TYPE=cloudinary`
   - `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`
5. Faça o deploy
