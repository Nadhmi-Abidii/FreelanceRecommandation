import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../app/src/environments/environment';

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data?: T;
}

export interface WalletDto {
  id?: number | null;
  balance?: number | null;
  currency?: string | null;
  isActive?: boolean | null;
  client?: { id?: number | null } | null;
  freelancer?: { id?: number | null } | null;
}

export interface TransactionDto {
  id?: number | null;
  amount?: number | null;
  currency?: string | null;
  status?: string | null;
  description?: string | null;
  createdAt?: string | null;
  processedAt?: string | null;
}

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;

  getClientWallet(clientId: number) {
    return this.http.get<ApiResponse<WalletDto>>(`${this.baseUrl}/payments/wallets/client/${clientId}`);
  }

  getFreelancerWallet(freelancerId: number) {
    return this.http.get<ApiResponse<WalletDto>>(`${this.baseUrl}/payments/wallets/freelancer/${freelancerId}`);
  }

  getClientTransactions(clientId: number) {
    return this.http.get<ApiResponse<TransactionDto[]>>(`${this.baseUrl}/payments/transactions/client/${clientId}`);
  }

  getFreelancerTransactions(freelancerId: number) {
    return this.http.get<ApiResponse<TransactionDto[]>>(`${this.baseUrl}/payments/transactions/freelancer/${freelancerId}`);
  }

  createFeedback(payload: any) {
    return this.http.post<ApiResponse<any>>(`${this.baseUrl}/feedbacks`, payload);
  }
}
