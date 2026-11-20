import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { LoginComponent } from './login';

describe('LoginComponent', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
  });

  it('logs in and navigates home on success', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    const nav = spyOn(TestBed.inject(Router), 'navigateByUrl');
    const http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    const cmp = fixture.componentInstance;
    cmp.email = 'a@example.com';
    cmp.password = 'pw';
    cmp.submit();

    http.expectOne('/api/users/login').flush({ token: 'JWT' });
    expect(localStorage.getItem('token')).toBe('JWT');
    expect(nav).toHaveBeenCalledWith('/');
  });

  it('shows an error message on 401', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    const http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    const cmp = fixture.componentInstance;
    cmp.email = 'a@example.com';
    cmp.password = 'bad';
    cmp.submit();
    http.expectOne('/api/users/login').flush({ error: 'invalid' }, { status: 401, statusText: 'Unauthorized' });
    expect(cmp.error).toBeTruthy();
  });
});
