import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { forkJoin, map, Observable } from 'rxjs';

export type ManagedRole = 'ROLE_CLIENT' | 'ROLE_FREELANCER' | 'ROLE_ADMIN';
export type ManagedStatus = 'Actif' | 'Inactif';

export type ManagedUser = {
  id: number;
  name: string;
  email: string;
  role: ManagedRole;
  status: ManagedStatus;
  missions: number;
  joined: string; // ISO date
  phone?: string;
  address?: string;
  city?: string;
  country?: string;
};

type MessageResponse<T> = { success: boolean; message: string; data: T };

type Page<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
};

export type CreateUserInput = {
  firstName?: string;
  lastName?: string;
  email: string;
  role: ManagedRole;
  status: ManagedStatus;
  password: string; // <-- IMPORTANT : ajouté
};

type ClientDto = {
  id: number;
  firstName?: string;
  lastName?: string;
  email: string;
  phone?: string;
  address?: string;
  city?: string;
  country?: string;
  createdAt?: string;
  isActive?: boolean;
  missionsCount?: number;
};

type FreelancerDto = {
  id: number;
  firstName?: string;
  lastName?: string;
  email: string;
  phone?: string;
  address?: string;
  city?: string;
  country?: string;
  createdAt?: string;
  isActive?: boolean;
  missionsCount?: number;
};

type AuthResponse = {
  token: string;
  tokenType: string;
  userId: number;
  email: string;
  role: string;
  firstName?: string;
  lastName?: string;
};

@Injectable({ providedIn: 'root' })
export class UsersService {
  private http = inject(HttpClient);
  private baseUrl = 'http://localhost:9020';

  private getAuthHeaders(): HttpHeaders {
    const token =
      (globalThis.localStorage && localStorage.getItem('token')) ??
      '';
    return token ? new HttpHeaders({ Authorization: `Bearer ${token}` }) : new HttpHeaders();
  }

  private roleBasePath(role: ManagedRole) {
    if (role === 'ROLE_CLIENT') return 'clients';
    if (role === 'ROLE_FREELANCER') return 'freelancers';
    return 'users';
  }

  /* ===================== LISTES POUR TABLEAU ===================== */

  getAllClients(page = 0, size = 100): Observable<ClientDto[]> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http
      .get<MessageResponse<Page<ClientDto>>>(`${this.baseUrl}/clients`, { params, headers: this.getAuthHeaders() })
      .pipe(map(res => res.data?.content ?? []));
  }

  getAllFreelancers(page = 0, size = 100): Observable<FreelancerDto[]> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http
      .get<MessageResponse<Page<FreelancerDto>>>(`${this.baseUrl}/freelancers`, { params, headers: this.getAuthHeaders() })
      .pipe(map(res => res.data?.content ?? []));
  }

  /** Fusionne clients & freelancers en ManagedUser[] prêt pour le tableau */
  getAllUsers(): Observable<ManagedUser[]> {
    return forkJoin({
      clients: this.getAllClients(),
      freelancers: this.getAllFreelancers()
    }).pipe(
      map(({ clients, freelancers }) => {
        const mappedClients: ManagedUser[] = clients.map(c => ({
          id: c.id,
          name: [c.firstName, c.lastName].filter(Boolean).join(' ').trim() || 'Client',
          email: c.email,
          role: 'ROLE_CLIENT',
          status: c.isActive === false ? 'Inactif' : 'Actif',
          missions: c.missionsCount ?? 0,
          joined: c.createdAt ?? new Date().toISOString(),
          phone: c.phone,
          address: c.address,
          city: c.city,
          country: c.country
        }));

        const mappedFreelancers: ManagedUser[] = freelancers.map(f => ({
          id: f.id,
          name: [f.firstName, f.lastName].filter(Boolean).join(' ').trim() || 'Freelance',
          email: f.email,
          role: 'ROLE_FREELANCER',
          status: f.isActive === false ? 'Inactif' : 'Actif',
          missions: f.missionsCount ?? 0,
          joined: f.createdAt ?? new Date().toISOString(),
          phone: f.phone,
          address: f.address,
          city: f.city,
          country: f.country
        }));

        return [...mappedClients, ...mappedFreelancers];
      })
    );
  }

  /* ===================== CRÉATION (popup) ===================== */

  /** Utilise /auth/register/... pour avoir password encodé et création correcte */
  createUser(input: CreateUserInput): Observable<ManagedUser> {
    const headers = this.getAuthHeaders();
    const firstName = input.firstName ?? '';
    const lastName = input.lastName ?? '';

    if (!input.password) {
      throw new Error('Password is required');
    }

    if (input.role === 'ROLE_CLIENT') {
      const body = {
        firstName,
        lastName,
        email: input.email,
        password: input.password,
        phone: null,
        address: null,
        city: null
      };
      return this.http
        .post<MessageResponse<AuthResponse>>(`${this.baseUrl}/auth/register/client`, body, { headers })
        .pipe(map(res => this.toManagedUserFromAuth(res.data, input)));
    }

    if (input.role === 'ROLE_FREELANCER') {
      const body = {
        firstName,
        lastName,
        email: input.email,
        password: input.password,
        phone: null,
        hourlyRate: 0,
        availability: 'Disponible',
        address: null
      };
      return this.http
        .post<MessageResponse<AuthResponse>>(`${this.baseUrl}/auth/register/freelancer`, body, { headers })
        .pipe(map(res => this.toManagedUserFromAuth(res.data, input)));
    }

    // ROLE_ADMIN
    const body = {
      firstName,
      lastName,
      email: input.email,
      password: input.password,
      phone: null,
      address: null
    };
    return this.http
      .post<MessageResponse<AuthResponse>>(`${this.baseUrl}/auth/register/admin`, body, { headers })
      .pipe(map(res => this.toManagedUserFromAuth(res.data, input)));
  }

  private toManagedUserFromAuth(data: AuthResponse | undefined, input: CreateUserInput): ManagedUser {
    const nameFromAuth = [data?.firstName, data?.lastName].filter(Boolean).join(' ').trim();
    const nameFromInput = [input.firstName, input.lastName].filter(Boolean).join(' ').trim();

    return {
      id: data?.userId ?? Date.now(),
      name: nameFromAuth || nameFromInput || (input.role === 'ROLE_CLIENT'
        ? 'Client'
        : input.role === 'ROLE_FREELANCER'
          ? 'Freelance'
          : 'Administrateur'),
      email: data?.email ?? input.email,
      role: input.role,
      status: input.status,
      missions: 0,
      joined: new Date().toISOString()
    };
  }
}
