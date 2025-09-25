export function resolveWebSocketUrl(path: string): string {
  const base = process.env.NEXT_PUBLIC_API_BASE_URL ?? '';
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  const httpUrl = `${base}${normalizedPath}`;

  if (httpUrl.startsWith('ws://') || httpUrl.startsWith('wss://')) {
    return httpUrl;
  }

  if (httpUrl.startsWith('http://')) {
    return `ws://${httpUrl.substring('http://'.length)}`;
  }

  if (httpUrl.startsWith('https://')) {
    return `wss://${httpUrl.substring('https://'.length)}`;
  }

  if (httpUrl.startsWith('//')) {
    const protocol =
      typeof window !== 'undefined' && window.location?.protocol === 'http:' ? 'ws:' : 'wss:';
    return `${protocol}${httpUrl}`;
  }

  if (httpUrl.startsWith('/')) {
    if (typeof window === 'undefined' || !window.location) {
      throw new Error('Cannot resolve relative WebSocket URL in a non-browser environment');
    }
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    return `${protocol}//${window.location.host}${httpUrl}`;
  }

  throw new Error('Unsupported WebSocket URL');
}
