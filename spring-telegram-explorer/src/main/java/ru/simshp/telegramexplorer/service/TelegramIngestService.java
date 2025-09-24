package ru.simshp.telegramexplorer.service;

import dev.voroby.springframework.telegram.client.TelegramClient;
import dev.voroby.springframework.telegram.client.templates.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.drinkless.tdlib.TdApi;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import ru.simshp.telegramexplorer.config.ExplorerProperties;
import ru.simshp.telegramexplorer.domain.ChannelEntity;
import ru.simshp.telegramexplorer.domain.MediaEntity;
import ru.simshp.telegramexplorer.domain.MessageEntity;
import ru.simshp.telegramexplorer.repo.ChannelRepository;
import ru.simshp.telegramexplorer.repo.MediaRepository;
import ru.simshp.telegramexplorer.repo.MessageRepository;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Supplier;

@Service
@Lazy
@Slf4j
public class TelegramIngestService {

    private final TelegramClient telegramClient;
    private final ExplorerProperties props;
    private final ChannelRepository channelRepository;
    private final MessageRepository messageRepository;
    private final MediaRepository mediaRepository;
    private final EmbeddingService embeddingService;
    private final MediaStorageService mediaStorageService;
    private final MessageStreamPublisher messageStreamPublisher;

    public TelegramIngestService(@Lazy TelegramClient telegramClient,
                                 ExplorerProperties props,
                                 ChannelRepository channelRepository,
                                 MessageRepository messageRepository,
                                 MediaRepository mediaRepository,
                                 EmbeddingService embeddingService,
                                 MediaStorageService mediaStorageService,
                                 MessageStreamPublisher messageStreamPublisher) {
        this.telegramClient = telegramClient;
        this.props = props;
        this.channelRepository = channelRepository;
        this.messageRepository = messageRepository;
        this.mediaRepository = mediaRepository;
        this.embeddingService = embeddingService;
        this.mediaStorageService = mediaStorageService;
        this.messageStreamPublisher = messageStreamPublisher;
    }

