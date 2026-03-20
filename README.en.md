[🇧🇷 Ler em Português](README.md)

# anime-api

REST API for an anime streaming platform built with Spring Boot 3. Handles authentication, anime and episode management, video streaming with range request support, and user activity tracking.

## Tech stack

- **Java 17** + **Spring Boot 3.2**
- **Spring Security** with stateless JWT authentication
- **Spring Data JPA** + **MySQL** (Flyway migrations)
- **Caffeine** for in-memory caching
- **Bucket4j** for rate limiting on auth endpoints
- **Hibernate @Formula** for computed fields without N+1 queries
- **Docker** with multi-stage build

## Requirements

- Java 17+
- Maven 3.9+
- MySQL 8+
- Docker (optional)

## Environment variables

| Variable | Description | Required |
|---|---|---|
| `DB_URL` | JDBC connection string (e.g. `jdbc:mysql://host:3306/animedb`) | Yes |
| `DB_USERNAME` | Database user | Yes |
| `DB_PASSWORD` | Database password | Yes |
| `JWT_SECRET` | Secret key for signing JWTs — use a strong random string (32+ chars) | Yes |
| `STORAGE_TYPE` | `local` (default) or `s3` | No |
| `STORAGE_LOCAL_PATH` | Absolute path for file storage (default: `/app/uploads`) | No |

## Running locally

```bash
# Copy and fill in environment variables
cp .env.example .env

# Run with Maven
./mvnw spring-boot:run

# Or build and run the JAR directly
./mvnw clean package -DskipTests
java -jar target/anime-api-1.0.0.jar
```

The API will be available at `http://localhost:8080`.

## Running with Docker

```bash
docker build -t anime-api .

docker run -p 8080:8080 \
  -e DB_URL=jdbc:mysql://host:3306/animedb \
  -e DB_USERNAME=user \
  -e DB_PASSWORD=secret \
  -e JWT_SECRET=your-secret-key \
  anime-api
```

## Database migrations

Migrations are managed by Flyway and run automatically on startup. The initial schema is in `src/main/resources/db/migration/V1__init_schema.sql`.

The migration seeds two default users:

| Username | Password | Role |
|---|---|---|
| `admin` | `admin123` | `ADMIN` |
| `user` | `user123` | `USER` |

Change these credentials before deploying to any environment.

## API overview

All endpoints are prefixed with `/api`.

### Authentication

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | Public | Register a new user |
| `POST` | `/api/auth/login` | Public | Login and receive a JWT |

### Animes

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/api/animes` | Public | List all animes (paginated) |
| `GET` | `/api/animes/search` | Public | Search by title, genre, status, year |
| `GET` | `/api/animes/genres` | Public | List all available genres |
| `GET` | `/api/animes/:id` | Public | Get anime by ID |
| `POST` | `/api/animes` | Admin | Create anime |
| `PUT` | `/api/animes/:id` | Admin | Update anime |
| `DELETE` | `/api/animes/:id` | Admin | Delete anime |
| `POST` | `/api/animes/:id/cover` | Admin | Upload cover image |
| `POST` | `/api/animes/:id/banner` | Admin | Upload banner image |

### Episodes

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/api/animes/:animeId/episodes` | Public | List published episodes |
| `GET` | `/api/animes/:animeId/episodes/:id` | Public | Get episode by ID |
| `POST` | `/api/animes/:animeId/episodes` | Admin | Create episode |
| `PUT` | `/api/animes/:animeId/episodes/:id` | Admin | Update episode |
| `DELETE` | `/api/animes/:animeId/episodes/:id` | Admin | Delete episode |
| `POST` | `/api/animes/:animeId/episodes/:id/video` | Admin | Upload video file |
| `POST` | `/api/animes/:animeId/episodes/:id/thumbnail` | Admin | Upload thumbnail |

### Video streaming

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/api/videos/stream/:episodeId` | Authenticated | Stream video with range request support |

Streaming supports the `Range` header for seek operations. Returns `206 Partial Content` for range requests and `200 OK` for full downloads.

### User features

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/api/favorites` | Authenticated | List favorites (paginated) |
| `POST` | `/api/favorites/:animeId` | Authenticated | Add to favorites |
| `DELETE` | `/api/favorites/:animeId` | Authenticated | Remove from favorites |
| `GET` | `/api/favorites/:animeId/check` | Authenticated | Check if favorited |
| `POST` | `/api/history` | Authenticated | Save watch progress |
| `GET` | `/api/history` | Authenticated | Get watch history (paginated) |

### Health check

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/actuator/health` | Application health status |

## Authentication

Authenticated requests require a `Bearer` token in the `Authorization` header:

```
Authorization: Bearer <token>
```

Tokens expire after 24 hours. There is currently no refresh token mechanism — users must re-authenticate after expiry.

## Rate limiting

Login and registration endpoints (`/api/auth/**`) are limited to 10 requests per minute per IP address. Exceeding this returns HTTP `429`.

In a multi-instance deployment, replace the in-memory `ConcurrentHashMap` in `RateLimitFilter` with a distributed store such as Redis + Bucket4j's Redis integration.

## File storage

The API supports local file storage by default. Files are stored under `STORAGE_LOCAL_PATH` with the following structure:

```
uploads/
├── images/   # Covers, banners, thumbnails
└── videos/   # Episode video files
```

Accepted video formats: `mp4`, `mkv`, `avi`, `webm`.

All stored paths are validated against the base directory to prevent path traversal attacks.

S3 support is stubbed in `StorageConfig` — set `STORAGE_TYPE=s3` and implement `S3StorageService` with the AWS SDK to enable it.

## Running tests

```bash
# All tests
./mvnw test

# Specific class
./mvnw test -Dtest=AnimeServiceTest
```

Tests use H2 in-memory database with Flyway disabled. The profile is configured in `src/main/resources/application-test.yml`.

## Project structure

```
src/main/java/com/animeapi/
├── config/          # Security, storage, and bean configuration
├── controller/      # REST controllers
├── dto/             # Request and response objects
│   ├── request/
│   └── response/
├── exception/       # Custom exceptions and global handler
├── model/           # JPA entities
├── repository/      # Spring Data repositories
├── security/        # JWT filter, JWT service, rate limiting
└── service/         # Business logic
```
