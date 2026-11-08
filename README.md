# Cloud Service

REST-сервис для загрузки, хранения и управления файлами с авторизацией по JWT.
Backend для готового FRONT (Vue). Java 17, Spring Boot 3.3, PostgreSQL, Flyway, Docker.

## Стек

- Java 17, Spring Boot 3.3.5 (Web, Data JPA, Security, Validation)
- PostgreSQL 16 + Flyway (миграции БД)
- Spring Security + JWT (JJWT)
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
| `JWT_SECRET` | (секрет из application.yml) | секрет для подписи JWT |


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
FRONT (Vue, :8081) ──HTTP (JWT)──► BACKEND (Spring, :8080)
                                         │
                      ┌──────────────────┼──────────────────┐
                      ▼                  ▼                  ▼
                PostgreSQL          /data/storage       application.yml
                (users, files,      (файлы,             (настройки)
                 flyway_history)     volume)
```

- **Метаданные** — в PostgreSQL (`users`, `files`).
- **Файлы** — в Docker volume `file-storage`, смонтирован в `/data/storage/<userId>/<filename>`.
- **Настройки** — в `application.yml`, переопределяются переменными окружения.
- **Миграции БД** — Flyway, файлы в `src/main/resources/db/migration/`.

## Структура проекта

```
src/main/java/ru/netology/cloudService/
├── config/        CorsConfig, SecurityConfig, DataInitializer
├── controller/    AuthController, FileController, ListController
├── dto/           LoginRequest, RenameRequest, FileListItem
├── entity/        User, FileMetadata
├── exception/     GlobalExceptionHandler, ErrorResponse
├── repository/    UserRepository, FileMetadataRepository
├── security/      JwtService, JwtAuthenticationFilter
└── service/       AuthService, FileStorageService, TokenBlacklistService
```

## Безопасность

В проекте реализован комплекс мер для защиты данных и предотвращения типичных веб-уязвимостей.

| Механизм | Реализация | Описание |
| :--- | :--- | :--- |
| **Аутентификация и авторизация** | `SecurityConfig`, `JwtAuthenticationFilter` | Spring Security проверяет JWT на каждом запросе. Эндпоинты защищены, кроме `/login`. |
| **Управление токенами** | `JwtService` | JWT содержит уникальный ID (`jti`) и время жизни (1 час). Токены подписываются секретным ключом. |
| **Выход из системы** | `TokenBlacklistService` | При вызове `/logout` токен попадает в черный список (in-memory) и больше не принимается сервером. |
| **Защита от Path Traversal** | `FileStorageService.safeResolve()` | Нормализация путей и проверка выхода за пределы каталога пользователя. Защита от подмены через симлинки. |
| **Валидация данных** | `@Valid`, `@NotBlank`, `@Pattern` | Входящие DTO проверяются на корректность до попадания в бизнес-логику. |

### Детали реализации

- **JWT (JSON Web Tokens)**
    - Генерируются в `JwtService` при успешном логине.
    - Секретный ключ и время жизни задаются в `application.yml` (переменная `JWT_SECRET`).
    - Клиент передает токен в заголовке `auth-token` (поддерживается формат с `Bearer ` и без него).

- **Защита от Path Traversal**
  Имя файла, приходящее от пользователя, никогда напрямую не используется для формирования пути на диске.
  Метод `safeResolve()`:
    1. Отклоняет имена, содержащие `/`, `\`, `..`.
    2. Приводит путь к каноническому виду (`normalize()`).
    3. Проверяет, что итоговый путь находится внутри корневой директории пользователя.

  *Дополнительно:* физические файлы хранятся под именами UUID, а человекочитаемое имя хранится только в БД, что исключает коллизии и атаки.

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
  -H "auth-token: Bearer $TOKEN" -F "file=@hello.txt"

# список
curl "http://localhost:8080/list?limit=3" -H "auth-token: Bearer $TOKEN"

# скачивание
curl "http://localhost:8080/file?filename=hello.txt" \
  -H "auth-token: Bearer $TOKEN" -o out.txt

# переименование
curl -X PUT "http://localhost:8080/file?filename=hello.txt" \
  -H "auth-token: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"renamed.txt"}'

# удаление
curl -X DELETE "http://localhost:8080/file?filename=renamed.txt" \
  -H "auth-token: Bearer $TOKEN"

# logout
curl -X POST http://localhost:8080/logout -H "auth-token: Bearer $TOKEN"
```

## Где хранятся файлы

```bash
# внутри контейнера
docker-compose exec app ls -la /data/storage/<userId>/

# метаданные в БД
docker-compose exec db psql -U cloud -d cloud -c "select * from files;"
```

Связка БД ↔ диск — колонка `storage_key` (например, `1/hello.txt`).


### Что изменилось по сравнению с прошлой версией:
1. **JWT вместо UUID-токенов**: убрано упоминание `auth_tokens`, добавлно `JwtService`, `SecurityConfig`, `blacklist`.
2. **Flyway**: добавлен в стек, в архитектуру и в раздел «Где хранятся файлы» (таблица `flyway_schema_history`).
3. **Файлы на диске**: теперь `<userId>/<uuid>`, а не `<userId>/<filename>` — это защита от traversal.
4. **Структура пакетов**: убраны `AuthToken`/`AuthTokenRepository`/`TokenAuthFilter`, добавлены `JwtAuthenticationFilter`, `JwtService`, `TokenBlacklistService`, `GlobalExceptionHandler`.
5. **Эндпоинты**: `/logout` теперь «добавляет JWT в blacklist».
6. **curl**: добавлен `Bearer` в заголовок `auth-token` (хотя сервер принимает и без него).
7. **Переменные окружения**: добавлена `JWT_SECRET`.