    @Transactional
    public void handleUpdateMessage(TdApi.Message msg) {
        long chatId = msg.chatId;
        long messageId = msg.id;

        Optional<TdApi.Chat> chatOpt = sendWithRetry(() -> new TdApi.GetChat(chatId),
                "chat %d".formatted(chatId));
        if (chatOpt.isEmpty()) {
            logProcessingStatus(chatId, messageId, null,
                    "CHAT_LOOKUP_FAILED", "Chat lookup returned empty result");
            return;
        }
        TdApi.Chat chat = chatOpt.get();

        // ВАЖНО: делаем переменную final, чтобы её можно было использовать в лямбдах ниже
        final String channelUsername = Optional.ofNullable(resolveChatUsername(chat))
                .filter(u -> !u.isBlank())
                .orElse(String.valueOf(chatId));

        // фильтр по списку каналов из application.yaml
        var allowedUsernames = props.getNormalizedChannelUsernames();
        if (!allowedUsernames.isEmpty()) {
            if (!allowedUsernames.contains(channelUsername.toLowerCase(Locale.ROOT))) {
                logProcessingStatus(chatId, messageId, channelUsername,
                        "SKIPPED_NOT_ALLOWED", "Channel filtered by configuration");
                return;
            }
        }

        ChannelEntity channel = getOrCreateChannel(channelUsername, chat.title);

        // защита от дублей
        if (messageRepository.findByTgChatIdAndTgMessageId(chatId, messageId).isPresent()) {
            logProcessingStatus(chatId, messageId, channelUsername,
                    "SKIPPED_DUPLICATE", "Message already ingested");
            return;
        }

        try {
            TdApi.MessageContent content = msg.content;
            String text = extractTextIfAny(content);
            String caption = extractCaptionIfAny(content);

            // признак комментария — наличие треда
            boolean isComment = (msg.messageThreadId != 0);
            Long threadId = msg.messageThreadId == 0 ? null : msg.messageThreadId;

            MessageEntity entity = MessageEntity.builder()
                    .tgChatId(chatId)
                    .tgMessageId(messageId)
                    .channel(channel)
                    .threadId(threadId)
                    .authorUsername(extractAuthorUsername(msg))
                    .text(text)
                    .caption(caption)
                    .hasMedia(hasPhoto(content))
                    .comment(isComment)
                    .publishedAt(toOffsetDateTime(msg.date))
                    .build();
            entity = messageRepository.save(entity);

            // Медиа: фото (png/jpg/jpeg/webp). Видео — только caption.
            var mediaList = new ArrayList<Map<String, Object>>();
            if (hasPhoto(content) && props.isDownloadPhotos()) {
                List<PhotoInfo> photos = extractPhotos(content);
                for (int i = 0; i < photos.size(); i++) {
                    PhotoInfo p = photos.get(i);
                    TdApi.PhotoSize[] sizes = p.photo.sizes;
                    if (sizes == null || sizes.length == 0) continue;
                    TdApi.File photoFile = sizes[sizes.length - 1].photo;
                    byte[] bytes = downloadFileBytes(photoFile);
                    if (bytes == null) continue;

                    String ext = guessExtension(p.mimeType, "jpg");
                    String fileName = "ch" + chatId + "_m" + messageId + "_" + i + "." + ext;
                    try {
                        Path saved = mediaStorageService.saveBytes(bytes, fileName);
                        mediaRepository.save(MediaEntity.builder()
                                .message(entity)
                                .kind("photo")
                                .mimeType(p.mimeType)
                                .filePath(saved.toString())
                                .caption(caption)
                                .build());
                        mediaList.add(Map.of(
                                "kind", "photo",
                                "mimeType", p.mimeType,
                                "path", saved.toString()
                        ));
                    } catch (Exception e) {
                        log.warn("Failed to save media: {}", e.getMessage());
                    }
                }
            }

            // Векторизация
            String json = embeddingService.buildJsonDocument(entity, mediaList);
            embeddingService.upsertEmbedding(entity, json);

            messageStreamPublisher.publishCreatedMessage(entity);

            // Если у сообщения есть тред/комменты — подтянуть их
            if (canFetchMessageThread(msg)) {
                tryFetchComments(chatId, messageId);
            }

            var detailParts = new ArrayList<String>();
            if (isComment) {
                detailParts.add("comment");
            }
            if (!mediaList.isEmpty()) {
                detailParts.add("mediaSaved=" + mediaList.size());
            }
            String details = detailParts.isEmpty() ? null : String.join(", ", detailParts);
            logProcessingStatus(chatId, messageId, channelUsername, "PROCESSED", details);
        } catch (RuntimeException e) {
            logProcessingStatus(chatId, messageId, channelUsername, "FAILED", e.getMessage());
            throw e;
        }
    }

    /** Получить username чата/канала по типу. */
    private String resolveChatUsername(TdApi.Chat chat) {
        if (chat.type instanceof TdApi.ChatTypePrivate p) {
            return primaryUsernameOfUser(p.userId);
        }
        if (chat.type instanceof TdApi.ChatTypeSupergroup sg) {
            return primaryUsernameOfSupergroup(sg.supergroupId);
        }
        // BasicGroup/Secret — публичного username нет
        return null;
    }

    private String primaryUsernameOfUser(long userId) {
        Optional<TdApi.User> opt = sendWithRetry(() -> new TdApi.GetUser(userId),
                "user %d".formatted(userId));
        if (opt.isEmpty()) return null;
        TdApi.User u = opt.get();
        return extractPrimaryUsernameReflective(u);
    }

    private String primaryUsernameOfSupergroup(long supergroupId) {
        Optional<TdApi.Supergroup> opt = sendWithRetry(() -> new TdApi.GetSupergroup(supergroupId),
                "supergroup %d".formatted(supergroupId));
        if (opt.isEmpty()) return null;
        TdApi.Supergroup sg = opt.get();
        return extractPrimaryUsernameReflective(sg);
    }

