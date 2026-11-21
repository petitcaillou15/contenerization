import { Component, OnDestroy, inject } from '@angular/core';
import { RouterOutlet, RouterLink } from '@angular/router';
import { AuthService } from './core/auth.service';
import { NotificationsBadgeService } from './notifications/notifications-badge.service';
import { VersionFooter } from './version/version-footer';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, VersionFooter],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App implements OnDestroy {
  auth = inject(AuthService);
  badge = inject(NotificationsBadgeService);

  constructor() {
    this.badge.start();
  }

  ngOnDestroy(): void {
    this.badge.stop();
  }
}
