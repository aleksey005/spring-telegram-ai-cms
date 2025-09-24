# spring-telegram-explorer

## Общее описание
`spring-telegram-explorer` — это self-hosted сервис, который подключается к Telegram через TDLib, 
авторизуется как пользовательский бот (userbot) и индексирует сообщения выбранных каналов. 
Сообщения и метаданные складываются в PostgreSQL, медиа-файлы сохраняются на диск, а текстовые 
фрагменты проходят векторизацию через OpenAI Embeddings. На базе этих данных сервис предоставляет:

* REST API для просмотра и поиска сообщений;
* поток WebSocket-уведомлений о новых постах;
* статически собранный фронтенд (Next.js) для интерактивного поиска.

## Архитектура решения
```
Telegram → TDLib (spring-boot-starter-telegram) → TelegramIngestService
        → PostgreSQL (Liquibase схемы + pgvector)
        → Файловое хранилище медиа (MediaStorageService)
        → OpenAI Embeddings (EmbeddingService)
        → REST API /api/* (MessageApiController)
        → WebSocket /ws/messages (MessageUpdatesWebSocketHandler)
        → Фронтенд (/search.html из Next.js)
```

Основные процессы:

1. **Bootstrap TDLib** (`tdlib.BootstrapRunner`) — после успешной авторизации TDLib загружает
   историю каналов из конфигурации, чтобы прогреть кэш и инициировать получение новых апдейтов.
2. **Получение апдейтов** (`tdlib.UpdateNewMessageListener`) — подписка на `TdApi.UpdateNewMessage`.
   Каждый апдейт асинхронно передается в `TelegramIngestService` через пул `telegramUpdateExecutor`.
3. **Ингест сообщений** (`service.TelegramIngestService`) —
   * фильтрует каналы по `explorer.channels`;
   * создает/обновляет записи каналов (`ChannelRepository`);
   * сохраняет сообщения (`MessageRepository`), избегая дублей;
   * скачивает фото и кладет их через `MediaStorageService` + `MediaRepository`;
   * формирует JSON-документ и вызывает `EmbeddingService.upsertEmbedding` для векторизации;
   * публикует DTO в `MessageStreamPublisher`, который уведомляет WebSocket-подписчиков.
4. **Поисковый API** (`service.SearchService`) — принимает запросы, генерирует embedding для
   запроса и ищет ближайшие сообщения через pgvector (`<=>`). При ошибке
   выполняет fallback на полнотекстовый поиск `ILIKE`. Обогащает результаты ссылкой на
   `MessageImageService`, чтобы указать URL первой фотографии.
5. **Web API** (`web.MessageApiController`) — предоставляет пагинированную выдачу сообщений,
   поиск, список каналов и отдачу изображений. Использует DTO-конвертер `MessageViewMapper`.
6. **WebSocket** (`web.websocket.MessageUpdatesWebSocketHandler`) — держит сессии, сериализует
   события `message-created` и рассылает их всем подключенным клиентам.

## Структура проекта
* `src/main/java` — основная бизнес-логика Spring Boot приложения.
* `src/main/resources/db/changelog` — Liquibase-миграции (создание таблиц, индексов, pgvector).
* `docs/vector-json-schema.json` — JSON Schema документа, который сохраняется в таблице `embedding`.
* `frontend` — исходники Next.js, которые собираются в статический каталог `/app/public`.
* `tdlibs/libtdjni.so` — нативная библиотека TDLib, необходимая для работы клиента.
* `Dockerfile` — многоэтапная сборка backend + frontend + установка JRE и TDLib.

## Требования и подготовка окружения
* **Java 21** (в Docker используется Eclipse Temurin 21).
* **Maven 3.9+** (либо `./mvnw`).
* **Node.js 22** и `npm` для сборки фронтенда (если собираете без Docker).
* **PostgreSQL 15+** с установленным расширением `pgvector` (`CREATE EXTENSION vector;`).
* **Liquibase** запускается автоматически при старте приложения.
* **TDLib credentials** — переменные окружения: `TELEGRAM_API_ID`, `TELEGRAM_API_HASH`,
  `TELEGRAM_API_PHONE`, `TELEGRAM_API_DATABASE_ENCRYPTION`.
