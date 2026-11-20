import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { NotificationsComponent } from './notifications';

describe('NotificationsComponent', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem('token', 'JWT');
    TestBed.configureTestingModule({
      imports: [NotificationsComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads the list on init and renders a human summary', () => {
    const fixture = TestBed.createComponent(NotificationsComponent);
    fixture.detectChanges(); // ngOnInit -> load() + badge.refresh() = 2 GET

    const reqs = http.match('/api/notifications/notifications');
    expect(reqs.length).toBe(2);
    reqs.forEach(r => r.flush([
      {
        id: 1, type: 'comment_on_post',
        payload: { comment_author_pseudo: 'Bob', post_id: 7 },
        read: false, created_at: '2026-01-02T03:04:05Z',
      },
    ]));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Bob a commenté votre post #7');
  });

  it('marks a notification read then reloads', () => {
    const fixture = TestBed.createComponent(NotificationsComponent);
    fixture.detectChanges();
    http.match('/api/notifications/notifications').forEach(r => r.flush([
      { id: 1, type: 'comment_on_post', payload: {}, read: false, created_at: '2026-01-02T03:04:05Z' },
    ]));

    fixture.componentInstance.markRead(1);
    const patch = http.expectOne('/api/notifications/notifications/1/read');
    expect(patch.request.method).toBe('PATCH');
    patch.flush(null, { status: 204, statusText: 'No Content' });

    const reloads = http.match('/api/notifications/notifications');
    expect(reloads.length).toBe(2); // load() + badge.refresh()
    reloads.forEach(r => r.flush([]));
    expect(fixture.componentInstance.list().length).toBe(0);
  });
});
