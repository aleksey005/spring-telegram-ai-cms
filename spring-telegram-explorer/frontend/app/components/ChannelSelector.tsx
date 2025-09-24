'use client';

import { FormEvent, useEffect, useMemo, useRef, useState } from 'react';
import type { ChannelView } from '../types';

type ChannelSelectorProps = {
  id?: string;
  name?: string;
  labelAll?: string;
  placeholder?: string;
  channels: ChannelView[];
  extraSelectedChannels: string[];
  selectedChannels: string[];
  onChange: (values: string[]) => void;
};

type ChannelOption = {
  username: string;
  label: string;
  shortLabel: string;
  extra: boolean;
};

export function ChannelSelector({
  id,
  name,
  labelAll = 'Все',
  placeholder = 'Выберите канал',
  channels,
  extraSelectedChannels,
  selectedChannels,
  onChange,
}: ChannelSelectorProps) {
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState('');
  const containerRef = useRef<HTMLDivElement>(null);
  const searchInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (!open) {
      return;
    }

    const handleClick = (event: MouseEvent) => {
      if (!containerRef.current?.contains(event.target as Node)) {
        setOpen(false);
      }
    };

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClick);
    document.addEventListener('keydown', handleKeyDown);

    return () => {
      document.removeEventListener('mousedown', handleClick);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [open]);

  useEffect(() => {
    if (open) {
      const timeout = setTimeout(() => {
        searchInputRef.current?.focus();
        searchInputRef.current?.select();
      }, 0);
      return () => clearTimeout(timeout);
    }
    setSearch('');
    return undefined;
  }, [open]);

  const options = useMemo<ChannelOption[]>(() => {
    const mapped = channels.map<ChannelOption>((channel) => {
      const title = channel.title?.trim();
      const hasTitle = Boolean(title && title.length > 0);
      return {
        username: channel.username,
        label: hasTitle
          ? `${title} (@${channel.username})`
          : `@${channel.username}`,
        shortLabel: hasTitle ? `${title}` : `@${channel.username}`,
        extra: false,
      };
    });

    const extras = extraSelectedChannels.map<ChannelOption>((username) => ({
      username,
      label: `@${username}`,
      shortLabel: `@${username}`,
      extra: true,
    }));

    return [...mapped, ...extras];
  }, [channels, extraSelectedChannels]);

  const selectedSet = useMemo(() => new Set(selectedChannels), [selectedChannels]);
  const isAllSelected = selectedChannels.length === 0;

  const filteredOptions = useMemo(() => {
    const trimmed = search.trim().toLowerCase();
    if (!trimmed) {
      return options;
    }
    return options.filter((option) => {
      const haystack = `${option.label} ${option.username}`.toLowerCase();
      return haystack.includes(trimmed);
    });
  }, [options, search]);

  const buttonLabel = useMemo(() => {
    if (isAllSelected) {
      return labelAll;
    }

    if (selectedChannels.length === 1) {
      const only = selectedChannels[0];
      const match = options.find((option) => option.username === only);
      return match?.shortLabel ?? `@${only}`;
    }

    if (selectedChannels.length <= 3) {
      return selectedChannels
        .map((username) => {
          const match = options.find((option) => option.username === username);
          return match?.shortLabel ?? `@${username}`;
        })
        .join(', ');
    }

    return `Выбрано: ${selectedChannels.length}`;
  }, [isAllSelected, labelAll, options, selectedChannels]);

  const handleToggle = (username: string) => {
    const exists = selectedSet.has(username);
    const next = exists
      ? selectedChannels.filter((value) => value !== username)
      : [...selectedChannels, username];

    if (next.length === 0) {
      onChange([]);
      return;
    }

    onChange(next);
  };

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault();
  };

  return (
    <div ref={containerRef} className="channel-selector position-relative" id={id ? `${id}-container` : undefined}>
      <button
        type="button"
        id={id}
        name={name}
        className="channel-selector__button btn btn-outline-secondary w-100 d-flex align-items-center justify-content-between"
        aria-haspopup="listbox"
        aria-expanded={open}
        onClick={() => setOpen((prev) => !prev)}
      >
        <span className="flex-grow-1 text-truncate me-2">
          {buttonLabel || placeholder}
        </span>
        <span className="channel-selector__caret" aria-hidden="true">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path
              d="M4.646 6.646a.5.5 0 0 1 .708 0L8 9.293l2.646-2.647a.5.5 0 0 1 .708.708l-3 3a.5.5 0 0 1-.708 0l-3-3a.5.5 0 0 1 0-.708z"
              fill="currentColor"
            />
          </svg>
        </span>
      </button>
      {open && (
        <div className="channel-selector__menu shadow">
          <form className="border-bottom p-2" role="search" onSubmit={handleSubmit}>
            <input
              ref={searchInputRef}
              type="search"
              className="form-control form-control-sm"
              placeholder="Поиск канала"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              aria-label="Поиск канала"
            />
          </form>
          <ul className="channel-selector__options list-unstyled mb-0" role="listbox" aria-multiselectable="true">
            <li role="option" aria-selected={isAllSelected}>
              <button
                type="button"
                className={`channel-selector__option dropdown-item ${isAllSelected ? 'active' : ''}`}
                onClick={() => onChange([])}
              >
                <span className="form-check">
                  <input
                    className="form-check-input"
                    type="checkbox"
                    readOnly
                    tabIndex={-1}
                    checked={isAllSelected}
                  />
                  <span className="form-check-label">{labelAll}</span>
                </span>
              </button>
            </li>
            {filteredOptions.length === 0 && (
              <li>
                <div className="px-3 py-2 text-body-secondary small">Ничего не найдено</div>
              </li>
            )}
            {filteredOptions.map((option) => {
              const selected = selectedSet.has(option.username);
              return (
                <li key={option.username} role="option" aria-selected={selected}>
                  <button
                    type="button"
                    className={`channel-selector__option dropdown-item ${selected ? 'active' : ''}`}
                    onClick={() => handleToggle(option.username)}
                  >
                    <span className="form-check">
                      <input
                        className="form-check-input"
                        type="checkbox"
                        tabIndex={-1}
                        readOnly
                        checked={selected}
                      />
                      <span className="form-check-label d-flex flex-column text-start">
                        <span>{option.label}</span>
                        {option.extra && (
                          <span className="small text-body-secondary">Вне списка каналов</span>
                        )}
                      </span>
                    </span>
                  </button>
                </li>
              );
            })}
          </ul>
        </div>
      )}
    </div>
  );
}
