import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { map } from 'rxjs/operators';

import { environment } from '../../app/src/environments/environment';
import { AuthService } from '../core/auth/auth';
import { FreelancerMission, MissionMessage, MissionUrgency } from '../models/freelancer-mission.model';
import { BudgetType, NiveauExperience, TypeTravail } from '../models/mission.model';

export type MissionStatus = 'OPEN' | 'APPLIED' | 'INTERVIEWING' | 'IN_PROGRESS' | 'PENDING_CLOSURE' | 'COMPLETED';

interface ApiMessageResponse<T> {
  success: boolean;
  message: string;
  data?: T;
}

interface MissionApiResponse {
  id: number;
  clientId?: number | null;
  domaineId?: number | null;
  clientName?: string | null;
  clientCompanyName?: string | null;
  clientCity?: string | null;
  clientCountry?: string | null;
  clientProfilePicture?: string | null;
  domaineName?: string | null;
  title: string;
  description?: string | null;
  requirements?: string | null;
  budgetMin?: number | null;
  budgetMax?: number | null;
  budgetType?: BudgetType | null;
  typeTravail?: TypeTravail | null;
  niveauExperience?: NiveauExperience | null;
  status?: string | null;
  deadline?: string | null;
  estimatedDuration?: number | null;
  skillsRequired?: string | null;
  isUrgent?: boolean | null;
  attachments?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

interface MissionPageResponse {
  content: MissionApiResponse[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

type CandidatureStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'WITHDRAWN';

interface CandidatureMessageDto {
  id?: number;
  author?: 'FREELANCER' | 'CLIENT';
  content?: string | null;
  resumeUrl?: string | null;
  createdAt?: string | null;
}

interface CandidatureDto {
  id?: number;
  coverLetter?: string | null;
  resumeUrl?: string | null;
  clientMessage?: string | null;
  status?: CandidatureStatus | null;
  createdAt?: string | null;
  updatedAt?: string | null;
  messages?: CandidatureMessageDto[] | null;
  mission?: { id?: number | null };
}

interface CreateCandidaturePayload {
  freelancerId: number;
  missionId: number;
  coverLetter: string;
  proposedPrice?: number;
  proposedDuration?: number;
  resumeUrl?: string;
}

interface ApplicationSnapshot {
  missionId: number;
  candidatureId: number;
  status?: string | null;
  createdAt?: string | null;
  messages: CandidatureMessageDto[];
}

@Injectable({ providedIn: 'root' })
export class FreelancerMissionService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly apiUrl = environment.apiUrl;

  private messageCounter = 4000;
  private applicationsLoaded = false;

  private readonly _missions = signal<FreelancerMission[]>([]);
  private readonly _loading = signal(false);
  private readonly _error = signal<string | null>(null);

  readonly missions = this._missions.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();

  syncApplications(force = false) {
    this.loadApplicationsForFreelancer(force);
  }

  loadMissions(force = false) {
    if (this._loading()) {
      return;
    }

    if (!force && this._missions().length) {
      this.loadApplicationsForFreelancer();
      return;
    }

    this._loading.set(true);
    this._error.set(null);
    this.http
      .get<ApiMessageResponse<MissionApiResponse[] | MissionPageResponse>>(
        `${this.apiUrl}/missions`,
        { params: { size: 100, sort: 'createdAt,DESC' } }
      )
      .subscribe({
        next: res => {
          const missionPayloads = this.normalizeMissionPayload(res.data);
          const missions = missionPayloads.map(mission => this.mapMission(mission));
          this._missions.set(missions);
          this._loading.set(false);
          this.loadApplicationsForFreelancer(true);
        },
        error: err => {
          const message = err?.error?.message ?? 'Impossible de récupérer les missions';
          this._error.set(message);
          this._loading.set(false);
        }
      });
  }

  fetchMission(id: number): Observable<FreelancerMission | null> {
    const cached = this.getMissionById(id);
    if (cached) {
      return of(cached);
    }

    return this.http
      .get<ApiMessageResponse<MissionApiResponse>>(`${this.apiUrl}/missions/${id}`)
      .pipe(
        map(res => {
          if (!res.success || !res.data) {
            return null;
          }
          const mission = this.mapMission(res.data);
          this.upsertMission(mission);
          this.loadApplicationsForFreelancer();
          return mission;
        })
      );
  }

  completeMission(id: number) {
    return this.submitFinalDelivery(id);
  }

  submitFinalDelivery(id: number) {
    return this.http.post<ApiMessageResponse<MissionApiResponse>>(
      `${this.apiUrl}/missions/${id}/submit-final`,
      {}
    );
  }

  getMissionById(id: number): FreelancerMission | null {
    return this._missions().find(mission => mission.id === id) ?? null;
  }

  markAsApplied(id: number): FreelancerMission | null {
    let updated: FreelancerMission | null = null;
    this._missions.update(missions =>
      missions.map(mission => {
        if (mission.id !== id) {
          return mission;
        }

        const nextStatus: MissionStatus = mission.status === 'OPEN' ? 'APPLIED' : mission.status;
        updated = { ...mission, status: nextStatus, appliedAt: mission.appliedAt ?? new Date().toISOString() };
        return updated;
      })
    );

    return updated;
  }

  toggleSaved(id: number): FreelancerMission | null {
    let updated: FreelancerMission | null = null;
    this._missions.update(missions =>
      missions.map(mission => {
        if (mission.id !== id) {
          return mission;
        }

        updated = { ...mission, isSaved: !mission.isSaved };
        return updated;
      })
    );

    return updated;
  }

  sendMessage(
    id: number,
    content: string,
    resumeUrl?: string,
    options?: { proposedPrice?: number; proposedDuration?: number }
  ): Observable<FreelancerMission | null> {
    const message = content.trim();
    if (!message) {
      return of(this.getMissionById(id));
    }

    const freelancerId = this.auth.currentUser()?.userId;
    if (!freelancerId) {
      return throwError(() => new Error('Freelancer non authentifié'));
    }

    const mission = this.getMissionById(id);
    const sanitizedResume = resumeUrl?.trim() || undefined;

    if (mission?.candidatureId) {
      return this.http
        .post<ApiMessageResponse<CandidatureMessageDto>>(
          `${this.apiUrl}/candidatures/${mission.candidatureId}/messages`,
          { author: 'FREELANCER', content: message, resumeUrl: sanitizedResume }
        )
        .pipe(
          map(res => {
            if (!res.success || !res.data) {
              throw new Error(res.message || 'Impossible d\'envoyer le message');
            }
            this.appendMissionMessage(id, this.mapMessageDto(res.data));
            return this.getMissionById(id);
          })
        );
    }

    const payload: CreateCandidaturePayload = {
      freelancerId,
      missionId: id,
      coverLetter: message,
      resumeUrl: sanitizedResume,
      proposedPrice: options?.proposedPrice,
      proposedDuration: options?.proposedDuration
    };

    return this.createCandidature(payload);
  }

  getStatusLabel(status: MissionStatus): string {
    switch (status) {
      case 'OPEN':
        return 'Nouvelle mission'
      case 'APPLIED':
        return 'Candidature envoyee'
      case 'INTERVIEWING':
        return 'En entretien'
      case 'IN_PROGRESS':
        return 'Mission en cours'
      case 'PENDING_CLOSURE':
        return 'En attente de cloture client'
      case 'COMPLETED':
        return 'Mission terminee'
      default:
        return status
    }
  }

  getUrgencyLabel(urgency: MissionUrgency): string {
    return urgency === 'URGENT' ? 'Urgent' : 'Standard';
  }

  private upsertMission(mission: FreelancerMission) {
    const existing = this.getMissionById(mission.id);
    if (!existing) {
      this._missions.update(list => [...list, mission]);
      return;
    }

    this._missions.update(list => list.map(item => (item.id === mission.id ? mission : item)));
  }

  private mapMission(api: MissionApiResponse): FreelancerMission {
    const requirements = this.parseList(api.requirements);
    const skills = this.parseList(api.skillsRequired);
    const tags = this.parseTags(api.skillsRequired);

    return {
      id: api.id,
      clientId: api.clientId ?? null,
      domaineId: api.domaineId ?? null,
      backendStatus: api.status ?? null,
      clientCompanyName: api.clientCompanyName ?? null,
      clientCity: api.clientCity ?? null,
      clientCountry: api.clientCountry ?? null,
      clientProfilePicture: api.clientProfilePicture ?? null,
      title: api.title,
      clientName: api.clientName ?? api.clientCompanyName ?? 'Client confidentiel',
      domaine: api.domaineName ?? 'Général',
      summary: this.formatSummary(api.description),
      description: api.description ?? '',
      location: this.formatLocation(api.clientCity, api.clientCountry, api.typeTravail),
      budget: this.formatBudget(api.budgetMin, api.budgetMax, api.budgetType),
      postedAt: api.createdAt ?? new Date().toISOString(),
      deadline: api.deadline ?? api.createdAt ?? new Date().toISOString(),
      status: this.mapStatus(api.status),
      urgency: api.isUrgent ? 'URGENT' : 'STANDARD',
      experienceLevel: this.experienceLabel(api.niveauExperience),
      workModel: this.workModelLabel(api.typeTravail),
      duration: this.formatDuration(api.estimatedDuration),
      weeklyRhythm: this.formatWeeklyRhythm(api.typeTravail),
      deliverables: requirements.slice(0, 3),
      tags: tags.length ? tags : skills.slice(0, 5),
      requirements: requirements.length ? requirements : skills,
      tools: tags.length ? tags : ['Communication', 'Collaboration'],
      isSaved: false,
      appliedAt: null,
      conversation: []
    };
  }

  private formatSummary(description?: string | null): string {
    if (!description) {
      return 'Mission publiée par nos clients';
    }

    const clean = description.replace(/\s+/g, ' ').trim();
    return clean.length > 220 ? `${clean.slice(0, 217)}...` : clean;
  }

  private mapStatus(status?: string | null): MissionStatus {
    switch ((status ?? '').toUpperCase()) {
      case 'IN_PROGRESS':
        return 'IN_PROGRESS';
      case 'PENDING_CLOSURE':
        return 'PENDING_CLOSURE';
      case 'COMPLETED':
        return 'COMPLETED';
      case 'PUBLISHED':
      case 'DRAFT':
      case 'PAUSED':
      case 'CANCELLED':
      default:
        return 'OPEN';
    }
  }

  private normalizeMissionPayload(
    data?: MissionApiResponse[] | MissionPageResponse | null
  ): MissionApiResponse[] {
    if (!data) {
      return [];
    }
    if (Array.isArray(data)) {
      return data;
    }
    if (Array.isArray(data.content)) {
      return data.content;
    }
    return [];
  }

  private formatBudget(min?: number | null, max?: number | null, type?: BudgetType | null): string {
    if (min == null && max == null) {
      return 'Budget à définir';
    }

    const format = (value: number) =>
      new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 }).format(value);

