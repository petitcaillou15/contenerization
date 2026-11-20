import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, map } from 'rxjs';
import { LoginResponse } from './api.types';

const TOKEN_KEY = 'token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly _token = signal<string | null>(localStorage.getItem(TOKEN_KEY));
  readonly isLoggedIn = computed(() => this._token() !== null);

  constructor(private http: HttpClient) {}

  register(pseudo: string, email: string, password: string): Observable<void> {
    return this.http.post<void>('/api/users/register', { pseudo, email, password });
  }

  login(email: string, password: string): Observable<void> {
    return this.http.post<LoginResponse>('/api/users/login', { email, password }).pipe(
      tap(res => {
        localStorage.setItem(TOKEN_KEY, res.token);
        this._token.set(res.token);
      }),
      map(() => void 0),
    );
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    this._token.set(null);
  }

  token(): string | null {
    return this._token();
  }

  /**
   * Décode le claim `sub` du JWT SANS vérifier la signature.
   * Usage strictement UI (savoir "quel profil est le mien").
   * Le serveur ne fait jamais confiance à cette valeur.
   * Lit directement le localStorage : lecture d'affichage, jamais pour une requête.
   */
  currentUserId(): number | null {
    const t = localStorage.getItem(TOKEN_KEY);
    if (!t) return null;
    try {
      const payload = JSON.parse(atob(t.split('.')[1]));
      const id = Number(payload.sub);
      return Number.isFinite(id) ? id : null;
    } catch {
      return null;
    }
  }
}
