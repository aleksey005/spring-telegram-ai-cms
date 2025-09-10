'use client';
import { useEffect, useMemo, useRef, useState } from 'react';

export default function SearchBox({
  value,
  onChange,
  disabled,
  onSearch,
  searchDisabled,
  hintsEnabled = false,
}) {
  const [items, setItems] = useState([]);
  const ctl = useRef(null);
  const debounced = useMemo(() => {
    let h;
    return (v, fn) => {
      clearTimeout(h);
      h = setTimeout(() => fn(v), 180);
    };
  }, []);

  useEffect(() => {
    if (!hintsEnabled || !value) {
      setItems([]);
      return;
    }
    debounced(value, async (s) => {
      ctl.current?.abort();
      ctl.current = new AbortController();
      try {
        const r = await fetch(`https://www.you_site.ru/suggest?q=${encodeURIComponent(s)}&limit=10`, {
          signal: ctl.current.signal,
          headers: { 'X-Session': 's1' },
          cache: 'no-store',
        });
        if (!r.ok) return;
        const data = await r.json();
        const unique = Array.from(
          new Set(data.suggestions.map((x) => x.text))
        );
        setItems(unique);
      } catch (_) {
        // ignore
      }
    });
  }, [value, debounced, hintsEnabled]);

  return (
    <div className="input-wrapper">
      <button
        type="button"
        className="btn btn-primary"
        onMouseDown={(e) => e.preventDefault()}
        onClick={onSearch}
        disabled={searchDisabled}
      >
        AI
      </button>
      <div className="input-container">
        <input
          type="text"
          className="form-control"
          value={value}
          disabled={disabled}
          placeholder="Введите запрос"
          onChange={(e) => onChange(e.target.value)}
          autoComplete="off"
          role="combobox"
          aria-expanded={hintsEnabled && items.length > 0}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && !searchDisabled) {
              onSearch();
            }
          }}
        />
        {value && (
          <button
            type="button"
            className="clear-btn"
            onMouseDown={(e) => e.preventDefault()}
            onClick={() => onChange('')}
          >
            ×
          </button>
        )}
        {hintsEnabled && items.length > 0 && (
          <ul className="suggestions">
            {items.map((t, i) => (
              <li
                key={i}
                onMouseDown={() =>
                  onChange((prev) => prev.replace(/([^\s]*)$/, t))
                }
              >
                {t}
              </li>
            ))}
          </ul>
        )}
      </div>
      <style jsx>{`
        .input-wrapper {
          display: flex;
          align-items: center;
          gap: 10px;
        }
        .input-container {
          position: relative;
          flex: 1;
        }
        .clear-btn {
          position: absolute;
          right: 10px;
          top: 50%;
          transform: translateY(-50%);
          background: none;
          border: none;
          font-size: 1.5rem;
          line-height: 1;
        }
        .suggestions {
          position: absolute;
          top: 100%;
          z-index: 10;
          margin-top: 4px;
          width: 100%;
          background: var(--bs-body-bg);
          color: var(--bs-body-color);
          border: 1px solid var(--bs-border-color);
          border-radius: 0.375rem;
          list-style: none;
          padding: 0;
        }
        .suggestions li {
          padding: 0.5rem 1rem;
          cursor: pointer;
        }
        .suggestions li:hover {
          background-color: var(--bs-secondary-bg);
        }
      `}</style>
    </div>
  );
}