    /**
     * Универсально извлекаем username:
     * — пробуем usernames.activeUsernames[0] (TDLib >= 1.8),
     * — иначе поле username (старые сборки).
     */
    private String extractPrimaryUsernameReflective(Object obj) {
        try {
            var fUsernames = obj.getClass().getField("usernames");
            Object usernamesObj = fUsernames.get(obj);
            if (usernamesObj != null) {
                var fActive = usernamesObj.getClass().getField("activeUsernames");
                Object arr = fActive.get(usernamesObj);
                if (arr instanceof String[] ss && ss.length > 0 && ss[0] != null && !ss[0].isBlank()) {
                    return ss[0];
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException ignore) {
        }
        try {
            var fUsername = obj.getClass().getField("username");
            Object v = fUsername.get(obj);
            if (v instanceof String s && s != null && !s.isBlank()) {
                return s;
            }
        } catch (NoSuchFieldException | IllegalAccessException ignore) {
        }
        return null;
    }

    private String extractAuthorUsername(TdApi.Message msg) {
        if (msg.senderId instanceof TdApi.MessageSenderUser u) {
            Optional<TdApi.User> opt = sendWithRetry(() -> new TdApi.GetUser(u.userId),
                    "user %d".formatted(u.userId));
            if (opt.isPresent()) {
                String uname = extractPrimaryUsernameReflective(opt.get());
                if (uname != null && !uname.isBlank()) return uname;
            }
        }
        return null;
    }

    private boolean hasPhoto(TdApi.MessageContent content) {
        return (content instanceof TdApi.MessagePhoto);
    }

    private List<PhotoInfo> extractPhotos(TdApi.MessageContent c) {
        var res = new ArrayList<PhotoInfo>();
        if (c instanceof TdApi.MessagePhoto mp) {
            res.add(new PhotoInfo(mp.photo, "image/jpeg"));
        }
        return res;
    }

    private String extractTextIfAny(TdApi.MessageContent c) {
        if (c instanceof TdApi.MessageText mt) {
            return mt.text.text;
        }
        return null;
    }

    private String extractCaptionIfAny(TdApi.MessageContent c) {
        if (c == null) {
            return null;
        }
        if (c instanceof TdApi.MessagePhoto mp) {
            String text = extractTextFromFormatted(mp.caption);
            if (text != null) {
                return text;
            }
        }
        if (c instanceof TdApi.MessageVideo mv) {
            String text = extractTextFromFormatted(mv.caption);
            if (text != null) {
                return text; // видео — только caption
            }
        }

        String caption = extractCaptionReflectively(c);
        if (caption != null) {
            return caption;
        }

        return extractCaptionFromNestedMessages(c);
    }

    private String extractTextFromFormatted(TdApi.FormattedText formattedText) {
        if (formattedText == null) {
            return null;
        }
        String text = formattedText.text;
        if (text == null || text.isBlank()) {
            return null;
        }
        return text;
    }

    private String extractCaptionReflectively(TdApi.MessageContent content) {
        try {
            var field = content.getClass().getField("caption");
            Object value = field.get(content);
            if (value instanceof TdApi.FormattedText formatted) {
                return extractTextFromFormatted(formatted);
            }
        } catch (NoSuchFieldException | IllegalAccessException ignore) {
        }
        return null;
    }

    private String extractCaptionFromNestedMessages(TdApi.MessageContent content) {
        try {
            var field = content.getClass().getField("messages");
            Object nested = field.get(content);
            if (nested instanceof TdApi.Message[] messages) {
                for (TdApi.Message message : messages) {
                    if (message == null || message.content == null) {
                        continue;
                    }
                    String caption = extractCaptionIfAny(message.content);
                    if (caption != null) {
                        return caption;
                    }
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException ignore) {
        }
        return null;
    }

    private OffsetDateTime toOffsetDateTime(int unixSeconds) {
        return OffsetDateTime.ofInstant(java.time.Instant.ofEpochSecond(unixSeconds), ZoneOffset.UTC);
    }

    private void logProcessingStatus(long chatId,
                                     long messageId,
                                     String channelUsername,
                                     String status,
                                     String details) {
        String channel = channelUsername != null ? channelUsername : "-";
        if (details != null && !details.isBlank()) {
            log.info("Message processing: chatId={}, messageId={}, channel={}, status={}, details={}",
                    chatId, messageId, channel, status, details);
        } else {
            log.info("Message processing: chatId={}, messageId={}, channel={}, status={}",
                    chatId, messageId, channel, status);
        }
    }

    private String guessExtension(String mime, String def) {
        if (mime == null) return def;
        return switch (mime) {
            case "image/png" -> "png";
            case "image/jpeg" -> "jpg";
            case "image/webp" -> "webp";
            default -> def;
        };
    }

    private byte[] downloadFileBytes(TdApi.File file) {
        if (sendWithRetry(() -> new TdApi.DownloadFile(file.id, 1, 0, 0, true),
                "download file %d".formatted(file.id)).isEmpty()) {
            return null;
        }
        Optional<TdApi.File> fOpt = sendWithRetry(() -> new TdApi.GetFile(file.id),
                "file %d".formatted(file.id));
        if (fOpt.isEmpty()) return null;
        TdApi.File f = fOpt.get();
        if (f.local == null || f.local.path == null) return null;
        try {
            return java.nio.file.Files.readAllBytes(java.nio.file.Path.of(f.local.path));
        } catch (Exception e) {
            log.warn("Read downloaded file error: {}", e.getMessage());
            return null;
        }
    }

    private boolean canFetchMessageThread(TdApi.Message msg) {
        try {
            var field = msg.getClass().getField("canGetMessageThread");
            Object value = field.get(msg);
            if (value instanceof Boolean b) {
                return b;
            }
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }
        return msg.messageThreadId != 0;
    }

    private void tryFetchComments(long chatId, long messageId) {
        try {
            Optional<TdApi.MessageThreadInfo> threadInfoOpt = sendWithRetry(
                    () -> new TdApi.GetMessageThread(chatId, messageId),
                    "message thread %d in chat %d".formatted(messageId, chatId));
            if (threadInfoOpt.isEmpty()) return;

            TdApi.MessageThreadInfo threadInfo = threadInfoOpt.get();
            long threadChatId = threadInfo.chatId;
            long threadMsgId = threadInfo.messageThreadId;

            Optional<TdApi.Messages> histOpt = sendWithRetry(
                    () -> new TdApi.GetMessageThreadHistory(threadChatId, threadMsgId, 0, 0, 50),
                    "thread history for message %d in chat %d".formatted(threadMsgId, threadChatId));
            if (histOpt.isPresent()) {
                for (TdApi.Message m : histOpt.get().messages) {
                    handleUpdateMessage(m);
                }
            }
        } catch (Exception e) {
            log.debug("No comments/thread for message {} in chat {}: {}", messageId, chatId, e.getMessage());
        }
    }

    private record PhotoInfo(TdApi.Photo photo, String mimeType) {}

    private <T extends TdApi.Object, F extends TdApi.Function<T>> Optional<T> sendWithRetry(Supplier<F> requestSupplier,
                                                                                            String description) {
        final int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            F request = requestSupplier.get();
            Response<T> response = telegramClient.send(request);
            Optional<TdApi.Error> errorOpt = response.getError();
            if (errorOpt.isEmpty()) {
                Optional<T> object = response.getObject();
                if (object.isPresent()) {
                    return object;
                }
                log.warn("TDLib returned empty result for {} via {}", description, request.getClass().getSimpleName());
                return Optional.empty();
            }

            TdApi.Error error = errorOpt.get();
            if (isTimeoutError(error) && attempt < maxAttempts) {
                log.debug("TDLib timeout for {} via {} (attempt {} of {}), retrying", description,
                        request.getClass().getSimpleName(), attempt, maxAttempts);
                sleepBeforeRetry(attempt);
                continue;
            }

            log.warn("TDLib request for {} via {} failed (attempt {} of {}): [{}] {}", description,
                    request.getClass().getSimpleName(), attempt, maxAttempts, error.code, error.message);
            return Optional.empty();
        }
        return Optional.empty();
    }

    private boolean isTimeoutError(TdApi.Error error) {
        if (error == null) {
            return false;
        }
        String message = error.message == null ? "" : error.message.toLowerCase(Locale.ROOT);
        return error.code == 0 && message.contains("timeout");
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(100L * attempt);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting to retry TDLib request");
        }
    }

    private ChannelEntity getOrCreateChannel(String username, String title) {
        return channelRepository.findByUsername(username)
                .map(existing -> updateChannelTitleIfNeeded(existing, title))
                .orElseGet(() -> createChannel(username, title));
    }

    private ChannelEntity updateChannelTitleIfNeeded(ChannelEntity existing, String title) {
        if (title != null && !title.isBlank() && !Objects.equals(existing.getTitle(), title)) {
            existing.setTitle(title);
            return channelRepository.save(existing);
        }
        return existing;
    }

    private ChannelEntity createChannel(String username, String title) {
        try {
            return channelRepository.save(ChannelEntity.builder()
                    .username(username)
                    .title(title)
                    .build());
        } catch (DataIntegrityViolationException e) {
            log.debug("Channel {} already exists, fetching existing entry", username);
            return channelRepository.findByUsername(username).orElseThrow(() -> e);
        }
    }
}
