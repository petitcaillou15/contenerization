import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [AuthService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('login stores the token and flips isLoggedIn', () => {
    expect(service.isLoggedIn()).toBe(false);

    service.login('a@example.com', 'pw').subscribe();
    const req = http.expectOne('/api/users/login');
    expect(req.request.method).toBe('POST');
    req.flush({ token: 'JWT123' });

    expect(localStorage.getItem('token')).toBe('JWT123');
    expect(service.isLoggedIn()).toBe(true);
  });

  it('logout clears the token', () => {
    localStorage.setItem('token', 'x');
    service.logout();
    expect(localStorage.getItem('token')).toBeNull();
    expect(service.isLoggedIn()).toBe(false);
  });

  it('register posts credentials and does not store a token', () => {
    service.register('alice', 'a@example.com', 'pw').subscribe();
    const req = http.expectOne('/api/users/register');
    expect(req.request.method).toBe('POST');
    req.flush(null, { status: 201, statusText: 'Created' });
    expect(localStorage.getItem('token')).toBeNull();
  });

  it('currentUserId decodes the JWT sub for display only', () => {
    // header.payload.signature ; payload = {"sub":"42","pseudo":"alice"}
    const payload = btoa(JSON.stringify({ sub: '42', pseudo: 'alice' }));
    localStorage.setItem('token', `h.${payload}.s`);
    expect(service.currentUserId()).toBe(42);
  });
});
