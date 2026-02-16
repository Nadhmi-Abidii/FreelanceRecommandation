import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../app/src/environments/environment';

export interface MessageResponse<T> {
  success: boolean;
  message: string;
  data?: T;
}

export interface PaidMilestoneItem {
  milestoneId: number;
  missionTitle: string | null;
  milestoneTitle: string | null;
  amount: number | null;
  paidAt: string | null;
}

export interface PortfolioPayload {
  totalAmountEarned: number;
  paidMilestones: PaidMilestoneItem[];
}

@Injectable({ providedIn: 'root' })
export class PortfolioService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api`;

  getPortfolio() {
    return this.http.get<MessageResponse<PortfolioPayload>>(`${this.baseUrl}/freelancer/portfolio`);
  }
}
