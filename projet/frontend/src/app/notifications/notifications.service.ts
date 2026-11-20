import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { NotificationView } from '../core/api.types';

@Injectable({ providedIn: 'root' })
export class NotificationsService {
  private http = inject(HttpClient);

  list(): Observable<NotificationView[]> {
    return this.http.get<NotificationView[]>('/api/notifications/notifications');
  }
  markRead(id: number): Observable<void> {
    return this.http.patch<void>(`/api/notifications/notifications/${id}/read`, {});
  }
}
