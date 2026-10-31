# Cloud Service

REST-сервис для загрузки, хранения и управления файлами с авторизацией по `auth-token`.
Backend для готового FRONT (Vue). Java 17, Spring Boot 3.3, PostgreSQL, Maven, Docker.

## Стек

- Java 17, Spring Boot 3.3.5 (Web, Data JPA, Validation)
- PostgreSQL 16
- Maven (Wrapper включён — `./mvnw`)
- Docker / docker-compose
- Тесты: JUnit 5, Mockito, Testcontainers

## Запуск

```bash
docker-compose up --build
```

- Сервис: http://localhost:8080
- Тестовый пользователь: **user / user** (создаётся при старте)

Остановка: `docker-compose down`
Полная очистка (файлы + БД): `docker-compose down -v`

## Переменные окружения

| Переменная | По умолчанию | Назначение |
|---|---|---|
| `DB_HOST` | `localhost` | хост PostgreSQL |
| `DB_PORT` | `5432` | порт PostgreSQL |
| `DB_NAME` | `cloud` | имя БД |
| `DB_USER` | `cloud` | пользователь БД |
| `DB_PASSWORD` | `cloud` | пароль БД |
| `STORAGE_PATH` | `./data/storage` | директория файлов |
| `CORS_ORIGIN` | `http://localhost:8081` | origin фронта |

## Эндпоинты

| Метод | Путь | Описание | Заголовки / параметры |
|---|---|---|---|
| POST | `/login` | авторизация | body `{"login","password"}` → `{"auth-token"}` |
| POST | `/logout` | выход | header `auth-token` |
| POST | `/file?filename=X` | загрузка | header `auth-token`, multipart поле `file` |
| GET | `/file?filename=X` | скачивание | header `auth-token` |
| PUT | `/file?filename=X` | переименование | header `auth-token`, body `{"name":"new"}` |
| DELETE | `/file?filename=X` | удаление | header `auth-token` |
| GET | `/list?limit=N` | список файлов | header `auth-token` → `[{"filename","size"}]` |

Все эндпоинты, кроме `/login`, требуют заголовок `auth-token`.
Принимается как чистый токен, так и `Bearer <token>`.

## Архитектура

```
FRONT (Vue, :8081) ──HTTP──► BACKEND (Spring, :8080)
                                   │
                    ┌──────────────┼──────────────┐
                    ▼              ▼              ▼
              PostgreSQL      /data/storage   application.yml
              (users,         (файлы,         (настройки)
               auth_tokens,    volume)
               files)
```

- **Метаданные** — в PostgreSQL (`users`, `auth_tokens`, `files`).
- **Файлы** — в Docker volume `file-storage`, смонтирован в `/data/storage/<userId>/<filename>`.
- **Настройки** — в `application.yml`, переопределяются переменными окружения.

## Структура проекта

```
src/main/java/ru/netology/cloudService/
├── config/        CorsConfig, DataInitializer
├── controller/    AuthController, FileController, ListController
├── dto/           LoginRequest, RenameRequest, FileListItem, ErrorResponse
├── entity/        User, AuthToken, FileMetadata
├── repository/    UserRepository, AuthTokenRepository, FileMetadataRepository
├── security/      TokenAuthFilter
└── service/       AuthService, FileStorageService
```

## Тесты

```bash
./mvnw test
```

- Unit-тесты (Mockito) — сервисы и контроллеры.
- Интеграционные тесты (Testcontainers) — `CloudServiceIntegrationTest`, требует Docker.

## Подключение FRONT

В `.env` фронта:

```
VUE_APP_BASE_URL=http://localhost:8080
```

```bash
npm install
npm run serve   # http://localhost:8081
```

## curl-примеры

```bash
# логин
TOKEN=$(curl -s -X POST http://localhost:8080/login \
  -H "Content-Type: application/json" \
  -d '{"login":"user","password":"user"}' \
  | sed -E 's/.*"auth-token":"([^"]+)".*/\1/')

# загрузка
curl -X POST "http://localhost:8080/file?filename=hello.txt" \
  -H "auth-token: $TOKEN" -F "file=@hello.txt"

# список
curl "http://localhost:8080/list?limit=3" -H "auth-token: $TOKEN"

# скачивание
curl "http://localhost:8080/file?filename=hello.txt" \
  -H "auth-token: $TOKEN" -o out.txt

# переименование
curl -X PUT "http://localhost:8080/file?filename=hello.txt" \
  -H "auth-token: $TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"renamed.txt"}'

# удаление
curl -X DELETE "http://localhost:8080/file?filename=renamed.txt" \
  -H "auth-token: $TOKEN"

# logout
curl -X POST http://localhost:8080/logout -H "auth-token: $TOKEN"
```

## Где хранятся файлы

```bash
# внутри контейнера
docker-compose exec app ls -la /data/storage/<userId>/

# метаданные в БД
docker-compose exec db psql -U cloud -d cloud -c "select * from files;"
```

Связка БД ↔ диск — колонка `storage_key` (например, `1/hello.txt`).