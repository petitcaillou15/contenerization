import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { PostsService } from './posts.service';

describe('PostsService', () => {
  let service: PostsService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [PostsService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(PostsService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('feed hits the paginated gateway route', () => {
    service.feed(1, 10).subscribe();
    const req = http.expectOne('/api/posts/posts?page=1&limit=10');
    expect(req.request.method).toBe('GET');
    req.flush({ items: [], page: 1, limit: 10, total: 0 });
  });

  it('detail hits /api/posts/posts/{id}', () => {
    service.detail(3).subscribe();
    const req = http.expectOne('/api/posts/posts/3');
    expect(req.request.method).toBe('GET');
    req.flush({ id: 3, title: 't', content: 'c', author_id: 1, created_at: 'x', comments: [] });
  });

  it('create posts to /api/posts/posts', () => {
    service.create('t', 'c').subscribe();
    const req = http.expectOne('/api/posts/posts');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ title: 't', content: 'c' });
    req.flush({ id: 5 });
  });

  it('remove issues DELETE', () => {
    service.remove(8).subscribe();
    const req = http.expectOne('/api/posts/posts/8');
    expect(req.request.method).toBe('DELETE');
    req.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('addComment targets the nested route', () => {
    service.addComment(3, 'hi').subscribe();
    const req = http.expectOne('/api/posts/posts/3/comments');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ content: 'hi' });
    req.flush({ id: 9 });
  });
});
