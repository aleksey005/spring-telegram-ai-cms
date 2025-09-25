'use client';

import { useTheme } from './ThemeProvider';

export function ThemeToggle() {
  const { theme, toggleTheme } = useTheme();

  return (
    <button
      type="button"
      className="theme-toggle"
      onClick={toggleTheme}
      data-theme-mode={theme}
      aria-label={theme === 'light' ? 'Включить тёмную тему' : 'Включить светлую тему'}
    >
      <span className="visually-hidden">
        {theme === 'light' ? 'Включить тёмную тему' : 'Включить светлую тему'}
      </span>
      <span className="theme-toggle__icon theme-toggle__icon--sun" aria-hidden="true">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path
            d="M12 3V5.5M12 18.5V21M4.2218 4.22183L5.99998 6.00001M18 18L19.7782 19.7782M3 12H5.5M18.5 12H21M4.2218 19.7782L6.00001 18M18 6.00002L19.7782 4.22183M16 12C16 14.2091 14.2091 16 12 16C9.79086 16 8 14.2091 8 12C8 9.79087 9.79086 8 12 8C14.2091 8 16 9.79087 16 12Z"
            stroke="currentColor"
            strokeWidth="1.5"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      </span>
      <span className="theme-toggle__icon theme-toggle__icon--moon" aria-hidden="true">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path
            d="M21 12.79C20.7285 13.0031 20.4317 13.1851 20.115 13.3325C18.6817 13.9907 17.018 14.1085 15.5 13.66C12.1863 12.6766 10.1869 9.14664 11.1703 5.83293C11.6192 4.31157 12.5744 3.01825 13.8298 2.17029C9.94982 1.44362 5.90251 3.74773 4.33253 7.61494C2.5478 12.0014 4.73405 17.0236 9.12051 18.8083C12.9877 20.3783 17.3529 18.7443 19.4881 15.2716C20.143 14.2063 20.6652 13.0402 21 11.79V12.79Z"
            stroke="currentColor"
            strokeWidth="1.5"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      </span>
    </button>
  );
}
