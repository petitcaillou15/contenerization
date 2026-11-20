import { Injectable, inject, signal } from '@angular/core';
import { Subscription, timer, switchMap, of } from 'rxjs';
import { NotificationsService } from './notifications.service';
import { AuthService } from '../core/auth.service';
import { NotificationView } from '../core/api.types';

const POLL_MS = 15000;

@Injectable({ providedIn: 'root' })
export class NotificationsBadgeService {
  private notifications = inject(NotificationsService);
  private auth = inject(AuthService);

  readonly unread = signal(0);
  private sub?: Subscription;

  /** Démarre le polling (toutes les POLL_MS), uniquement effectif tant qu'on est connecté. */
  start(): void {
    this.stop();
    this.sub = timer(0, POLL_MS).pipe(
      switchMap(() => this.auth.isLoggedIn() ? this.notifications.list() : of<NotificationView[]>([])),
    ).subscribe(list => this.unread.set(list.filter(n => !n.read).length));
  }

  /** Force un rafraîchissement immédiat du compteur. */
  refresh(): void {
    if (!this.auth.isLoggedIn()) { this.unread.set(0); return; }
    this.notifications.list().subscribe(list =>
      this.unread.set(list.filter(n => !n.read).length));
  }

  stop(): void {
    this.sub?.unsubscribe();
    this.sub = undefined;
  }
}
