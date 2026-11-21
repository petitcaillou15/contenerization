import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, forkJoin, map, of } from 'rxjs';

export interface ServiceVersion { service: string; version: string; }

const SERVICES = ['users', 'posts', 'notifications'];

/**
 * Interroge GET /version de chaque service (via le gateway).
 * Chaque appel atteint UN pod au hasard : pendant un rolling update, on voit donc
 * alterner l'ancienne et la nouvelle version.
 */
@Injectable({ providedIn: 'root' })
export class VersionService {
  private http = inject(HttpClient);

  backends(): Observable<ServiceVersion[]> {
    return forkJoin(SERVICES.map(s =>
      this.http.get<{ version: string }>(`/api/${s}/version`).pipe(
        map(r => ({ service: s, version: r.version })),
        catchError(() => of({ service: s, version: '?' })),
      ),
    ));
  }
}
