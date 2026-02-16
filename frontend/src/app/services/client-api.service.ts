// src/app/services/client-api.service.ts
import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

// ✅ Chemin correct si ce fichier est à src/app/services/...
//   (si ton fichier est placé ailleurs, ajuste en ajoutant ou en retirant un "../")
import { environment } from '../../app/src/environments/environment';

export interface MessageResponse<T> {
  success: boolean;
  message: string;
  data?: T;
}

export interface ClientDto {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  companyName?: string;
  companySize?: string;
  industry?: string;
  website?: string;
  address?: string;
  city?: string;
  country?: string;
  postalCode?: string;
  profilePicture?: string;
  bio?: string;
  // côté backend tu utilises souvent isActive; si tu n'as pas isVerified, rends-le optionnel
  isVerified?: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface UpdateClientProfileRequest {
  firstName?: string;
  lastName?: string;
  phone?: string;
  companyName?: string;
  companySize?: string;
  industry?: string;
  website?: string;
  address?: string;
  city?: string;
  country?: string;
  postalCode?: string;
  profilePicture?: string;
  bio?: string;
}

@Injectable({ providedIn: 'root' })
export class ClientApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl; // ex: http://localhost:9020

  /* ===================== Compte connecté ===================== */

  getMe() {
    return this.http.get<MessageResponse<ClientDto>>(`${this.baseUrl}/clients/me`);
  }

  updateMyProfile(payload: UpdateClientProfileRequest) {
    return this.http.put<MessageResponse<ClientDto>>(
      `${this.baseUrl}/clients/me/profile`,
      payload
    );
  }

  /* ===================== Admin utils (optionnels) ===================== */
  // Utiles pour la page admin si tu veux manipuler les clients depuis un service

  /** Supprimer définitivement un client (admin) */
  deleteClientById(id: number) {
    return this.http.delete<MessageResponse<null>>(`${this.baseUrl}/clients/${id}`);
  }

  /** Récupérer une page de clients (admin) */
  getClients(page = 0, size = 20) {
    return this.http.get<MessageResponse<{
      content: ClientDto[];
      totalElements: number;
      totalPages: number;
      size: number;
      number: number;
    }>>(`${this.baseUrl}/clients`, { params: { page, size } as any });
  }

  /** Recherche (admin) */
  searchClients(keyword: string) {
    return this.http.get<MessageResponse<ClientDto[]>>(
      `${this.baseUrl}/clients/search`,
      { params: { keyword } as any }
    );
  }
}
