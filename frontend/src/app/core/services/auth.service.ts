import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LoginRequest, LoginResponse } from '../models/auth.models';

const TOKEN_KEY = 'rankwise_admin_token';
const USER_KEY = 'rankwise_admin_user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly tokenSignal = signal<string | null>(this.readToken());

  readonly isLoggedIn = computed(() => !!this.tokenSignal());
  readonly token = computed(() => this.tokenSignal());

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${environment.apiBaseUrl}/admin/login`, request)
      .pipe(
        tap((res) => {
          localStorage.setItem(TOKEN_KEY, res.token);
          localStorage.setItem(USER_KEY, res.username);
          this.tokenSignal.set(res.token);
        })
      );
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this.tokenSignal.set(null);
  }

  getUsername(): string | null {
    return localStorage.getItem(USER_KEY);
  }

  private readToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }
}
