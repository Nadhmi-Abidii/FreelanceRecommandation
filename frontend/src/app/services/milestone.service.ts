import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map } from 'rxjs/operators';
import { environment } from '../../app/src/environments/environment';

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data?: T;
}

export type MilestoneStatus = 'PENDING' | 'IN_PROGRESS' | 'SUBMITTED' | 'VALIDATED' | 'PAID' | 'REJECTED' | string;

export interface MilestoneDeliverableDto {
  id: number;
  fileName: string;
  downloadUrl: string;
  comment?: string | null;
  uploadedBy?: string | null;
  contentType?: string | null;
  createdAt?: string | null;
}

export interface MilestoneDto {
  id?: number;
  missionId?: number | null;
  missionTitle?: string | null;
  mission?: { id?: number | null } | null;
  title: string;
  description?: string | null;
  amount: number;
  dueDate?: string | null;
  status?: MilestoneStatus | null;
  isCompleted?: boolean | null;
  completionDate?: string | null;
  completionNotes?: string | null;
  rejectionReason?: string | null;
  orderIndex?: number | null;
  paidAt?: string | null;
  deliverableFileName?: string | null;
  deliverableUrl?: string | null;
  deliverableUploadedAt?: string | null;
  deliverables?: MilestoneDeliverableDto[];
}

export interface MilestonePaymentPayload {
  clientId: number;
  freelancerId: number;
  paymentMethod?: string;
  description?: string;
}

@Injectable({ providedIn: 'root' })
export class MilestoneService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;
  private readonly apiBase = `${this.baseUrl}/api`;

  byMission(missionId: number) {
    return this.http.get<ApiResponse<MilestoneDto[]>>(`${this.baseUrl}/missions/${missionId}/milestones/ordered`);
  }

  listForMission(missionId: number) {
    // Client view for a mission
    return this.http.get<ApiResponse<MilestoneDto[]>>(`${this.apiBase}/client/missions/${missionId}/milestones`);
  }

  listForFreelancer(missionId: number) {
    return this.http.get<ApiResponse<MilestoneDto[]>>(`${this.apiBase}/freelancer/missions/${missionId}/milestones`);
  }

  create(missionId: number, dto: MilestoneDto) {
    return this.http.post<ApiResponse<MilestoneDto>>(`${this.baseUrl}/missions/${missionId}/milestones`, dto);
  }

  uploadDeliverable(milestoneId: number, file: File, comment?: string) {
    const formData = new FormData();
    formData.append('file', file);
    if (comment) {
      formData.append('comment', comment);
    }

    return this.http
      .post<MilestoneDto | ApiResponse<MilestoneDto>>(
        `${this.apiBase}/milestones/${milestoneId}/deliverable`,
        formData
      )
      .pipe(map(res => this.wrapMilestoneResponse(res, 'Livrable envoye.')));
  }

  // Kept for backward compatibility with former multi-file API; now proxies the single upload.
  uploadDeliverables(milestoneId: number, files: File[], comment?: string) {
    const firstFile = files?.[0];
    if (!firstFile) {
      throw new Error('Aucun fichier selectionne pour le jalon.');
    }
    return this.uploadDeliverable(milestoneId, firstFile, comment);
  }

  deliver(id: number, notes?: string) {
    return this.http.post<ApiResponse<MilestoneDto>>(`${this.baseUrl}/milestones/${id}/deliver`, null, {
      params: notes ? { completionNotes: notes } : {}
    });
  }

  revert(id: number) {
    return this.http.put<ApiResponse<MilestoneDto>>(`${this.baseUrl}/milestones/${id}/pending`, {});
  }

  validate(id: number, notes?: string) {
    return this.accept(id, notes);
  }

  accept(milestoneId: number, approvalNotes?: string) {
    const body = approvalNotes ? { approvalNotes } : {};
    return this.http
      .post<MilestoneDto | ApiResponse<MilestoneDto>>(
        `${this.apiBase}/milestones/${milestoneId}/accept`,
        body
      )
      .pipe(map(res => this.wrapMilestoneResponse(res, 'Jalon accepte.')));
  }

  reject(milestoneId: number, reason?: string) {
    const body = reason ? { reason } : {};
    return this.http
      .post<MilestoneDto | ApiResponse<MilestoneDto>>(
        `${this.apiBase}/milestones/${milestoneId}/reject`,
        body
      )
      .pipe(map(res => this.wrapMilestoneResponse(res, 'Jalon rejete.')));
  }

  pay(milestoneId: number, payload: MilestonePaymentPayload) {
    return this.http.post<ApiResponse<any>>(
      `${this.baseUrl}/payments/milestones/${milestoneId}/pay`,
      payload
    );
  }

  topUpClientWallet(clientId: number, amount: number) {
    return this.http.post<ApiResponse<any>>(
      `${this.baseUrl}/payments/wallets/client/${clientId}/topup`,
      null,
      { params: { amount } }
    );
  }

  private wrapMilestoneResponse(res: any, fallbackMessage: string): ApiResponse<MilestoneDto> {
    if (res && typeof res === 'object' && 'success' in res) {
      return res as ApiResponse<MilestoneDto>;
    }
    return {
      success: true,
      message: fallbackMessage,
      data: res as MilestoneDto
    };
  }
}
