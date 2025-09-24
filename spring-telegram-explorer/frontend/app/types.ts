export interface MessageView {
  id: number;
  channel: string;
  comment: boolean;
  threadId: number | null;
  author: string | null;
  text: string | null;
  caption: string | null;
  hasMedia: boolean;
  imageUrl: string | null;
  aiComment: string | null;
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

export type MessageStreamEvent =
  | { type: 'message-created'; message: MessageView }
  | { type: 'message-updated'; message: MessageView };

export interface SearchResponse {
  query: string;
  results: MessageView[];
}

export interface AiCommentResponse {
  messageId: number;
  comment: string;
}

export interface ChannelView {
  username: string;
  title: string | null;
}
