import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { environment } from '../../app/src/environments/environment';
import { Candidature, CandidatureMessage, CandidatureStatus } from '../models/candidature.model';

interface MessageResponse<T> {
  success: boolean;
  message: string;
  data?: T;
}

@Injectable({ providedIn: 'root' })
export class CandidatureClientService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;

  listByMission(missionId: number) {
    return this.http.get<MessageResponse<Candidature[]>>(
      `${this.baseUrl}/candidatures/mission/${missionId}`
    );
  }

  listByFreelancer(freelancerId: number) {
    return this.http.get<MessageResponse<Candidature[]>>(
      `${this.baseUrl}/candidatures/freelancer/${freelancerId}`
    );
  }

  getMessages(candidatureId: number) {
    return this.http.get<MessageResponse<CandidatureMessage[]>>(
      `${this.baseUrl}/candidatures/${candidatureId}/messages`
    );
  }

  sendMessage(candidatureId: number, content: string, resumeUrl?: string, author: 'CLIENT' | 'FREELANCER' = 'CLIENT') {
    return this.http.post<MessageResponse<CandidatureMessage>>(
      `${this.baseUrl}/candidatures/${candidatureId}/messages`,
      { author, content, resumeUrl }
    );
  }

  updateStatus(id: number, status: CandidatureStatus, clientMessage?: string) {
    let params = new HttpParams().set('status', status);
    if (clientMessage) {
      params = params.set('clientMessage', clientMessage);
    }

    return this.http.put<MessageResponse<Candidature>>(
      `${this.baseUrl}/candidatures/${id}/status`,
      {},
      { params }
    );
  }
}

export type CandidatureApiResponse = Candidature;
export type CandidatureMessageApiResponse = CandidatureMessage;
export type { CandidatureStatus };
