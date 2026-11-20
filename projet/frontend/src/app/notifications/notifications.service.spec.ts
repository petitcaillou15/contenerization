import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { NotificationsService } from './notifications.service';

describe('NotificationsService', () => {
  let service: NotificationsService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [NotificationsService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(NotificationsService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('list hits /api/notifications/notifications', () => {
    service.list().subscribe();
    const req = http.expectOne('/api/notifications/notifications');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('markRead patches /api/notifications/notifications/{id}/read', () => {
    service.markRead(4).subscribe();
    const req = http.expectOne('/api/notifications/notifications/4/read');
    expect(req.request.method).toBe('PATCH');
    req.flush(null, { status: 204, statusText: 'No Content' });
  });
});
