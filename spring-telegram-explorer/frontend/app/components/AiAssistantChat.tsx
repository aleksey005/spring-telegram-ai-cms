'use client';

import { FormEvent, useEffect, useMemo, useRef, useState } from 'react';
import { resolveWebSocketUrl } from '../lib/websocket';
import type { AssistantMessageEvent } from '../types';

type ChatRole = 'assistant' | 'user' | 'system';

interface ChatMessage {
  id: number;
  role: ChatRole;
  text: string;
}

interface AiAssistantChatProps {
  active: boolean;
}

export function AiAssistantChat({ active }: AiAssistantChatProps) {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [inputValue, setInputValue] = useState('');
  const [status, setStatus] = useState<'connecting' | 'open' | 'closed'>('closed');
  const socketRef = useRef<WebSocket | null>(null);
  const reconnectRef = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
  const messageIdRef = useRef(0);
  const containerRef = useRef<HTMLDivElement | null>(null);

  const nextMessageId = () => {
    messageIdRef.current += 1;
    return messageIdRef.current;
  };

  useEffect(() => {
    if (!active) {
      if (reconnectRef.current) {
        clearTimeout(reconnectRef.current);
        reconnectRef.current = undefined;
      }
      const socket = socketRef.current;
      socketRef.current = null;
      if (socket) {
        try {
          socket.close();
        } catch (err) {
          console.error('Failed to close assistant WebSocket', err);
        }
      }
      setStatus('closed');
      return;
    }

    let stopped = false;

    const connect = () => {
      if (stopped || !active) {
        return;
      }
      setStatus('connecting');

      let url: string;
      try {
        url = resolveWebSocketUrl('/ws/assistant');
      } catch (err) {
        console.error('Failed to resolve assistant WebSocket URL', err);
        scheduleReconnect();
        return;
      }

      let socket: WebSocket;
      try {
        socket = new WebSocket(url);
      } catch (err) {
        console.error('Failed to open assistant WebSocket connection', err);
        scheduleReconnect();
        return;
      }

      socketRef.current = socket;

      socket.onopen = () => {
        setStatus('open');
      };

      socket.onclose = () => {
        socketRef.current = null;
        setStatus('closed');
        if (!stopped) {
          scheduleReconnect();
        }
      };

      socket.onerror = () => {
        socket.close();
      };

      socket.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data) as AssistantMessageEvent;
          if (!data || data.type !== 'assistant-message' || !data.text) {
            return;
          }
          const role: ChatRole = data.role === 'assistant' || data.role === 'system' ? data.role : 'assistant';
          setMessages((prev) => [
            ...prev,
            { id: nextMessageId(), role, text: data.text },
          ]);
        } catch (err) {
          console.error('Failed to parse assistant message', err);
        }
      };
    };

    const scheduleReconnect = () => {
      if (stopped || !active) {
        return;
      }
      if (reconnectRef.current) {
        clearTimeout(reconnectRef.current);
      }
      reconnectRef.current = setTimeout(connect, 2000);
    };

    connect();

    return () => {
      stopped = true;
      if (reconnectRef.current) {
        clearTimeout(reconnectRef.current);
        reconnectRef.current = undefined;
      }
      const socket = socketRef.current;
      socketRef.current = null;
      if (socket) {
        try {
          socket.close();
        } catch (err) {
          console.error('Failed to close assistant WebSocket', err);
        }
      }
    };
  }, [active]);

  useEffect(() => {
    if (!containerRef.current) {
      return;
    }
    containerRef.current.scrollTop = containerRef.current.scrollHeight;
  }, [messages]);

  const statusLabel = useMemo(() => {
    switch (status) {
      case 'open':
        return 'онлайн';
      case 'connecting':
        return 'подключение…';
      default:
        return 'офлайн';
    }
  }, [status]);

  const statusIndicatorClass = useMemo(() => {
    if (status === 'open') {
      return 'text-success';
    }
    if (status === 'connecting') {
      return 'text-warning';
    }
    return 'text-danger';
  }, [status]);

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const trimmed = inputValue.trim();
    if (!trimmed) {
      return;
    }

    setMessages((prev) => [
      ...prev,
      { id: nextMessageId(), role: 'user', text: trimmed },
    ]);

    const socket = socketRef.current;
    if (socket && socket.readyState === WebSocket.OPEN) {
      try {
        socket.send(JSON.stringify({ type: 'user-message', text: trimmed }));
      } catch (err) {
        console.error('Failed to send message to assistant', err);
        setMessages((prev) => [
          ...prev,
          {
            id: nextMessageId(),
            role: 'system',
            text: 'Не удалось отправить сообщение. Попробуйте еще раз чуть позже.',
          },
        ]);
      }
    } else {
      setMessages((prev) => [
        ...prev,
        {
          id: nextMessageId(),
          role: 'system',
          text: 'Ассистент недоступен. Сообщение не отправлено.',
        },
      ]);
    }

    setInputValue('');
  };

  return (
    <div className="card shadow-sm w-100 assistant-chat">
      <div className="card-body d-flex flex-column gap-3 assistant-chat__body">
        <div className="d-flex flex-wrap justify-content-between align-items-start gap-2">
          <div>
            <h3 className="h5 mb-1">AI-ассистент</h3>
            <p className="mb-0 text-body-secondary">
              Помогу принять заявку на мониторинг каналов, расскажу о стоимости и соберу контакты.
            </p>
          </div>
          <div className="text-end small text-body-secondary">
            <span className={`me-2 ${statusIndicatorClass}`}>●</span>
            {statusLabel}
          </div>
        </div>
        <div ref={containerRef} className="assistant-chat__messages">
          {messages.length === 0 ? (
            <p className="assistant-chat__placeholder">Ожидаем сообщение ассистента…</p>
          ) : (
            <div className="assistant-chat__messages-list">
              {messages.map((message) => {
                const messageClass =
                  message.role === 'user'
                    ? 'assistant-chat__message assistant-chat__message--user'
                    : message.role === 'assistant'
                      ? 'assistant-chat__message assistant-chat__message--assistant'
                      : 'assistant-chat__message assistant-chat__message--system';

                return (
                  <div key={message.id} className={messageClass}>
                    <div className={`assistant-chat__bubble assistant-chat__bubble--${message.role}`}>
                      <div className="assistant-chat__sender">
                        {message.role === 'user'
                          ? 'Вы'
                          : message.role === 'assistant'
                            ? 'Ассистент'
                            : 'Система'}
                      </div>
                      <div className="assistant-chat__text">{message.text}</div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
        <form className="d-flex flex-column gap-2" onSubmit={handleSubmit}>
          <label htmlFor="assistantMessage" className="form-label mb-0">
            Ваш запрос
          </label>
          <textarea
            id="assistantMessage"
            name="assistantMessage"
            className="form-control"
            rows={3}
            value={inputValue}
            onChange={(event) => setInputValue(event.target.value)}
            placeholder="Например: хочу добавить канал @example и узнать цену"
          />
          <div className="d-flex justify-content-end">
            <button type="submit" className="btn btn-primary" disabled={status === 'connecting'}>
              Отправить
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
