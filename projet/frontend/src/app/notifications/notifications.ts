import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { NotificationsService } from './notifications.service';
import { NotificationsBadgeService } from './notifications-badge.service';
import { NotificationView } from '../core/api.types';

@Component({
  selector: 'app-notifications',
  imports: [DatePipe],
  templateUrl: './notifications.html',
})
export class NotificationsComponent implements OnInit {
  private notifications = inject(NotificationsService);
  private badge = inject(NotificationsBadgeService);

  list = signal<NotificationView[]>([]);

  ngOnInit(): void { this.load(); this.badge.refresh(); }

  load(): void { this.notifications.list().subscribe(l => this.list.set(l)); }

  markRead(id: number): void {
    this.notifications.markRead(id).subscribe(() => { this.load(); this.badge.refresh(); });
  }

  summary(n: NotificationView): string {
    if (n.type === 'comment_on_post' && n.payload) {
      return `${n.payload.comment_author_pseudo ?? "Quelqu'un"} a commenté votre post #${n.payload.post_id ?? '?'}`;
    }
    return n.type;
  }
}
