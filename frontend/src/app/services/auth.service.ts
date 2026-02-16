import { inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { environment } from '../../app/src/environments/environment' ; 
import { AuthResponse, MessageResponse } from '../models/auth.model';

const TOKEN_KEY = 'token';
const ROLE_KEY  = 'role';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private baseUrl = environment.apiUrl; // ex: http://localhost:9020

  isLoggedIn = signal<boolean>(!!globalThis.localStorage?.getItem(TOKEN_KEY));
  role = signal<string | null>(globalThis.localStorage?.getItem(ROLE_KEY) ?? null);

  login(email: string, password: string) {
    return this.http.post<MessageResponse<AuthResponse>>(
      `${this.baseUrl}/auth/login`,
      { email, password }
    );
  }

  storeSession(res: AuthResponse) {
    const token = res?.token;
    if (token) {
      localStorage.setItem(TOKEN_KEY, token);
      localStorage.setItem(ROLE_KEY, res.role || '');
      this.isLoggedIn.set(true);
      this.role.set(res.role || null);
    }
  }

  logout() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(ROLE_KEY);
    this.isLoggedIn.set(false);
    this.role.set(null);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }
}
