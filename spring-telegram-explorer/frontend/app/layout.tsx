import Script from 'next/script';
import type { Metadata } from 'next';
import 'bootstrap/dist/css/bootstrap.min.css';
import './globals.css';
import { ThemeProvider } from './components/ThemeProvider';
import { ThemeToggle } from './components/ThemeToggle';
import { AssistantLauncher } from './components/AssistantLauncher';

const themeInitScript = `(() => {
  try {
    const storageKey = 'telegram-explorer-theme';
    const stored = window.localStorage.getItem(storageKey);
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    const theme = stored === 'light' || stored === 'dark' ? stored : prefersDark ? 'dark' : 'light';
    const root = document.documentElement;
    root.dataset.bsTheme = theme;
    root.dataset.theme = theme;
    root.style.colorScheme = theme;
  } catch (error) {
    console.error('Failed to apply theme', error);
  }
})();`;

export const metadata: Metadata = {
  title: 'Telegram Explorer',
  description: 'Интерфейс для просмотра сообщений телеграм-каналов',
  icons: {
    icon: [
      { url: '/favicon-96x96.png', sizes: '96x96', type: 'image/png' },
      { url: '/favicon.svg', type: 'image/svg+xml' }
    ],
    shortcut: '/favicon.ico',
    apple: { url: '/apple-touch-icon.png', sizes: '180x180' }
  },
  manifest: '/site.webmanifest'
};

export default function RootLayout({
  children
}: {
  children: React.ReactNode;
}) {
  const searchInputId = 'app-global-search';

  return (
    <html lang="ru" suppressHydrationWarning>
      <body className="bg-body text-body">
        <Script id="theme-initializer" strategy="beforeInteractive">
          {themeInitScript}
        </Script>
        <ThemeProvider>
          <div className="app-backdrop">
            <div className="app-shell container-xl">
              <header className="app-header surface-card">
                <div className="app-header__top">
                  <div className="app-brand">
                    <span className="app-brand__label">Мониторинг каналов</span>
                    <h1 className="app-brand__title">Telegram Explorer</h1>
                    <p className="app-brand__subtitle">
                      Современная панель для анализа и отслеживания контента телеграм-каналов.
                    </p>
                  </div>
                  <div className="app-header__actions">
                    <AssistantLauncher />
                    <ThemeToggle />
                  </div>
                </div>
                <form
                  action="/search"
                  method="get"
                  className="search-form"
                  role="search"
                  aria-label="Поиск по эмбеддингам"
                >
                  <label htmlFor={searchInputId} className="search-form__label">
                    Умный поиск по базе
                  </label>
                  <div className="search-form__field">
                    <span className="search-form__icon" aria-hidden="true">
                      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                        <path
                          d="M15.5 15.5L21 21"
                          stroke="currentColor"
                          strokeWidth="1.5"
                          strokeLinecap="round"
                          strokeLinejoin="round"
                        />
                        <path
                          d="M10.5 17C14.0899 17 17 14.0899 17 10.5C17 6.91015 14.0899 4 10.5 4C6.91015 4 4 6.91015 4 10.5C4 14.0899 6.91015 17 10.5 17Z"
                          stroke="currentColor"
                          strokeWidth="1.5"
                          strokeLinecap="round"
                          strokeLinejoin="round"
                        />
                      </svg>
                    </span>
                    <input
                      id={searchInputId}
                      type="text"
                      name="q"
                      className="search-form__input"
                      placeholder="Введите запрос или ключевое слово"
                    />
                  </div>
                  <button type="submit" className="btn btn-gradient search-form__submit">
                    Найти сообщения
                  </button>
                </form>
              </header>
              <main className="app-main flex-grow-1">
                <section className="app-content surface-card">{children}</section>
              </main>
              <footer className="app-footer surface-card">
                <div>
                  <p className="app-footer__title">Telegram Explorer</p>
                  <p className="app-footer__meta">
                    Современная панель для анализа и отслеживания контента телеграм-каналов.
                  </p>
                </div>
              </footer>
            </div>
          </div>
        </ThemeProvider>
      </body>
    </html>
  );
}
