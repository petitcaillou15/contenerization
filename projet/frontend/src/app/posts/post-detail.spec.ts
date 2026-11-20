import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { PostDetailComponent } from './post-detail';

describe('PostDetailComponent', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [PostDetailComponent],
      providers: [
        provideRouter([]), provideHttpClient(), provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { paramMap: of({ get: () => '3' }) } },
      ],
    });
  });

  it('loads and renders the post with its comments', () => {
    const fixture = TestBed.createComponent(PostDetailComponent);
    const http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    http.expectOne('/api/posts/posts/3').flush({
      id: 3, title: 'Titre', content: 'Corps', author_id: 1, created_at: '2026-01-02T03:04:05Z',
      comments: [{ id: 1, author_id: 2, content: 'Bravo', created_at: '2026-01-03T04:05:06Z' }],
    });
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Titre');
    expect(text).toContain('Bravo');
  });

  it('adds a comment then reloads', () => {
    localStorage.setItem('token', 'JWT');
    const fixture = TestBed.createComponent(PostDetailComponent);
    const http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    http.expectOne('/api/posts/posts/3').flush(
      { id: 3, title: 'T', content: 'C', author_id: 1, created_at: '2026-01-02T03:04:05Z', comments: [] });

    const cmp = fixture.componentInstance;
    cmp.newComment = 'Hello';
    cmp.addComment();
    http.expectOne(r => r.method === 'POST' && r.url === '/api/posts/posts/3/comments').flush({ id: 7 });
    http.expectOne('/api/posts/posts/3').flush(
      { id: 3, title: 'T', content: 'C', author_id: 1, created_at: '2026-01-02T03:04:05Z', comments: [] });
    expect(cmp.newComment).toBe('');
  });
});