    let range = '';
    if (min != null && max != null) {
      range = `${format(min)} - ${format(max)}`;
    } else if (min != null) {
      range = `À partir de ${format(min)}`;
    } else if (max != null) {
      range = `Jusqu'à ${format(max)}`;
    }

    switch (type) {
      case BudgetType.HOURLY:
        return `${range} / heure`;
      case BudgetType.NEGOTIABLE:
        return `${range} (négociable)`;
      default:
        return `${range} / mission`;
    }
  }

  private formatDuration(days?: number | null): string {
    if (!days) {
      return 'Durée flexible';
    }
    if (days < 30) {
      return `≈ ${days} jours`;
    }
    const months = Math.round(days / 30);
    return months <= 1 ? '≈ 1 mois' : `≈ ${months} mois`;
  }

  private formatWeeklyRhythm(type?: TypeTravail | null): string {
    switch (type) {
      case TypeTravail.REMOTE:
        return 'Mode remote';
      case TypeTravail.HYBRID:
        return 'Mode hybride';
      default:
        return 'Présence sur site';
    }
  }

  private parseList(raw?: string | null): string[] {
    if (!raw) {
      return [];
    }
    return raw
      .split(/\r?\n|•|-|\u2022/)
      .map(item => item.replace(/^[\s•-]+/, '').trim())
      .filter(Boolean);
  }

