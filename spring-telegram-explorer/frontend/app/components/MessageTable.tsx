'use client';

import Image from 'next/image';
import { useCallback, useMemo, useState } from 'react';
import { MessageView, PageResponse } from '../types';

interface MessageTableProps {
  page?: PageResponse<MessageView>;
  loading: boolean;
  error?: string;
  highlightedMessageIds?: ReadonlySet<number>;
  onMessageInteraction?: (messageId: number) => void;
  pendingCommentIds?: ReadonlySet<number>;
  onGenerateComment?: (message: MessageView) => void;
}

export function MessageTable({
  page,
  loading,
  error,
  highlightedMessageIds,
  onMessageInteraction,
  pendingCommentIds,
  onGenerateComment,
}: MessageTableProps) {
  const rows = useMemo<MessageView[]>(() => page?.content ?? [], [page]);
  const apiBase = process.env.NEXT_PUBLIC_API_BASE_URL ?? '';
  const [selectedImage, setSelectedImage] = useState<string | null>(null);

  const unreadMessages = useMemo(
    () => rows.filter((msg) => highlightedMessageIds?.has(msg.id)),
    [rows, highlightedMessageIds]
  );
  const unreadCount = unreadMessages.length;

  const handleImageClick = useCallback((url: string) => {
    setSelectedImage(url);
  }, []);

  const handleModalClose = useCallback(() => {
    setSelectedImage(null);
  }, []);

  const selectedImageUrl = selectedImage ? `${apiBase}${selectedImage}` : null;

  const emptyState = useMemo(() => {
    if (loading) {
      return 'Загружаем сообщения…';
    }

    if (error) {
      return error;
    }

    return 'Сообщения ещё не загружены.';
  }, [loading, error]);

  if (!page || rows.length === 0) {
    return <div className="alert alert-secondary" role="status">{emptyState}</div>;
  }

  return (
    <>
      {unreadCount > 0 && (
        <div className="alert alert-success unread-summary" role="status" aria-live="polite">
          <div className="d-flex flex-column flex-md-row gap-3 align-items-md-center">
            <span
              className="unread-summary__badge"
              aria-label={`Новых сообщений: ${unreadCount}`}
            >
              {unreadCount}
            </span>
            <div className="flex-grow-1">
              <h3 className="h6 mb-2 text-success-emphasis">Новые сообщения</h3>
              <ul className="unread-summary__list mb-0">
                {unreadMessages.map((msg) => {
                  const source = msg.text ?? msg.caption ?? '';
                  const trimmed = source.trim();
                  const preview = trimmed.length > 0 ? trimmed : '(без текста)';
                  const excerpt = preview.length > 80 ? `${preview.slice(0, 80)}…` : preview;

                  return (
                    <li key={msg.id} className="unread-summary__item">
                      <span className="unread-summary__id">#{msg.id}</span>
                      <span className="unread-summary__channel">{msg.channel}</span>
                      <span className="unread-summary__excerpt">{excerpt}</span>
                    </li>
                  );
                })}
              </ul>
            </div>
          </div>
        </div>
      )}
      <div className="d-none d-md-block">
        <div className="table-responsive message-table-container">
          <table className="table table-striped align-middle">
            <thead className="table-light">
              <tr>
                <th scope="col">ID</th>
                <th scope="col">Канал</th>
                <th scope="col">Автор</th>
                <th scope="col">Текст / Caption</th>
                <th scope="col">Медиа</th>
                <th scope="col">Изображение</th>
                <th scope="col">Дата</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((msg) => {
                const imageUrl = msg.imageUrl;
                const isHighlighted = highlightedMessageIds?.has(msg.id) ?? false;
                const commentPending = pendingCommentIds?.has(msg.id) ?? false;
                const hasText = typeof msg.text === 'string' && msg.text.trim().length > 0;
                const hasCaption = typeof msg.caption === 'string' && msg.caption.trim().length > 0;
                const canRequestComment = Boolean(onGenerateComment && (hasText || hasCaption));

                const handleRowInteraction = () => {
                  if (isHighlighted && onMessageInteraction) {
                    onMessageInteraction(msg.id);
                  }
                };

                const handleGenerateComment = () => {
                  if (onGenerateComment) {
                    onGenerateComment(msg);
                  }
                };

                return (
                  <tr
                    key={msg.id}
                    className={isHighlighted ? 'table-success message-row-new' : undefined}
                    onPointerEnter={isHighlighted ? handleRowInteraction : undefined}
                    onPointerDown={isHighlighted ? handleRowInteraction : undefined}
                    onFocus={isHighlighted ? handleRowInteraction : undefined}
                  >
                    <th scope="row">{msg.id}</th>
                    <td>{msg.channel}</td>
                    <td>{msg.author ?? '—'}</td>
                    <td>
                      {msg.text && <div className="mb-1">{msg.text}</div>}
                      {!msg.text && msg.caption && <div className="text-body-secondary">{msg.caption}</div>}
                      {!msg.text && !msg.caption && <span className="text-body-secondary">(пусто)</span>}
                      <div className="mt-2">
                        {msg.aiComment ? (
                          <span className="text-body-secondary small">
                            <strong>Комментарий AI:</strong> {msg.aiComment}
                          </span>
                        ) : canRequestComment ? (
                          <button
                            type="button"
                            className="btn btn-sm btn-outline-primary"
                            onClick={handleGenerateComment}
                            disabled={commentPending}
                          >
                            {commentPending ? 'Запрашиваем…' : 'Получить комментарий'}
                          </button>
                        ) : (
                          <span className="text-body-secondary small">Комментарий AI: —</span>
                        )}
                        {commentPending && (
                          <span className="spinner-border spinner-border-sm align-middle ms-2" role="status" />
                        )}
                      </div>
                    </td>
                    <td>
                      <span className={msg.hasMedia ? 'badge text-bg-success' : 'badge text-bg-secondary'}>
                        {msg.hasMedia ? 'Есть' : 'Нет'}
                      </span>
                    </td>
                    <td>
                      {imageUrl ? (
                        <button
                          type="button"
                          className="btn p-0 border-0 bg-transparent"
                          onClick={() => handleImageClick(imageUrl)}
                          aria-label="Открыть изображение в полном размере"
                        >
                          <Image
                            src={`${apiBase}${imageUrl}`}
                            alt="Превью сообщения"
                            className="img-thumbnail object-fit-cover"
                            width={96}
                            height={96}
                            loading="lazy"
                            unoptimized
                          />
                        </button>
                      ) : (
                        <span className="text-body-secondary">—</span>
                      )}
                    </td>
                    <td>
                      {msg.publishedAt
                        ? new Date(msg.publishedAt).toLocaleString('ru-RU')
                        : <span className="text-body-secondary">—</span>}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
      <div className="d-md-none message-card-list">
        {rows.map((msg) => {
          const imageUrl = msg.imageUrl;
          const isHighlighted = highlightedMessageIds?.has(msg.id) ?? false;
          const commentPending = pendingCommentIds?.has(msg.id) ?? false;
          const hasText = typeof msg.text === 'string' && msg.text.trim().length > 0;
          const hasCaption = typeof msg.caption === 'string' && msg.caption.trim().length > 0;
          const canRequestComment = Boolean(onGenerateComment && (hasText || hasCaption));
          const publishedAt = msg.publishedAt ? new Date(msg.publishedAt) : undefined;
          const formattedDate = publishedAt ? publishedAt.toLocaleString('ru-RU') : undefined;

          const handleCardInteraction = () => {
            if (isHighlighted && onMessageInteraction) {
              onMessageInteraction(msg.id);
            }
          };

          const handleGenerateComment = () => {
            if (onGenerateComment) {
              onGenerateComment(msg);
            }
          };

          const cardClassName = ['message-card', isHighlighted ? 'message-card--new' : undefined]
            .filter(Boolean)
            .join(' ');

          return (
            <article
              key={msg.id}
              className={cardClassName}
              onPointerEnter={isHighlighted ? handleCardInteraction : undefined}
              onPointerDown={isHighlighted ? handleCardInteraction : undefined}
              onFocus={isHighlighted ? handleCardInteraction : undefined}
              tabIndex={isHighlighted ? 0 : undefined}
            >
              <div className="message-card__meta mb-2">
                <strong>#{msg.id}</strong>
                <span>{msg.channel}</span>
                <span>Автор: {msg.author ?? '—'}</span>
                {formattedDate && publishedAt && (
                  <time dateTime={publishedAt.toISOString()}>{formattedDate}</time>
                )}
              </div>
              <div className="message-card__content">
                {msg.text && <div className="mb-2">{msg.text}</div>}
                {!msg.text && msg.caption && (
                  <div className="mb-2 text-body-secondary">{msg.caption}</div>
                )}
                {!msg.text && !msg.caption && (
                  <span className="text-body-secondary">(пусто)</span>
                )}
                <div className="mt-3">
                  {msg.aiComment ? (
                    <span className="text-body-secondary small d-block">
                      <strong>Комментарий AI:</strong> {msg.aiComment}
                    </span>
                  ) : canRequestComment ? (
                    <button
                      type="button"
                      className="btn btn-sm btn-outline-primary"
                      onClick={handleGenerateComment}
                      disabled={commentPending}
                    >
                      {commentPending ? 'Запрашиваем…' : 'Получить комментарий'}
                    </button>
                  ) : (
                    <span className="text-body-secondary small d-block">Комментарий AI: —</span>
                  )}
                  {commentPending && (
                    <span className="spinner-border spinner-border-sm align-middle ms-2" role="status" />
                  )}
                </div>
              </div>
              {imageUrl ? (
                <button
                  type="button"
                  className="btn p-0 border-0 bg-transparent mt-3"
                  onClick={() => handleImageClick(imageUrl)}
                  aria-label="Открыть изображение в полном размере"
                >
                  <Image
                    src={`${apiBase}${imageUrl}`}
                    alt="Превью сообщения"
                    className="img-fluid rounded message-card__image"
                    width={640}
                    height={640}
                    loading="lazy"
                    sizes="(min-width: 768px) 320px, 100vw"
                    unoptimized
                  />
                </button>
              ) : null}
              <div className="message-card__footer mt-3">
                <span className={msg.hasMedia ? 'badge text-bg-success' : 'badge text-bg-secondary'}>
                  {msg.hasMedia ? 'Медиа: есть' : 'Медиа: нет'}
                </span>
                {!imageUrl && <span className="text-body-secondary">Изображение: —</span>}
              </div>
            </article>
          );
        })}
      </div>
      {selectedImageUrl && (
        <div
          className="position-fixed top-0 start-0 w-100 h-100 bg-dark bg-opacity-75 d-flex align-items-center justify-content-center"
          role="dialog"
          aria-modal="true"
          aria-label="Просмотр изображения"
          onClick={handleModalClose}
        >
          <div
            className="bg-white rounded shadow-lg p-3 d-flex flex-column gap-3"
            style={{ maxWidth: '90vw', maxHeight: '90vh' }}
            onClick={(event) => event.stopPropagation()}
          >
            <div className="flex-grow-1 d-flex align-items-center justify-content-center">
              <Image
                src={selectedImageUrl}
                alt="Увеличенное изображение сообщения"
                width={1024}
                height={1024}
                className="img-fluid rounded"
                style={{ maxHeight: '70vh', width: 'auto', height: 'auto' }}
                sizes="(min-width: 992px) 70vw, 100vw"
                unoptimized
              />
            </div>
            <div className="d-flex justify-content-center">
              <button type="button" className="btn btn-primary" onClick={handleModalClose}>
                Закрыть
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
