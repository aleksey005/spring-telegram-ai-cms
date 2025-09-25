'use client';

import { Suspense, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { fetchJson } from './lib/api';
import { resolveWebSocketUrl } from './lib/websocket';
import { MessageTable } from './components/MessageTable';
import { Pagination } from './components/Pagination';
import { ChannelSelector } from './components/ChannelSelector';
import { AiCommentResponse, ChannelView, MessageStreamEvent, MessageView, PageResponse } from './types';

const DEFAULT_PAGE = 0;

export default function HomePage() {
  return (
    <Suspense fallback={<div className="py-4">Загружаем сообщения…</div>}>
      <HomePageContent />
    </Suspense>
  );
}

function HomePageContent() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const pageParam = Number.parseInt(searchParams.get('page') ?? `${DEFAULT_PAGE}`, 10);
  const currentPage = Number.isNaN(pageParam) || pageParam < 0 ? DEFAULT_PAGE : pageParam;

  const [page, setPage] = useState<PageResponse<MessageView>>();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>();
  const [channels, setChannels] = useState<ChannelView[]>([]);
  const [channelsLoading, setChannelsLoading] = useState(false);
  const [channelsError, setChannelsError] = useState<string>();
  const [commentError, setCommentError] = useState<string>();
  const currentPageRef = useRef(currentPage);
  const [highlightedMessageIds, setHighlightedMessageIds] = useState<Set<number>>(
    () => new Set()
  );
  const [pendingCommentIds, setPendingCommentIds] = useState<Set<number>>(() => new Set());
  const unreadMessageIds = useMemo<number[]>(() => {
    if (!page || highlightedMessageIds.size === 0) {
      return [];
    }

    return page.content
      .filter((msg) => highlightedMessageIds.has(msg.id))
      .map((msg) => msg.id);
  }, [page, highlightedMessageIds]);
  const handleMessageInteraction = useCallback((messageId: number) => {
    setHighlightedMessageIds((prev) => {
      if (!prev.has(messageId)) {
        return prev;
      }
      const next = new Set(prev);
      next.delete(messageId);
      return next;
    });
  }, []);

  const selectedChannels = useMemo<string[]>(() => {
    const values = searchParams.getAll('channel');
    if (!values || values.length === 0) {
      return [];
    }

    const normalized = new Set<string>();
    values.forEach((value) => {
      const candidate = (value ?? '').trim();
      if (!candidate) {
        return;
      }
      const withoutAt = candidate.startsWith('@') ? candidate.substring(1) : candidate;
      if (withoutAt) {
        normalized.add(withoutAt);
      }
    });

    return Array.from(normalized);
  }, [searchParams]);

  const selectedChannelsRef = useRef<string[]>(
    selectedChannels.map((channel) => channel.toLowerCase())
  );

  useEffect(() => {
    let cancelled = false;
    setChannelsLoading(true);
    setChannelsError(undefined);

    fetchJson<ChannelView[]>('/api/channels')
      .then((response) => {
        if (!cancelled) {
          setChannels(response);
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setChannelsError(
            err instanceof Error ? err.message : 'Не удалось загрузить список каналов'
          );
          setChannels([]);
        }
      })
      .finally(() => {
        if (!cancelled) {
          setChannelsLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const extraSelectedChannels = useMemo(
    () =>
      selectedChannels.filter(
        (username) => !channels.some((channel) => channel.username === username)
      ),
    [channels, selectedChannels]
  );

  const loadPage = useCallback(
    async (pageIndex: number) => {
      setLoading(true);
      setError(undefined);
      try {
        const params = new URLSearchParams();
        params.set('page', `${pageIndex}`);
        selectedChannels.forEach((channel) => params.append('channel', channel));
        const query = params.toString();
        const url = query ? `/api/messages?${query}` : '/api/messages';
        const response = await fetchJson<PageResponse<MessageView>>(url);
        setPage(response);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Не удалось загрузить сообщения');
        setPage(undefined);
      } finally {
        setLoading(false);
      }
    },
    [selectedChannels]
  );

  useEffect(() => {
    currentPageRef.current = currentPage;
  }, [currentPage]);

  useEffect(() => {
    selectedChannelsRef.current = selectedChannels.map((channel) => channel.toLowerCase());
  }, [selectedChannels]);

  useEffect(() => {
    loadPage(currentPage).catch((err) => console.error(err));
  }, [currentPage, loadPage]);

  useEffect(() => {
    let socket: WebSocket | null = null;
    let reconnectTimeout: ReturnType<typeof setTimeout> | undefined;
    let stopped = false;

    const scheduleReconnect = () => {
      if (stopped) {
        return;
      }
      reconnectTimeout = setTimeout(connect, 2000);
    };

    const connect = () => {
      if (stopped) {
        return;
      }

      let url: string;
      try {
        url = resolveWebSocketUrl('/ws/messages');
      } catch (err) {
        console.error('Failed to resolve WebSocket URL', err);
        scheduleReconnect();
        return;
      }

      try {
        socket = new WebSocket(url);
      } catch (err) {
        console.error('Failed to open WebSocket connection', err);
        scheduleReconnect();
        return;
      }

      socket.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data) as MessageStreamEvent;
          if (!data || !data.message) {
            return;
          }

          if (data.type === 'message-created') {
            const filters = selectedChannelsRef.current;
            if (filters.length > 0) {
              const messageChannel = data.message?.channel?.toLowerCase();
              if (!messageChannel || !filters.includes(messageChannel)) {
                return;
              }
            }

            const messageId = data.message?.id;
            if (typeof messageId === 'number') {
              setHighlightedMessageIds((prev) => {
                if (prev.has(messageId)) {
                  return prev;
                }
                const next = new Set(prev);
                next.add(messageId);
                return next;
              });
            }

            const targetPage = Math.max(0, currentPageRef.current ?? DEFAULT_PAGE);
            loadPage(targetPage).catch((err) => console.error('Failed to refresh messages', err));
            return;
          }

          if (data.type === 'message-updated') {
            const messageId = data.message.id;
            if (typeof messageId !== 'number') {
              return;
            }

            setPage((prev) => {
              if (!prev) {
                return prev;
              }
              const exists = prev.content.some((item) => item.id === messageId);
              if (!exists) {
                return prev;
              }
              const updatedContent = prev.content.map((item) =>
                item.id === messageId ? data.message : item
              );
              return { ...prev, content: updatedContent };
            });

            setPendingCommentIds((prev) => {
              if (!prev.has(messageId)) {
                return prev;
              }
              const next = new Set(prev);
              next.delete(messageId);
              return next;
            });
          }
        } catch (err) {
          console.error('Failed to parse WebSocket message', err);
        }
      };

      socket.onclose = () => {
        if (!stopped) {
          scheduleReconnect();
        }
      };

      socket.onerror = () => {
        socket?.close();
      };
    };

    connect();

    return () => {
      stopped = true;
      if (reconnectTimeout) {
        clearTimeout(reconnectTimeout);
      }
      if (socket) {
        try {
          socket.close();
        } catch (err) {
          console.error('Failed to close WebSocket connection', err);
        }
      }
    };
  }, [loadPage]);

  const handleChangePage = (next: number) => {
    const params = new URLSearchParams(searchParams.toString());
    params.set('page', `${next}`);
    params.delete('channel');
    selectedChannels.forEach((channel) => params.append('channel', channel));
    const query = params.toString();
    router.push(query ? `/?${query}` : '/', { scroll: false });
  };

  const handleGenerateComment = useCallback(
    async (message: MessageView) => {
      const messageId = message.id;
      if (typeof messageId !== 'number') {
        return;
      }

      setCommentError(undefined);
      setPendingCommentIds((prev) => {
        if (prev.has(messageId)) {
          return prev;
        }
        const next = new Set(prev);
        next.add(messageId);
        return next;
      });

      try {
        const channelPart = (message.channel ?? '').trim();
        const textPart = (message.text ?? message.caption ?? '').trim();
        const datePart = message.publishedAt
          ? new Date(message.publishedAt).toLocaleString('ru-RU')
          : '';
        const parts = [channelPart, textPart, datePart].filter(
          (part) => part.length > 0
        );
        const payload = {
          text: parts.join(' '),
        };
        const response = await fetchJson<AiCommentResponse>(
          `/api/messages/${messageId}/ai-comment`,
          {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
            },
            body: JSON.stringify(payload),
          }
        );

        setPage((prev) => {
          if (!prev) {
            return prev;
          }
          const updatedContent = prev.content.map((item) =>
            item.id === messageId ? { ...item, aiComment: response.comment } : item
          );
          return { ...prev, content: updatedContent };
        });
      } catch (err) {
        setCommentError(
          err instanceof Error ? err.message : 'Не удалось получить комментарий'
        );
      } finally {
        setPendingCommentIds((prev) => {
          if (!prev.has(messageId)) {
            return prev;
          }
          const next = new Set(prev);
          next.delete(messageId);
          return next;
        });
      }
    },
    [setPage, setCommentError, setPendingCommentIds]
  );

  useEffect(() => {
    if (typeof document === 'undefined') {
      return;
    }

    const baseTitle = 'Telegram Explorer';
    if (unreadMessageIds.length > 0) {
      document.title = `🟢 ${unreadMessageIds.length} · ${baseTitle}`;
    } else {
      document.title = baseTitle;
    }

    return () => {
      document.title = baseTitle;
    };
  }, [unreadMessageIds.length]);

  const handleChannelsChange = (values: string[]) => {
    const uniqueValues = Array.from(
      new Set(
        values
          .map((value) => value.trim())
          .filter((value) => value.length > 0)
      )
    );

    const params = new URLSearchParams(searchParams.toString());
    params.delete('channel');
    params.delete('page');
    uniqueValues.forEach((value) => params.append('channel', value));

    const query = params.toString();
    router.push(query ? `/?${query}` : '/', { scroll: false });
  };

  return (
    <div className="py-4">
      <div className="d-flex flex-column flex-lg-row justify-content-between align-items-lg-end gap-3 mb-3">
        <div>
          <h2 className="h4 mb-0">Последние сообщения</h2>
          <p className="text-body-secondary mb-0">Страница {currentPage + 1}</p>
        </div>
        <div className="w-100 w-lg-auto">
          <label htmlFor="channelFilter" className="form-label mb-1">
            Канал
          </label>
          <ChannelSelector
            id="channelFilter"
            name="channelFilter"
            labelAll="Все каналы"
            placeholder="Выберите канал"
            channels={channels}
            extraSelectedChannels={extraSelectedChannels}
            selectedChannels={selectedChannels}
            onChange={handleChannelsChange}
          />
          {channelsLoading && (
            <div className="form-text">Загружаем список каналов…</div>
          )}
          {channelsError && (
            <div className="form-text text-danger">{channelsError}</div>
          )}
        </div>
      </div>

      {error && (
        <div className="alert alert-danger" role="alert">
          {error}
        </div>
      )}

      {commentError && (
        <div className="alert alert-warning" role="alert">
          {commentError}
        </div>
      )}

      <MessageTable
        page={page}
        loading={loading}
        error={error}
        highlightedMessageIds={highlightedMessageIds}
        onMessageInteraction={handleMessageInteraction}
        pendingCommentIds={pendingCommentIds}
        onGenerateComment={handleGenerateComment}
      />
      <Pagination page={page} onChange={handleChangePage} />
    </div>
  );
}
