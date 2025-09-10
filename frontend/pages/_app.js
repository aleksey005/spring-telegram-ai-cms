import 'bootstrap/dist/css/bootstrap.min.css';
import { useEffect, useState } from 'react';
import Head from 'next/head';

export default function MyApp({ Component, pageProps }) {
  const [theme, setTheme] = useState('light');

  useEffect(() => {
    const stored = localStorage.getItem('theme');
    if (stored === 'light' || stored === 'dark') {
      setTheme(stored);
    }
  }, []);

  useEffect(() => {
    document.documentElement.setAttribute('data-bs-theme', theme);
    localStorage.setItem('theme', theme);
  }, [theme]);

  return (
    <>
      <Head>
        <title>AI-публикатор</title>
      </Head>
      <button
        type="button"
        className="theme-toggle"
        onClick={() => setTheme(theme === 'light' ? 'dark' : 'light')}
        aria-label="Toggle theme"
      >
        {theme === 'light' ? '🌙' : '☀️'}
      </button>
      <Component {...pageProps} />
      <style jsx global>{`
        .theme-toggle {
          position: fixed;
          top: 0.25rem;
          right: 1rem;
          z-index: 1100;
          background: transparent;
          border: none;
          font-size: 1.5rem;
        }
      `}</style>
    </>
  );
}
