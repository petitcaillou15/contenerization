import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Page, PostSummary, PostDetail } from '../core/api.types';

@Injectable({ providedIn: 'root' })
export class PostsService {
  private http = inject(HttpClient);

  feed(page = 0, limit = 20): Observable<Page<PostSummary>> {
    const params = new HttpParams().set('page', page).set('limit', limit);
    return this.http.get<Page<PostSummary>>('/api/posts/posts', { params });
  }
  detail(id: number): Observable<PostDetail> {
    return this.http.get<PostDetail>(`/api/posts/posts/${id}`);
  }
  create(title: string, content: string): Observable<{ id: number }> {
    return this.http.post<{ id: number }>('/api/posts/posts', { title, content });
  }
  remove(id: number): Observable<void> {
    return this.http.delete<void>(`/api/posts/posts/${id}`);
  }
  addComment(postId: number, content: string): Observable<{ id: number }> {
    return this.http.post<{ id: number }>(`/api/posts/posts/${postId}/comments`, { content });
  }
}
