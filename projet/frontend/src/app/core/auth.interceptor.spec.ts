import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

describe('authInterceptor', () => {
  let http: HttpClient;
  let ctrl: HttpTestingController;
  let auth: AuthService;
  let navigate: jasmine.Spy;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: Router, useValue: { navigateByUrl: (_u: string) => {} } },
      ],
    });
    http = TestBed.inject(HttpClient);
    ctrl = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
    navigate = spyOn(TestBed.inject(Router), 'navigateByUrl');
  });

  afterEach(() => ctrl.verify());

  it('adds the Bearer header when a token is present and URL is /api', () => {
    localStorage.setItem('token', 'JWT');
    (auth as any)['_token'].set('JWT');

    http.get('/api/posts/posts').subscribe();
    const req = ctrl.expectOne('/api/posts/posts');
    expect(req.request.headers.get('Authorization')).toBe('Bearer JWT');
    req.flush({});
  });

  it('does not add a header when there is no token', () => {
    http.get('/api/posts/posts').subscribe();
    const req = ctrl.expectOne('/api/posts/posts');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('on 401 clears the token and navigates to /login', () => {
    localStorage.setItem('token', 'JWT');
    (auth as any)['_token'].set('JWT');

    http.get('/api/notifications/notifications').subscribe({ error: () => {} });
    const req = ctrl.expectOne('/api/notifications/notifications');
    req.flush({ error: 'unauthorized' }, { status: 401, statusText: 'Unauthorized' });

    expect(localStorage.getItem('token')).toBeNull();
    expect(navigate).toHaveBeenCalledWith('/login');
  });
});
