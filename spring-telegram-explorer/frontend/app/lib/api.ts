const DEFAULT_BASE_URL = '';

const apiBase = process.env.NEXT_PUBLIC_API_BASE_URL ?? DEFAULT_BASE_URL;

export async function fetchJson<T>(input: string, init?: RequestInit): Promise<T> {
  const url = `${apiBase}${input}`;
  const response = await fetch(url, {
    ...init,
    headers: {
      'Accept': 'application/json',
      ...(init?.headers ?? {})
    }
  });

  if (!response.ok) {
    const body = await response.text();
    throw new Error(`Запрос ${url} завершился ошибкой ${response.status}: ${body}`);
  }

  return (await response.json()) as T;
}
