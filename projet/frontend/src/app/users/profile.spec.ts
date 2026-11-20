import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { ProfileComponent } from './profile';

describe('ProfileComponent', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [ProfileComponent],
      providers: [
        provideRouter([]), provideHttpClient(), provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { paramMap: of({ get: () => '5' }) } },
      ],
    });
  });

  it('loads and renders the profile', () => {
    const fixture = TestBed.createComponent(ProfileComponent);
    const http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    http.expectOne('/api/users/users/5').flush(
      { id: 5, pseudo: 'Alice', bio: null, created_at: '2026-01-02T03:04:05Z' });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Alice');
  });

  it('lets the owner edit their bio via PATCH /api/users/users/me', () => {
    // token payload sub = 5 -> currentUserId() === route id 5 -> isMe()
    localStorage.setItem('token', `h.${btoa(JSON.stringify({ sub: '5' }))}.s`);
    const fixture = TestBed.createComponent(ProfileComponent);
    const http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    http.expectOne('/api/users/users/5').flush(
      { id: 5, pseudo: 'Alice', bio: null, created_at: '2026-01-02T03:04:05Z' });
    fixture.detectChanges();

    const cmp = fixture.componentInstance;
    expect(cmp.isMe()).toBe(true);
    cmp.bio = 'nouvelle bio';
    cmp.save();

    const req = http.expectOne('/api/users/users/me');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ bio: 'nouvelle bio' });
    req.flush({ id: 5, pseudo: 'Alice', bio: 'nouvelle bio', created_at: '2026-01-02T03:04:05Z' });
    expect(cmp.saved).toBe(true);
  });
});
