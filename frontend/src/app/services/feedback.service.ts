import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../app/src/environments/environment';

export type FeedbackDirection = 'CLIENT_TO_FREELANCER' | 'FREELANCER_TO_CLIENT';

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data?: T;
}

export interface FeedbackResponse {
  id: number;
  missionId: number;
  authorUserId: number;
  targetUserId: number;
  rating: number;
  comment?: string | null;
  createdAt?: string | null;
  direction: FeedbackDirection;
}

export interface FeedbackSummaryResponse {
  averageRating: number;
  count: number;
}

export interface FeedbackAggregateResponse {
  summary: FeedbackSummaryResponse;
  feedbacks: FeedbackResponse[];
}

export interface CreateFeedbackPayload {
  rating: number;
  comment?: string | null;
  direction?: FeedbackDirection;
}

@Injectable({ providedIn: 'root' })
export class FeedbackService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;

  createFeedback(missionId: number, payload: CreateFeedbackPayload) {
    return this.http.post<ApiResponse<FeedbackResponse>>(
      `${this.baseUrl}/missions/${missionId}/feedback`,
      payload
    );
  }

  getMyFeedback(missionId: number) {
    return this.http.get<ApiResponse<FeedbackResponse>>(
      `${this.baseUrl}/missions/${missionId}/feedbacks/mine`
    );
  }

  getUserFeedbackSummary(userId: number) {
    return this.http.get<ApiResponse<FeedbackSummaryResponse>>(
      `${this.baseUrl}/users/${userId}/feedbacks/summary`
    );
  }

  getFreelancerFeedbacks(freelancerId: number) {
    return this.http.get<ApiResponse<FeedbackAggregateResponse>>(
      `${this.baseUrl}/freelances/${freelancerId}/feedbacks`
    );
  }
}