  private parseTags(raw?: string | null): string[] {
    if (!raw) {
      return [];
    }
    return raw
      .split(',')
      .map(tag => tag.trim())
      .filter(Boolean);
  }

  private formatLocation(city?: string | null, country?: string | null, type?: TypeTravail | null): string {
    const parts = [city, country].filter(Boolean);
    if (parts.length) {
      return parts.join(', ');
    }

    switch (type) {
      case TypeTravail.REMOTE:
        return 'Remote';
      case TypeTravail.HYBRID:
        return 'Hybride';
      default:
        return 'Sur site';
    }
  }

  private experienceLabel(level?: NiveauExperience | null): 'Junior' | 'Intermédiaire' | 'Senior' {
    switch (level) {
      case NiveauExperience.BEGINNER:
        return 'Junior';
      case NiveauExperience.INTERMEDIATE:
        return 'Intermédiaire';
      default:
        return 'Senior';
    }
  }

  private workModelLabel(type?: TypeTravail | null): 'Remote' | 'Hybrid' | 'On-site' {
    switch (type) {
      case TypeTravail.REMOTE:
        return 'Remote';
      case TypeTravail.HYBRID:
        return 'Hybrid';
      default:
        return 'On-site';
    }
  }

  private mapMessageDto(dto: CandidatureMessageDto): MissionMessage {
    const sentAt = dto.createdAt ?? new Date().toISOString();
    return {
      id: dto.id ?? ++this.messageCounter,
      author: dto.author === 'CLIENT' ? 'client' : 'freelancer',
      content: dto.content ?? '',
      sentAt
    };
  }

