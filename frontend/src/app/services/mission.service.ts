import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { environment } from '../../app/src/environments/environment';
import { AiSummaryResponse } from './ai.service';

export interface MessageResponse<T> {
  success: boolean;
  message: string;
  data?: T;
}

export interface FreelancerMilestoneDto {
  id: number;
  title: string;
  description?: string | null;
  amount?: number | null;
  dueDate?: string | null;
  status?: string | null;
  isCompleted?: boolean | null;
  paidAt?: string | null;
}

export interface FreelancerMissionWithMilestonesDto {
  id: number;
  title: string;
  description?: string | null;
  status?: 'DRAFT' | 'PUBLISHED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED' | string | null;
  totalAmount?: number | null;
  clientName?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
  milestones: FreelancerMilestoneDto[];
}

export interface AiFreelancerMatch {
  freelancerId: number;
  freelancerName?: string | null;
  title?: string | null;
  score?: number | null;
  reason?: string | null;
}

@Injectable({ providedIn: 'root' })
export class MissionService {
  private http = inject(HttpClient);
  private baseUrl = environment.apiUrl; // ex: http://localhost:9020

  createMission(payload: any, token?: string) {
    return this.http.post<MessageResponse<any>>(
      `${this.baseUrl}/missions`,
      payload,
      token ? { headers: this.authHeaders(token) } : undefined
    );
  }

  getAllMissions() {
    return this.http.get<MessageResponse<any>>(`${this.baseUrl}/missions`);
  }

  getMissionById(id: number) {
    return this.http.get<MessageResponse<any>>(`${this.baseUrl}/missions/${id}`);
  }

  getRecommendations(id: number, limit?: number) {
    const params = limit != null ? `?limit=${encodeURIComponent(limit)}` : '';
    return this.http.get<MessageResponse<AiFreelancerMatch[]>>(
      `${this.baseUrl}/missions/${id}/recommendations${params}`
    );
  }

  getMissionSummary(id: number, language?: string) {
    const params = language ? `?language=${encodeURIComponent(language)}` : '';
    return this.http.get<MessageResponse<AiSummaryResponse>>(
      `${this.baseUrl}/missions/${id}/summary${params}`
    );
  }

  getMyMissions(token: string) {
    return this.http.get<MessageResponse<any>>(
      `${this.baseUrl}/missions/me`,
      { headers: this.authHeaders(token) }
    );
  }

  updateMission(id: number, payload: any, token: string) {
    return this.http.put<MessageResponse<any>>(
      `${this.baseUrl}/missions/${id}`,
      payload,
      { headers: this.authHeaders(token) }
    );
  }

  updateMissionStatus(id: number, status: string, token: string) {
    return this.http.put<MessageResponse<any>>(
      `${this.baseUrl}/missions/${id}/status?status=${encodeURIComponent(status)}`,
      {},
      { headers: this.authHeaders(token) }
    );
  }

  deleteMission(id: number, token: string) {
    return this.http.delete<MessageResponse<any>>(
      `${this.baseUrl}/missions/${id}`,
      { headers: this.authHeaders(token) }
    );
  }

  getFreelancerMissions() {
    return this.http.get<MessageResponse<FreelancerMissionWithMilestonesDto[]>>(
      `${this.baseUrl}/freelancer/missions`
    );
  }

  completeMission(id: number) {
    return this.submitFinalDelivery(id);
  }

  submitFinalDelivery(id: number) {
    return this.http.post<MessageResponse<any>>(
      `${this.baseUrl}/missions/${id}/submit-final`,
      {}
    );
  }

  closeMission(id: number, token?: string) {
    const options = token ? { headers: this.authHeaders(token) } : undefined;
    return this.http.post<MessageResponse<any>>(
      `${this.baseUrl}/missions/${id}/close`,
      {},
      options
    );
  }

  private authHeaders(token: string): HttpHeaders {
    return new HttpHeaders({ Authorization: `Bearer ${token}` });
  }
}
