import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { Subscription, switchMap, timer } from 'rxjs';
import { FRONTEND_VERSION } from './version';
import { ServiceVersion, VersionService } from './version.service';

const POLL_MS = 5000;

@Component({
  selector: 'app-version-footer',
  template: `
    <footer class="container versions">
      <small>
        frontend <strong>{{ frontend }}</strong>
        @for (b of backends(); track b.service) {
          · {{ b.service }} <strong>{{ b.version }}</strong>
        }
      </small>
    </footer>
  `,
})
export class VersionFooter implements OnInit, OnDestroy {
  private versions = inject(VersionService);
  readonly frontend = FRONTEND_VERSION;
  readonly backends = signal<ServiceVersion[]>([]);
  private sub?: Subscription;

  ngOnInit(): void {
    this.sub = timer(0, POLL_MS)
      .pipe(switchMap(() => this.versions.backends()))
      .subscribe(v => this.backends.set(v));
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }
}
