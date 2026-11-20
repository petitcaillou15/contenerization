import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { UsersService } from './users.service';

describe('UsersService', () => {
  let service: UsersService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [UsersService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(UsersService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('profile hits /api/users/users/{id}', () => {
    service.profile(7).subscribe();
    const req = http.expectOne('/api/users/users/7');
    expect(req.request.method).toBe('GET');
    req.flush({ id: 7, pseudo: 'bob', bio: null, created_at: 'x' });
  });

  it('updateBio patches /api/users/users/me with the bio', () => {
    service.updateBio('hello').subscribe();
    const req = http.expectOne('/api/users/users/me');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ bio: 'hello' });
    req.flush({ id: 7, pseudo: 'bob', bio: 'hello', created_at: 'x' });
  });
});
