'use client';

import { PageResponse, MessageView } from '../types';

interface PaginationProps {
  page?: PageResponse<MessageView>;
  onChange: (page: number) => void;
}

export function Pagination({ page, onChange }: PaginationProps) {
  if (!page) {
    return null;
  }

  const totalPages = Math.max(page.totalPages, 1);
  const currentPage = Math.min(page.pageNumber, totalPages - 1);

  const previousDisabled = page.first || currentPage <= 0;
  const nextDisabled = page.last || currentPage >= totalPages - 1;

  const goPrev = () => {
    if (!previousDisabled) {
      onChange(currentPage - 1);
    }
  };

  const goNext = () => {
    if (!nextDisabled) {
      onChange(currentPage + 1);
    }
  };

  return (
    <nav aria-label="Навигация по страницам" className="mt-3">
      <ul className="pagination justify-content-center flex-wrap gap-2">
        <li className={`page-item${previousDisabled ? ' disabled' : ''}`}>
          <button className="page-link" onClick={goPrev} type="button">
            Назад
          </button>
        </li>
        <li className="page-item disabled">
          <span className="page-link">
            {currentPage + 1} / {totalPages}
          </span>
        </li>
        <li className={`page-item${nextDisabled ? ' disabled' : ''}`}>
          <button className="page-link" onClick={goNext} type="button">
            Вперёд
          </button>
        </li>
      </ul>
    </nav>
  );
}
