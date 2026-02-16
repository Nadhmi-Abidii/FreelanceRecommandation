import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map } from 'rxjs';

import { environment } from '../../app/src/environments/environment';
import { ApiResponse } from '../core/api/api-response.model';

export interface AiDomainSuggestionRequest {
  title?: string | null;
  description?: string | null;
  requirements?: string | null;
  skillsRequired?: string | null;
  limit?: number | null;
  language?: string | null;
}

export interface AiDomainSuggestion {
  domaineId: number;
  domaineName: string;
  score?: number | null;
  reason?: string | null;
}

export interface AiDraftRequest {
  title?: string | null;
  description?: string | null;
  requirements?: string | null;
  skillsRequired?: string | null;
  tone?: string | null;
  language?: string | null;
  maxLength?: number | null;
}

export interface AiDraftResponse {
  title?: string | null;
  description?: string | null;
  requirements?: string | null;
  skillsSuggested?: string | null;
  notes?: string | null;
}

export interface AiRewriteRequest {
  content: string;
  intent?: string | null;
  tone?: string | null;
  language?: string | null;
  maxLength?: number | null;
}

export interface AiRewriteResponse {
  content?: string | null;
  notes?: string | null;
}

export interface AiSummaryResponse {
  summary?: string | null;
  nextSteps?: string[] | null;
}

export interface AiResumeSkill {
  name: string;
  level?: string | null;
  yearsOfExperience?: number | null;
  isCertified?: boolean | null;
  certificationName?: string | null;
}

export interface AiResumeExtractionResponse {
  fileKey: string;
  summary?: string | null;
  skills?: AiResumeSkill[] | null;
  createdCount?: number | null;
}

@Injectable({ providedIn: 'root' })
export class AiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;

  draftMission(payload: AiDraftRequest) {
    return this.http
      .post<ApiResponse<AiDraftResponse>>(`${this.baseUrl}/ai/draft/mission`, payload)
      .pipe(map(res => res.data));
  }

  rewrite(payload: AiRewriteRequest) {
    return this.http
      .post<ApiResponse<AiRewriteResponse>>(`${this.baseUrl}/ai/rewrite`, payload)
      .pipe(map(res => res.data));
  }

  extractResume(file: File, freelancerId?: number, language?: string) {
    const form = new FormData();
    form.append('file', file);
    if (freelancerId != null) {
      form.append('freelancerId', String(freelancerId));
    }
    if (language) {
      form.append('language', language);
    }
    return this.http
      .post<ApiResponse<AiResumeExtractionResponse>>(`${this.baseUrl}/ai/resume/extract`, form)
      .pipe(map(res => res.data));
  }
}
