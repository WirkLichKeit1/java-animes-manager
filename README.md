[🇺🇸 Read in english](README.en.md)

# anime-api

API REST para uma plataforma de streaming de animes construída com Spring Boot 3. Gerencia autenticação, cadastro de animes e episódios, streaming de vídeo com suporte a range requests e rastreamento de atividade dos usuários.

## Stack

- **Java 17** + **Spring Boot 3.2**
- **Spring Security** com autenticação JWT stateless
- **Spring Data JPA** + **MySQL** (migrações via Flyway)
- **Caffeine** para cache em memória
- **Bucket4j** para rate limiting nos endpoints de autenticação
- **Hibernate @Formula** para campos computados sem N+1 queries
- **Docker** com build multi-stage

## Requisitos

- Java 17+
- Maven 3.9+
- MySQL 8+
- Docker (opcional)

## Variáveis de ambiente

| Variável | Descrição | Obrigatória |
|---|---|---|
| `DB_URL` | String de conexão JDBC (ex: `jdbc:mysql://host:3306/animedb`) | Sim |
| `DB_USERNAME` | Usuário do banco | Sim |
| `DB_PASSWORD` | Senha do banco | Sim |
| `JWT_SECRET` | Chave para assinatura dos JWTs — use uma string aleatória forte (32+ caracteres) | Sim |
| `STORAGE_TYPE` | `local` (padrão) ou `s3` | Não |
| `STORAGE_LOCAL_PATH` | Caminho absoluto para armazenamento de arquivos (padrão: `/app/uploads`) | Não |

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
  -e JWT_SECRET=sua-chave-secreta \
  anime-api
```

## Migrações de banco

As migrações são gerenciadas pelo Flyway e executadas automaticamente na inicialização. O schema inicial está em `src/main/resources/db/migration/V1__init_schema.sql`.

A migração cria dois usuários padrão:

| Usuário | Senha | Perfil |
|---|---|---|
| `admin` | `admin123` | `ADMIN` |
| `user` | `user123` | `USER` |

Altere essas credenciais antes de fazer deploy em qualquer ambiente.

## Visão geral da API

Todos os endpoints têm o prefixo `/api`.

### Autenticação

| Método | Endpoint | Autenticação | Descrição |
|---|---|---|---|
| `POST` | `/api/auth/register` | Pública | Cadastrar novo usuário |
| `POST` | `/api/auth/login` | Pública | Login e recebimento do JWT |

### Animes

| Método | Endpoint | Autenticação | Descrição |
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

| Método | Endpoint | Autenticação | Descrição |
|---|---|---|---|
| `GET` | `/api/animes/:animeId/episodes` | Pública | Listar episódios publicados |
| `GET` | `/api/animes/:animeId/episodes/:id` | Pública | Buscar episódio por ID |
| `POST` | `/api/animes/:animeId/episodes` | Admin | Criar episódio |
| `PUT` | `/api/animes/:animeId/episodes/:id` | Admin | Atualizar episódio |
| `DELETE` | `/api/animes/:animeId/episodes/:id` | Admin | Deletar episódio |
| `POST` | `/api/animes/:animeId/episodes/:id/video` | Admin | Upload do arquivo de vídeo |
| `POST` | `/api/animes/:animeId/episodes/:id/thumbnail` | Admin | Upload da thumbnail |

### Streaming de vídeo

| Método | Endpoint | Autenticação | Descrição |
|---|---|---|---|
| `GET` | `/api/videos/stream/:episodeId` | Autenticada | Transmitir vídeo com suporte a range requests |

O streaming suporta o header `Range` para operações de seek. Retorna `206 Partial Content` para range requests e `200 OK` para downloads completos.

### Funcionalidades do usuário

| Método | Endpoint | Autenticação | Descrição |
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

Os endpoints de login e cadastro (`/api/auth/**`) são limitados a 10 requisições por minuto por endereço IP. Exceder esse limite retorna HTTP `429`.

Em deployments com múltiplas instâncias, substitua o `ConcurrentHashMap` em memória do `RateLimitFilter` por um armazenamento distribuído como Redis com a integração do Bucket4j.

## Armazenamento de arquivos

Por padrão a API usa armazenamento local. Os arquivos ficam em `STORAGE_LOCAL_PATH` com a seguinte estrutura:

```
uploads/
├── images/   # Capas, banners e thumbnails
└── videos/   # Arquivos de vídeo dos episódios
```

Formatos de vídeo aceitos: `mp4`, `mkv`, `avi`, `webm`.

Todos os caminhos de armazenamento são validados contra o diretório base para prevenir ataques de path traversal.

O suporte a S3 está estruturado em `StorageConfig` — defina `STORAGE_TYPE=s3` e implemente `S3StorageService` com o AWS SDK para habilitá-lo.

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
├── config/          # Configuração de segurança, armazenamento e beans
├── controller/      # Controllers REST
├── dto/             # Objetos de requisição e resposta
│   ├── request/
│   └── response/
├── exception/       # Exceções customizadas e handler global
├── model/           # Entidades JPA
├── repository/      # Repositórios Spring Data
├── security/        # Filtro JWT, serviço JWT, rate limiting
└── service/         # Regras de negócio
```
