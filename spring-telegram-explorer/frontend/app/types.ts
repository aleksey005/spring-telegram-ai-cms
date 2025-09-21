export interface MessageView {
  id: number;
  channel: string;
  comment: boolean;
  threadId: number | null;
  author: string | null;
  text: string | null;
  caption: string | null;
  hasMedia: boolean;
  publishedAt: string | null;
}

export interface PageResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface SearchResponse {
  query: string;
  results: MessageView[];
}
