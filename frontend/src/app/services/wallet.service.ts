import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map } from 'rxjs';

import { environment } from '../../app/src/environments/environment';

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data?: T;
}

export type WalletTransactionType = 'CREDIT' | 'DEBIT' | 'RECHARGE' | 'PAYMENT' | 'PAYOUT' | 'WITHDRAWAL' | string;

export interface WalletTransaction {
  id: number;
  type: WalletTransactionType;
  amount: number;
  currency?: string | null;
  missionTitle?: string | null;
  milestoneTitle?: string | null;
  createdAt?: string | null;
}

export interface WalletSnapshot {
  id?: number | null;
  balance: number;
  currency: string;
  transactions: WalletTransaction[];
}

@Injectable({ providedIn: 'root' })
export class WalletService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;
  private readonly apiBase = `${this.baseUrl}/api`;

  getClientWallet() {
    return this.http
      .get<ApiResponse<any>>(`${this.apiBase}/client/wallet/me`)
      .pipe(map(res => this.mapWallet(res?.data)));
  }

  getFreelancerWallet() {
    return this.http
      .get<ApiResponse<any>>(`${this.apiBase}/freelancer/wallet/me`)
      .pipe(map(res => this.mapWallet(res?.data)));
  }

  getMyWallet(role: 'CLIENT' | 'FREELANCER' = 'CLIENT') {
    return role === 'FREELANCER' ? this.getFreelancerWallet() : this.getClientWallet();
  }

  recharge(amount: number) {
    return this.http
      .post<ApiResponse<any>>(`${this.baseUrl}/wallet/recharge`, { amount })
      .pipe(map(res => this.mapWallet(res?.data)));
  }

  private mapWallet(payload: any): WalletSnapshot {
    const wallet = payload?.wallet ?? payload ?? {};
    return {
      id: wallet?.id ?? wallet?.walletId ?? payload?.walletId ?? null,
      balance: Number(wallet?.balance ?? wallet?.solde ?? 0),
      currency: wallet?.currency ?? wallet?.devise ?? 'EUR',
      transactions: this.normalizeTransactions(wallet?.transactions ?? payload?.transactions ?? payload)
    };
  }

  private normalizeTransactions(list: any): WalletTransaction[] {
    const items = Array.isArray(list) ? list : [];
    return items.map(item => this.mapTransaction(item)).filter(tx => tx.id != null);
  }

  private mapTransaction(raw: any): WalletTransaction {
    return {
      id: Number(raw?.id ?? raw?.transactionId ?? Math.floor(Math.random() * 1_000_000)),
      type: (raw?.type ?? raw?.transactionType ?? 'PAYMENT') as WalletTransactionType,
      amount: Number(raw?.amount ?? raw?.montant ?? 0),
      currency: raw?.currency ?? raw?.devise ?? 'EUR',
      missionTitle: raw?.missionTitle ?? raw?.missionName ?? raw?.mission?.title ?? null,
      milestoneTitle: raw?.milestoneTitle ?? raw?.milestone?.title ?? null,
      createdAt: raw?.createdAt ?? raw?.date ?? raw?.timestamp ?? null
    };
  }
}
