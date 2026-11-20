import { TestBed, fakeAsync, tick, discardPeriodicTasks } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { NotificationsBadgeService } from './notifications-badge.service';
import { AuthService } from '../core/auth.service';

describe('NotificationsBadgeService', () => {
  let svc: NotificationsBadgeService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [NotificationsBadgeService, provideHttpClient(), provideHttpClientTesting()],
    });
    svc = TestBed.inject(NotificationsBadgeService);
    http = TestBed.inject(HttpTestingController);
    (TestBed.inject(AuthService) as any)['_token'].set('JWT'); // simulate logged in
  });

  it('polls and exposes the unread count', fakeAsync(() => {
    svc.start();
    tick(0);
    http.expectOne('/api/notifications/notifications').flush([
      { id: 1, type: 'comment_on_post', payload: {}, read: false, created_at: '2026-01-02T03:04:05Z' },
      { id: 2, type: 'comment_on_post', payload: {}, read: true, created_at: '2026-01-02T03:04:05Z' },
    ]);
    expect(svc.unread()).toBe(1);

    svc.stop();
    discardPeriodicTasks();
  }));

  it('does not poll when logged out', fakeAsync(() => {
    (TestBed.inject(AuthService) as any)['_token'].set(null);
    svc.start();
    tick(0);
    http.expectNone('/api/notifications/notifications');
    expect(svc.unread()).toBe(0);
    svc.stop();
    discardPeriodicTasks();
  }));
});
