import { inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map } from 'rxjs';

export interface AuthResponse {
  token: string;
  tokenType: string; // "Bearer"
  userId: number;
  email: string;
  role: 'ROLE_CLIENT' | 'ROLE_FREELANCER' | 'ROLE_ADMIN';
  firstName: string;
  lastName: string;
}

interface MessageResponse<T> {
  success: boolean;
  message: string;
  data?: T;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private baseUrl = 'http://localhost:9020'; // change si besoin

  private readonly storage = globalThis as { localStorage?: Storage; sessionStorage?: Storage };
  private readonly tokenKey = 'token';
  private readonly roleKey = 'role';
  private readonly userKey = 'user';

  private loadStoredUser(): AuthResponse | null {
    const raw = this.storage.localStorage?.getItem(this.userKey)
      ?? this.storage.sessionStorage?.getItem(this.userKey);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as AuthResponse;
    } catch {
      return null;
    }
  }

  private loadStoredRole(): AuthResponse['role'] | null {
    return (
      this.storage.localStorage?.getItem(this.roleKey)
        ?? this.storage.sessionStorage?.getItem(this.roleKey)
        ?? null
    ) as AuthResponse['role'] | null;
  }

  private getStoredToken(): string | null {
    return (
      this.storage.localStorage?.getItem(this.tokenKey)
        ?? this.storage.sessionStorage?.getItem(this.tokenKey)
        ?? null
    );
  }

  private readonly storedUser = this.loadStoredUser();
  private readonly storedRole = this.storedUser?.role ?? this.loadStoredRole();

  isLoggedIn = signal<boolean>(!!this.getStoredToken());
  currentUser = signal<AuthResponse | null>(this.storedUser);
  role = signal<AuthResponse['role'] | null>(this.storedRole);

  login(email: string, password: string) {
    return this.http.post<MessageResponse<AuthResponse>>(
      `${this.baseUrl}/auth/login`,
      { email, password }
    ).pipe(
      map(res => {
        if (!res.success || !res.data) throw new Error(res.message || 'Login failed');
        return res.data;
      })
    );
  }

  persistSession(payload: AuthResponse, remember = true) {
    const primary = remember ? this.storage.localStorage : this.storage.sessionStorage;
    const secondary = remember ? this.storage.sessionStorage : this.storage.localStorage;

    primary?.setItem(this.tokenKey, payload.token);
    primary?.setItem(this.roleKey, payload.role);
    primary?.setItem(this.userKey, JSON.stringify(payload));

    secondary?.removeItem(this.tokenKey);
    secondary?.removeItem(this.roleKey);
    secondary?.removeItem(this.userKey);

    this.isLoggedIn.set(true);
    this.currentUser.set(payload);
    this.role.set(payload.role);
  }

  logout() {
    this.storage.localStorage?.removeItem(this.tokenKey);
    this.storage.localStorage?.removeItem(this.roleKey);
    this.storage.localStorage?.removeItem(this.userKey);
    this.storage.sessionStorage?.removeItem(this.tokenKey);
    this.storage.sessionStorage?.removeItem(this.roleKey);
    this.storage.sessionStorage?.removeItem(this.userKey);

    this.isLoggedIn.set(false);
    this.currentUser.set(null);
    this.role.set(null);
  }

  register(payload: any, type: 'client' | 'freelancer' = 'client') {
    const url = type === 'client'
      ? `${this.baseUrl}/auth/register/client`
      : `${this.baseUrl}/auth/register/freelancer`;

    return this.http.post<MessageResponse<AuthResponse>>(url, payload).pipe(
      map(res => {
        if (!res.success || !res.data) throw new Error(res.message || 'Registration failed');
        return res.data;
      })
    );
  }

  getToken(): string | null {
    return this.getStoredToken();
  }
}
