import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { FeedComponent } from './feed';

describe('FeedComponent', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [FeedComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
  });

  it('loads the first page on init', () => {
    const fixture = TestBed.createComponent(FeedComponent);
    const http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    const req = http.expectOne('/api/posts/posts?page=0&limit=20');
    req.flush({ items: [{ id: 1, title: 'Hello', author_id: 2, created_at: '2026-01-02T03:04:05Z' }], page: 0, limit: 20, total: 1 });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Hello');
  });

  it('creates a post then reloads page 0', () => {
    localStorage.setItem('token', 'JWT');
    const fixture = TestBed.createComponent(FeedComponent);
    const http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    http.expectOne('/api/posts/posts?page=0&limit=20').flush({ items: [], page: 0, limit: 20, total: 0 });

    const cmp = fixture.componentInstance;
    cmp.newTitle = 'T';
    cmp.newContent = 'C';
    cmp.create();

    http.expectOne(r => r.method === 'POST' && r.url === '/api/posts/posts').flush({ id: 9 });
    http.expectOne('/api/posts/posts?page=0&limit=20').flush({ items: [], page: 0, limit: 20, total: 0 });
    expect(cmp.newTitle).toBe('');
  });
});