* **OpenAI API key** — переменная `OPENAI_API_KEY` для генерации embeddings.

## Конфигурация (application.yaml)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://<host>:<port>/<db>
    username: <user>
    password: <pass>
  jpa:
    hibernate:
      ddl-auto: none
    properties:
      hibernate.dialect: org.hibernate.dialect.PostgreSQLDialect
  telegram.client:
    api-id: ${TELEGRAM_API_ID}
    api-hash: ${TELEGRAM_API_HASH}
    phone: ${TELEGRAM_API_PHONE}
    database-encryption-key: ${TELEGRAM_API_DATABASE_ENCRYPTION}
    database-directory: /tdlib-db
explorer:
  channels: "mash, NyashnyeKartinki"
  media-dir: /data/media
  download-photos: true
  download-formats: [png, jpg, jpeg, webp]
  openai:
    api-key: ${OPENAI_API_KEY}
    embeddings-model: text-embedding-3-small
    dimensions: 1536
```

Ключевые параметры `ExplorerProperties`:

| Параметр | Описание |
|----------|----------|
| `channels` | Список username каналов для индексации; поддерживаются формы `@channel`, `https://t.me/channel`. |
| `media-dir` | Каталог для сохранения медиа (`MediaStorageService.ensureMediaDir`). |
| `download-photos` | Разрешить скачивание фото. |
| `download-formats` | Допустимые расширения для сохранения фото. |
| `openai.api-key` | Токен OpenAI для embeddings. |
| `openai.embeddings-model` | Модель для `EmbeddingRequest`. |
| `openai.dimensions` | Размерность вектора; должна совпадать с типом `vector(…)` в БД. |

## Сборка и запуск
### Локальный запуск (без Docker)
1. Установите PostgreSQL и создайте базу данных. Выполните `CREATE EXTENSION vector;`.
2. Задайте необходимые переменные окружения (`TELEGRAM_*`, `OPENAI_API_KEY`).
3. Соберите фронтенд: `cd frontend && npm install && npm run build` — статические файлы появятся в `frontend/out`.
4. Запустите backend:
   ```bash
   ./mvnw spring-boot:run
   ```
   По умолчанию приложение стартует на порту `8080`.

### Сборка и запуск через Docker
```bash
docker build -t spring-telegram-explorer .
docker run --rm \
  -p 8080:8080 \
  -e TELEGRAM_API_ID=... \
  -e TELEGRAM_API_HASH=... \
  -e TELEGRAM_API_PHONE=... \
  -e TELEGRAM_API_DATABASE_ENCRYPTION=... \
  -e OPENAI_API_KEY=... \
  -v $(pwd)/data/media:/data/media \
  -v $(pwd)/data/tdlib:/tdlib-db \
  spring-telegram-explorer
```

Dockerfile выполняет три этапа:
1. Сборка JAR через Maven (stage `build`).
2. Сборка Next.js фронтенда (stage `frontend`).
3. Формирование runtime-образа на базе Ubuntu 24.04 с установкой Temurin JRE 21
   и размещением `libtdjni.so` (stage по умолчанию).

## Работа с данными
### Схема базы данных
Миграции (`db/changelog/*.yml`) создают таблицы:

* `channel(id, username, title)`.
* `message(id, tg_chat_id, tg_message_id, channel_id, thread_id, author_username, text, caption, has_media, is_comment, published_at)`.
* `media(id, message_id, kind, mime_type, file_path, caption)`.
* `embedding(id, message_id, json_payload, vector)` — хранит JSON-документ и embedding (pgvector).

Создаются индексы `idx_message_channel_time`, `idx_message_chat_msg` и векторный индекс `idx_embedding_cosine`.

### Сохранение медиа
`MediaStorageService` обеспечивает создание каталога (`ensureMediaDir`) и сохранение байтов (`saveBytes`).
`TelegramIngestService` скачивает фото через TDLib (`TdApi.DownloadFile` → чтение локального файла), определяет
расширение (`guessExtension`) и сохраняет метаданные в таблицу `media`. URL для первой фотографии формируется
`MessageImageService.buildImageUrl` и используется в DTO/REST-ответах.

### Векторизация и JSON-документ
`EmbeddingService.buildJsonDocument` собирает JSON в соответствии с `docs/vector-json-schema.json` и возвращает строку
для записи в `embedding.json_payload`. Метод `upsertEmbedding`:

