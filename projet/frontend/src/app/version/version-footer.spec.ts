import { TestBed, discardPeriodicTasks, fakeAsync, tick } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { VersionFooter } from './version-footer';

describe('VersionFooter', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VersionFooter],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
  });

  it('shows the frontend version and the versions returned by the services', fakeAsync(() => {
    const fixture = TestBed.createComponent(VersionFooter);
    const http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    tick(0); // le polling démarre via timer(0, ...)

    for (const s of ['users', 'posts', 'notifications']) {
      http.expectOne(`/api/${s}/version`).flush({ service: s, version: '2.0.0' });
    }
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent.replace(/\s+/g, ' ');
    expect(text).toContain('frontend 2.0.0');
    expect(text).toContain('users 2.0.0');
    expect(text).toContain('posts 2.0.0');
    expect(text).toContain('notifications 2.0.0');

    discardPeriodicTasks();
    fixture.destroy();
  }));
});
