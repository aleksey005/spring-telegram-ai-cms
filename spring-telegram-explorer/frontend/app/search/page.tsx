'use client';

export const dynamic = 'force-static';

import { Suspense, useEffect, useState } from 'react';
import { useSearchParams } from 'next/navigation';
import { fetchJson } from '../lib/api';
import { MessageTable } from '../components/MessageTable';
import { MessageView, SearchResponse } from '../types';

export default function SearchPage() {
  return (
    <Suspense fallback={<div className="py-4">Подготовка страницы поиска…</div>}>
      <SearchPageContent />
    </Suspense>
  );
}

function SearchPageContent() {
  const searchParams = useSearchParams();
  const query = searchParams.get('q') ?? '';

  const [results, setResults] = useState<MessageView[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>();

  useEffect(() => {
    if (!query) {
      setResults([]);
      return;
    }

    const controller = new AbortController();
    setLoading(true);
    setError(undefined);
    fetchJson<SearchResponse>(`/api/search?q=${encodeURIComponent(query)}`, { signal: controller.signal })
      .then((response) => {
        setResults(response.results);
      })
      .catch((err) => {
        if (err.name !== 'AbortError') {
          setError(err instanceof Error ? err.message : 'Не удалось выполнить поиск');
        }
      })
      .finally(() => {
        setLoading(false);
      });

    return () => controller.abort();
  }, [query]);

  return (
    <div className="py-4">
      <h2 className="h4">Поиск (эмбеддинги)</h2>
      <p className="text-body-secondary">Введите запрос и нажмите кнопку в шапке.</p>

      {!query && <div className="alert alert-info">Введите запрос для поиска.</div>}
      {query && loading && <div className="alert alert-secondary">Ищем по запросу «{query}»…</div>}
      {error && <div className="alert alert-danger">{error}</div>}
      {query && !loading && results.length === 0 && !error && (
        <div className="alert alert-warning">Результатов пока нет (демо-заглушка).</div>
      )}
      {results.length > 0 && (
        <MessageTable
          page={{
            content: results,
            pageNumber: 0,
            pageSize: results.length,
            totalElements: results.length,
            totalPages: 1,
            first: true,
            last: true
          }}
          loading={loading}
        />
      )}
    </div>
  );
}
