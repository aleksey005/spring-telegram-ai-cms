import type { Metadata } from 'next';
import 'bootstrap/dist/css/bootstrap.min.css';
import './globals.css';

export const metadata: Metadata = {
  title: 'Telegram Explorer',
  description: 'Интерфейс для просмотра сообщений телеграм-каналов'
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
          <div className="container d-flex flex-column flex-md-row align-items-md-center gap-2">
            <h1 className="h3 mb-0">Telegram Explorer</h1>
            <form action="/search" method="get" className="ms-md-auto d-flex" role="search">
              <input
                type="text"
                name="q"
                className="form-control me-2"
                placeholder="Поиск (по эмбеддингам)"
                aria-label="Поиск по эмбеддингам"
              />
              <button type="submit" className="btn btn-outline-light">
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