  private syncCandidature(snapshot: ApplicationSnapshot): FreelancerMission | null {
    let updated: FreelancerMission | null = null;
    const messages = snapshot.messages.map(dto => this.mapMessageDto(dto));

    this._missions.update(missions =>
      missions.map(mission => {
        if (mission.id !== snapshot.missionId) {
          return mission;
        }

        updated = {
          ...mission,
          candidatureId: snapshot.candidatureId,
          status: mission.status === 'OPEN' ? 'APPLIED' : mission.status,
          appliedAt: mission.appliedAt ?? snapshot.createdAt ?? new Date().toISOString(),
          conversation: messages.length ? messages : mission.conversation
        };
        return updated;
      })
    );

    return updated;
  }

  private appendMissionMessage(missionId: number, message: MissionMessage) {
    this._missions.update(missions =>
      missions.map(mission => {
        if (mission.id !== missionId) {
          return mission;
        }
        return {
          ...mission,
          conversation: [...mission.conversation, message].sort(
            (a, b) => new Date(a.sentAt).getTime() - new Date(b.sentAt).getTime()
          )
        };
      })
    );
  }

  private loadApplicationsForFreelancer(force = false) {
    const freelancerId = this.auth.currentUser()?.userId;
    if (!freelancerId || (this.applicationsLoaded && !force)) {
      return;
    }

    this.http
      .get<ApiMessageResponse<CandidatureDto[]>>(`${this.apiUrl}/candidatures/freelancer/${freelancerId}`)
      .subscribe({
        next: res => {
          this.applicationsLoaded = true;
          this.hydrateApplications(res.data ?? []);
        },
        error: () => {
          this.applicationsLoaded = true;
        }
      });
  }

  private hydrateApplications(candidatures: CandidatureDto[]) {
    if (!candidatures.length) {
      return;
    }

    const snapshots: ApplicationSnapshot[] = candidatures
      .filter(c => !!c.mission?.id && !!c.id)
      .map(c => this.buildSnapshot(c));

    snapshots.forEach(snapshot => this.syncCandidature(snapshot));

    candidatures.forEach(candidature => {
      const missionId = candidature.mission?.id;
      if (!missionId) {
        return;
      }

      this.updateMissionStatusFromCandidature(missionId, candidature.status);

      if (candidature.clientMessage) {
        this.appendClientMessage(
          missionId,
          candidature.clientMessage,
          candidature.updatedAt ?? candidature.createdAt ?? new Date().toISOString()
        );
      }
    });
  }

  private updateMissionStatusFromCandidature(missionId: number, status?: CandidatureStatus | null) {
    if (!status) {
      return;
    }

    let nextStatus: MissionStatus | null = null;
    switch (status) {
      case 'ACCEPTED':
        nextStatus = 'IN_PROGRESS';
        break;
      case 'PENDING':
        nextStatus = 'APPLIED';
        break;
      case 'REJECTED':
        nextStatus = 'OPEN';
        break;
      default:
        nextStatus = null;
    }

    if (!nextStatus) {
      return;
    }

    this._missions.update(missions =>
      missions.map(mission => (mission.id === missionId ? { ...mission, status: nextStatus } : mission))
    );
  }

  private appendClientMessage(missionId: number, message: string, sentAt: string) {
    const content = message.trim();
    if (!content) {
      return;
    }

    const nextMessage: MissionMessage = {
      id: ++this.messageCounter,
      author: 'client',
      content,
      sentAt
    };

    this._missions.update(missions =>
      missions.map(mission => {
        if (mission.id !== missionId) {
          return mission;
        }

        const alreadyExists = mission.conversation.some(
          existing => existing.author === 'client' && existing.content === content && existing.sentAt === sentAt
        );

        if (alreadyExists) {
          return mission;
        }

        return {
          ...mission,
          conversation: [...mission.conversation, nextMessage].sort(
            (a, b) => new Date(a.sentAt).getTime() - new Date(b.sentAt).getTime()
          )
        };
      })
    );
  }

  private buildSnapshot(c: CandidatureDto): ApplicationSnapshot {
    const messages: CandidatureMessageDto[] = (c.messages ?? []).length
      ? (c.messages as CandidatureMessageDto[])
      : [{
          author: 'FREELANCER' as const,
          content: c.coverLetter ?? '',
          resumeUrl: c.resumeUrl ?? undefined,
          createdAt: c.createdAt ?? new Date().toISOString()
        }];

    return {
      missionId: c.mission!.id!,
      candidatureId: c.id!,
      status: c.status ?? undefined,
      createdAt: c.createdAt,
      messages
    };
  }

  createCandidature(payload: CreateCandidaturePayload) {
    return this.http
      .post<ApiMessageResponse<CandidatureDto>>(`${this.apiUrl}/candidatures`, payload)
      .pipe(
        map(res => {
          if (!res.success || !res.data) {
            throw new Error(res.message || 'Impossible d\'envoyer la candidature');
          }
          const snapshot = this.buildSnapshot(res.data);
          return this.syncCandidature(snapshot);
        })
      );
  }
}
