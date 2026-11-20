export interface LoginResponse { token: string; }
export interface PublicProfile { id: number; pseudo: string; bio: string | null; created_at: string; }
export interface PostSummary { id: number; title: string; author_id: number; created_at: string; }
export interface Page<T> { items: T[]; page: number; limit: number; total: number; }
export interface CommentView { id: number; author_id: number; content: string; created_at: string; }
export interface PostDetail {
  id: number; title: string; content: string; author_id: number; created_at: string;
  comments: CommentView[];
}
export interface NotificationView {
  id: number; type: string; payload: any; read: boolean; created_at: string;
}
