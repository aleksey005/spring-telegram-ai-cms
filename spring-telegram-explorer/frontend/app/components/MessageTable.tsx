'use client';

import { useMemo } from 'react';
import { MessageView, PageResponse } from '../types';

interface MessageTableProps {
  page?: PageResponse<MessageView>;
  loading: boolean;
  error?: string;
}

export function MessageTable({ page, loading, error }: MessageTableProps) {
  const rows = page?.content ?? [];

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
    <div className="table-responsive">
      <table className="table table-striped align-middle">
        <thead className="table-light">
          <tr>
            <th scope="col">ID</th>
            <th scope="col">Канал</th>
            <th scope="col">Автор</th>
            <th scope="col">Текст / Caption</th>
            <th scope="col">Медиа</th>
            <th scope="col">Дата</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((msg) => (
            <tr key={msg.id}>
              <th scope="row">{msg.id}</th>
              <td>{msg.channel}</td>
              <td>{msg.author ?? '—'}</td>
              <td>
                {msg.text && <div className="mb-1">{msg.text}</div>}
                {!msg.text && msg.caption && <div className="text-body-secondary">{msg.caption}</div>}
                {!msg.text && !msg.caption && <span className="text-body-secondary">(пусто)</span>}
              </td>
              <td>
                <span className={msg.hasMedia ? 'badge text-bg-success' : 'badge text-bg-secondary'}>
                  {msg.hasMedia ? 'Есть' : 'Нет'}
                </span>
              </td>
              <td>
                {msg.publishedAt
                  ? new Date(msg.publishedAt).toLocaleString('ru-RU')
                  : <span className="text-body-secondary">—</span>}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
