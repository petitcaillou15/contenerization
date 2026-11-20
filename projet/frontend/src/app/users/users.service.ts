import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PublicProfile } from '../core/api.types';

@Injectable({ providedIn: 'root' })
export class UsersService {
  private http = inject(HttpClient);

  profile(id: number): Observable<PublicProfile> {
    return this.http.get<PublicProfile>(`/api/users/users/${id}`);
  }
  updateBio(bio: string): Observable<PublicProfile> {
    return this.http.patch<PublicProfile>('/api/users/users/me', { bio });
  }
}