1. Выбирает текст сообщения (`text` либо `caption`).
2. Отправляет `EmbeddingRequest` в OpenAI (`createEmbeddings`).
3. Формирует литерал `[...]::vector` и выполняет `INSERT ... ON CONFLICT` через `JdbcTemplate`.

## REST API
Все конечные точки находятся под префиксом `/api` (`MessageApiController`).

| Метод | Путь | Параметры | Описание | Ответ |
|-------|------|-----------|----------|-------|
| GET | `/api/messages` | `page` — номер страницы; `size` — размер (1..200); `channel` — список фильтров по username | Пагинированный список сообщений. | `PageResponse<MessageView>`: `content` — массив DTO, поля `pageNumber`, `totalElements`, `totalPages`, `first`, `last`. |
| GET | `/api/messages/{id}/image` | `id` — идентификатор сообщения | Возвращает первую фотографию сообщения. | `200 OK` с `Content-Type` по `mimeType`, кешируемый 1 час. `404`, если фото нет. |
| GET | `/api/search` | `q` — поисковая строка | Выполняет поиск по embeddings (fallback на ILIKE). | `SearchResponse`: `query`, `results` (список `MessageView`). |
| GET | `/api/channels` | — | Список известных каналов. | Массив `ChannelView { username, title }`. |

Дополнительный контроллер: `POST /telegram/auth/code` (`TelegramAuthController.submitCode`) — обертка над
`TdApi.CheckAuthenticationCode` для завершения авторизации TDLib. При успехе возвращает `200 OK`, при ошибке —
`400` с JSON `{ error, details }`, при исключении — `500`.

### DTO форматы
`MessageView` содержит:
* `id` — внутренний идентификатор;
* `channel` — username канала;
* `comment` — признак комментария (`true`, если сообщение из треда);
* `threadId` — идентификатор треда (`null`, если нет);
* `author` — username автора (для комментариев);
* `text`, `caption`;
* `hasMedia` — признак наличия медиа;
* `imageUrl` — ссылка на первое фото или `null`;
* `publishedAt` — время публикации (`OffsetDateTime`).

`SearchResponse` — объект `{ "query": "...", "results": [MessageView, ...] }`.
`PageResponse` — стандартная структура Spring Data (см. `web/dto/PageResponse`).

## WebSocket `/ws/messages`
`MessageUpdatesWebSocketHandler` регистрируется через `WebSocketConfig`. Сообщения отправляются в формате:
```json
{
  "type": "message-created",
  "message": { ... MessageView ... }
}
```
Сессии автоматически очищаются при ошибках (`handleTransportError`). События генерирует `MessageStreamPublisher`,
который вызывается из `TelegramIngestService` после успешного сохранения сообщения и embedding.

## Фронтенд
Каталог `frontend` содержит приложение на Next.js. Команда `npm run build` создает статический экспорт (`out/`),
который копируется в образ и раздается Spring Boot как обычные статические файлы (`StaticPageConfig` проксирует
`/search` на `search.html`). Фронтенд использует REST API и WebSocket для динамического обновления списка сообщений.

## Логи и отладка
* Логи обработки сообщений формируются методом `TelegramIngestService.logProcessingStatus` с деталями
  (`PROCESSED`, `FAILED`, `SKIPPED_*`).
* Повторные попытки TDLib-запросов реализованы в `sendWithRetry` с экспоненциальной задержкой.
* Для кастомизации пула потоков используйте бин `TelegramExecutorConfig.telegramUpdateExecutor`.

## Расширение и модификация
* При добавлении новых типов медиа расширьте `TelegramIngestService.extractPhotos` и таблицу `media`.
* Для альтернативных моделей embeddings обновите `explorer.openai.embeddings-model` и размерность
  (измените тип столбца `embedding.vector`).
* API легко расширить дополнительными контроллерами или GraphQL-слоем; DTO мапятся через `MessageViewMapper`.

## Полезные ссылки
* [TDLib API](https://core.telegram.org/tdlib/docs/td__api_8h.html)
* [pgvector documentation](https://github.com/pgvector/pgvector)
* [OpenAI Embeddings API](https://platform.openai.com/docs/guides/embeddings)
