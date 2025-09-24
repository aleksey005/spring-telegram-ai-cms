import type { Metadata } from 'next';
import 'bootstrap/dist/css/bootstrap.min.css';
import './globals.css';

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
  return (
    <html lang="ru">
      <body>
        <div className="bg-dark text-white py-2">
          <div className="container d-flex flex-column flex-lg-row align-items-lg-center gap-3">
            <h1 className="h3 mb-0">Telegram Explorer</h1>
            <form
              action="/search"
              method="get"
              className="ms-lg-auto d-flex flex-column flex-sm-row align-items-stretch align-items-sm-center gap-2 w-100 w-lg-auto search-form"
              role="search"
            >
              <input
                type="text"
                name="q"
                className="form-control flex-grow-1"
                placeholder="Поиск (по эмбеддингам)"
                aria-label="Поиск по эмбеддингам"
              />
              <button type="submit" className="btn btn-outline-light search-form__submit">
                Искать
              </button>
            </form>
          </div>
        </div>
        <main>
          <div className="container">{children}</div>
        </main>
        <footer className="bg-body-tertiary py-3 mt-auto border-top">
          <div className="container text-muted small">
            Данные берутся из локальной базы Telegram Explorer. Интерфейс собран на Next.js 15 и Bootstrap 5.
          </div>
        </footer>
      </body>
    </html>
  );
}
