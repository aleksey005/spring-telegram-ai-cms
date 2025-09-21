'use client';

import { Suspense, useCallback, useEffect, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { fetchJson } from './lib/api';
import { MessageTable } from './components/MessageTable';
import { Pagination } from './components/Pagination';
import { MessageView, PageResponse } from './types';

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

  const loadPage = useCallback(
    async (pageIndex: number) => {
      setLoading(true);
      setError(undefined);
      try {
        const response = await fetchJson<PageResponse<MessageView>>(`/api/messages?page=${pageIndex}`);
        setPage(response);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Не удалось загрузить сообщения');
        setPage(undefined);
      } finally {
        setLoading(false);
      }
    },
    []
  );

  useEffect(() => {
    loadPage(currentPage).catch((err) => console.error(err));
  }, [currentPage, loadPage]);

  const handleChangePage = (next: number) => {
    const params = new URLSearchParams(searchParams.toString());
    params.set('page', `${next}`);
    router.push(`/?${params.toString()}`, { scroll: false });
  };

  return (
    <div className="py-4">
      <div className="d-flex justify-content-between align-items-center mb-3">
        <div>
          <h2 className="h4 mb-0">Последние сообщения</h2>
          <p className="text-body-secondary mb-0">Страница {currentPage + 1}</p>
        </div>
      </div>

      {error && (
        <div className="alert alert-danger" role="alert">
          {error}
        </div>
      )}

      <MessageTable page={page} loading={loading} error={error} />
      <Pagination page={page} onChange={handleChangePage} />
    </div>
  );
}
