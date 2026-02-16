import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';

import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatMenuModule } from '@angular/material/menu';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatBadgeModule } from '@angular/material/badge';
import { MissionFeedbackButtonComponent } from '../../feedback/mission-feedback-button/mission-feedback-button.component';

import { AuthService } from '../../../core/auth/auth';
import { AiFreelancerMatch, MissionService } from '../../../services/mission.service';
import {
  CandidatureApiResponse,
  CandidatureClientService,
  CandidatureMessageApiResponse
} from '../../../services/candidature-client.service';
import { CandidatureStatus } from '../../../models/candidature.model';

interface ClientMission {
  id: number;
  title: string;
  status: string;
  statusLabel: string;
  statusClass: string;
  deadlineLabel: string;
  domaine: string;
  isUrgent: boolean;
  candidaturesCount: number;
}

interface CandidateView {
  id: number;
  freelancerName: string;
  headline: string;
  location: string;
  freelancerId?: number | null;
  resumeUrl?: string;
  coverLetter: string;
  clientMessage?: string | null;
  status: CandidatureStatus;
  statusLabel: string;
  statusTone: string;
  createdAt?: string | null;
  updatedAt?: string | null;
  email?: string | null;
  phone?: string | null;
}

@Component({
  standalone: true,
  selector: 'app-client-candidatures',
  templateUrl: './client-candidatures.component.html',
  styleUrls: ['./client-candidatures.component.scss'],
  imports: [
    CommonModule,
    ReactiveFormsModule,

    RouterLink,
    RouterLinkActive,

    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatDividerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatToolbarModule,
    MatMenuModule,
    MatSlideToggleModule,
    MatBadgeModule,
    MissionFeedbackButtonComponent
  ]
})
export default class ClientCandidaturesComponent implements OnInit {
  private readonly missionService = inject(MissionService);
  private readonly candidatureService = inject(CandidatureClientService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  readonly missions = signal<ClientMission[]>([]);
  readonly missionsLoading = signal(false);
  readonly missionsError = signal<string | null>(null);

  readonly selectedMissionId = signal<number | null>(null);
  readonly selectedCandidateId = signal<number | null>(null);

  private readonly candidatureStore = signal(new Map<number, CandidateView[]>());
  private readonly recommendationStore = signal(new Map<number, AiFreelancerMatch[]>());
  readonly candidaturesLoading = signal(false);
  readonly candidaturesError = signal<string | null>(null);
  readonly actionLoading = signal(false);
  readonly recommendationsLoading = signal(false);
  readonly recommendationsError = signal<string | null>(null);

  readonly messageForm = this.fb.nonNullable.group({
    message: ['', [Validators.minLength(4)]]
  });

  readonly selectedMission = computed(() => {
    const missionId = this.selectedMissionId();
    if (!missionId) {
      return null;
    }
    return this.missions().find(mission => mission.id === missionId) ?? null;
  });

  readonly candidatesForMission = computed(() => {
    const missionId = this.selectedMissionId();
    if (!missionId) {
      return [];
    }
    return this.candidatureStore().get(missionId) ?? [];
  });

  readonly selectedCandidate = computed(() => {
    const candidateId = this.selectedCandidateId();
    if (!candidateId) {
      return null;
    }
    return this.candidatesForMission().find(candidate => candidate.id === candidateId) ?? null;
  });

  readonly recommendations = computed(() => {
    const missionId = this.selectedMissionId();
    if (!missionId) {
      return [];
    }
    return this.recommendationStore().get(missionId) ?? [];
  });

  readonly recommendationCards = computed(() =>
    this.recommendations().map(rec => this.mapRecommendation(rec))
  );

  ngOnInit() {
    this.loadMissions();
  }

  loadMissions() {
    const token = this.auth.getToken();
    if (!token) {
      this.missionsError.set('Connectez-vous en tant que client pour suivre vos candidatures.');
      return;
    }

    this.missionsLoading.set(true);
    this.missionsError.set(null);

    this.missionService
      .getMyMissions(token)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.missionsLoading.set(false);
          if (!res.success) {
            this.missionsError.set(res.message || 'Impossible de récupérer vos missions.');
            this.missions.set([]);
            return;
          }
          const payload = res.data;
          const list: any[] = Array.isArray(payload)
            ? payload
            : Array.isArray(payload?.content)
            ? payload.content
            : [];
          const missions = list
            .map(mission => this.mapMission(mission))
            .filter(mission => mission.id != null);
          this.missions.set(missions);

          if (missions.length) {
            this.selectMission(missions[0].id);
          }
        },
        error: err => {
          this.missionsLoading.set(false);
          this.missionsError.set(
            err?.error?.message || err?.message || 'Erreur réseau lors du chargement.'
          );
          this.missions.set([]);
        }
      });
  }

  selectMission(missionId: number) {
    if (this.selectedMissionId() === missionId) {
      return;
    }
    this.selectedMissionId.set(missionId);
    this.selectedCandidateId.set(null);
    this.ensureCandidatures(missionId);
    this.ensureRecommendations(missionId);
  }

  selectCandidate(candidateId: number) {
    this.selectedCandidateId.set(candidateId);
    this.messageForm.reset();
  }

  refreshMission() {
    const missionId = this.selectedMissionId();
    if (!missionId) {
      return;
    }
    this.ensureCandidatures(missionId, true);
    this.ensureRecommendations(missionId, true);
  }

  goToMilestones(candidate?: CandidateView | null) {
    const missionId = this.selectedMissionId();
    if (!missionId) {
      this.snackBar.open('Selectionnez une mission pour voir les jalons.', 'Fermer', {
        duration: 2200
      });
      return;
    }

    const queryParams: { freelancerId?: number } = {};
    if (candidate?.freelancerId) {
      queryParams.freelancerId = candidate.freelancerId;
    }

    this.router.navigate(['/missions', missionId, 'milestones'], { queryParams });
  }

  openDiscussion(candidate: CandidateView) {
    if (!candidate?.id) {
      return;
    }

    const mission = this.selectedMission();
    const queryParams: {
      candidatureId: number;
      freelancerName: string;
      freelancerId?: number;
      missionId?: number;
      missionTitle?: string;
    } = {
      candidatureId: candidate.id,
      freelancerName: candidate.freelancerName
    };

    if (candidate.freelancerId) {
      queryParams.freelancerId = candidate.freelancerId;
    }
    if (mission?.id) {
      queryParams.missionId = mission.id;
    }
    if (mission?.title) {
      queryParams.missionTitle = mission.title;
    }

    this.router.navigate(['/messages'], { queryParams });
  }

  sendAction(action: 'message' | CandidatureStatus) {
    const candidate = this.selectedCandidate();
    if (!candidate || this.actionLoading()) {
      return;
    }

    const rawMessage = (this.messageForm.controls.message.value ?? '').trim();
    if (action === 'message' && !rawMessage) {
      this.messageForm.controls.message.setErrors({ required: true });
      return;
    }

    const targetStatus = action === 'message' ? candidate.status : action;
    this.actionLoading.set(true);

    this.candidatureService
      .updateStatus(candidate.id, targetStatus, rawMessage || undefined)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.actionLoading.set(false);
          if (!res.success || !res.data) {
            this.snackBar.open(
              res.message || 'Impossible de mettre à jour la candidature.',
              'Fermer',
              { duration: 2800 }
            );
            return;
          }

          const mapped = this.mapCandidature(res.data);
          this.upsertCandidate(mapped);
          this.messageForm.reset();

          const toastMessage = this.buildToastMessage(action, candidate.freelancerName);
          this.snackBar.open(toastMessage, 'Fermer', { duration: 2400 });

          if (action === 'ACCEPTED') {
            this.handleMissionAccepted();
          }
        },
        error: err => {
          this.actionLoading.set(false);
          this.snackBar.open(
            err?.error?.message || err?.message || 'Action impossible actuellement.',
            'Fermer',
            { duration: 2800 }
          );
        }
      });
  }

  private ensureCandidatures(missionId: number, force = false) {
    if (!force && this.candidatureStore().has(missionId)) {
      const stored = this.candidatureStore().get(missionId) ?? [];
      if (stored.length) {
        this.selectedCandidateId.set(stored[0].id);
      }
      return;
    }

    this.candidaturesLoading.set(true);
    this.candidaturesError.set(null);

    this.candidatureService
      .listByMission(missionId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.candidaturesLoading.set(false);
          if (!res.success) {
            this.candidaturesError.set(
              res.message || 'Impossible de charger les candidatures.'
            );
            this.setCandidatures(missionId, []);
            return;
          }

          const list = (res.data ?? [])
            .filter(candidature => candidature.id != null)
            .map(candidature => this.mapCandidature(candidature));
          this.setCandidatures(missionId, list);
          this.updateMissionCounter(missionId, list.length);
          this.selectedCandidateId.set(list[0]?.id ?? null);
        },
        error: err => {
          this.candidaturesLoading.set(false);
          this.candidaturesError.set(
            err?.error?.message || err?.message || 'Erreur réseau.'
          );
          this.setCandidatures(missionId, []);
        }
      });
  }

  private ensureRecommendations(missionId: number, force = false) {
    if (!force && this.recommendationStore().has(missionId)) {
      return;
    }

    this.recommendationsLoading.set(true);
    this.recommendationsError.set(null);

    this.missionService
      .getRecommendations(missionId, 5)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.recommendationsLoading.set(false);
          if (!res?.success) {
            this.recommendationsError.set(
              res?.message || 'Impossible de charger les suggestions.'
            );
            this.setRecommendations(missionId, []);
            return;
          }
          this.setRecommendations(missionId, res.data ?? []);
        },
        error: err => {
          this.recommendationsLoading.set(false);
          this.recommendationsError.set(
            err?.error?.message || err?.message || 'Impossible de charger les suggestions.'
          );
          this.setRecommendations(missionId, []);
        }
      });
  }

  private mapMission(payload: any): ClientMission {
    const status = String(payload?.status ?? 'DRAFT').toUpperCase();
    const statusMeta = this.missionStatusMeta(status);
    const deadline = payload?.deadline ?? payload?.createdAt ?? null;

    return {
      id: payload?.id ?? payload?.missionId ?? 0,
      title: payload?.title ?? 'Mission sans titre',
      status,
      statusLabel: statusMeta.label,
      statusClass: statusMeta.className,
      deadlineLabel: deadline ? this.formatDeadline(deadline) : 'Pas de date limite',
      domaine: payload?.domaineName ?? 'Général',
      isUrgent: !!payload?.isUrgent,
      candidaturesCount: payload?.candidaturesCount ?? 0
    };
  }

  private mapCandidature(payload: CandidatureApiResponse): CandidateView {
    const candidateId = payload.id ?? 0;
    const freelancer = payload.freelancer ?? {};
    const status = payload.status ?? 'PENDING';
    const location = [freelancer.city, freelancer.country].filter(Boolean).join(', ');
    const statusMeta = this.candidateStatusMeta(status);

    return {
      id: candidateId,
      freelancerId: freelancer.id ?? null,
      freelancerName: this.composeName(freelancer.firstName, freelancer.lastName),
      headline: freelancer.title ?? 'Freelancer',
      location: location || 'Localisation non renseignée',
      resumeUrl: payload.resumeUrl ?? undefined,
      coverLetter: payload.coverLetter ?? '',
      clientMessage: payload.clientMessage ?? null,
      status,
      statusLabel: statusMeta.label,
      statusTone: statusMeta.className,
      createdAt: payload.createdAt ?? null,
      updatedAt: payload.updatedAt ?? null,
      email: freelancer.email ?? null,
      phone: freelancer.phone ?? null
    };
  }

  private setCandidatures(missionId: number, list: CandidateView[]) {
    this.candidatureStore.update(store => {
      const next = new Map(store);
      next.set(missionId, list);
      return next;
    });
  }

  private setRecommendations(missionId: number, list: AiFreelancerMatch[]) {
    this.recommendationStore.update(store => {
      const next = new Map(store);
      next.set(missionId, list);
      return next;
    });
  }

  private mapRecommendation(rec: AiFreelancerMatch) {
    const rawScore = rec.score;
    const score = rawScore == null ? null : Math.max(0, Math.min(1, rawScore));
    const scorePercent = score == null ? null : Math.round(score * 100);

    let badgeLabel = 'A verifier';
    let badgeTone: 'high' | 'mid' | 'low' = 'mid';

    if (score != null) {
      if (score >= 0.07) {
        badgeLabel = 'Recommande';
        badgeTone = 'high';
      } else if (score >= 0.04) {
        badgeLabel = 'Pertinent';
        badgeTone = 'mid';
      } else {
        badgeLabel = 'Faible match';
        badgeTone = 'low';
      }
    }

    return {
      ...rec,
      scorePercent,
      badgeLabel,
      badgeTone
    };
  }

  private upsertCandidate(candidate: CandidateView) {
    const missionId = this.selectedMissionId();
    if (!missionId) {
      return;
    }

    this.candidatureStore.update(store => {
      const next = new Map(store);
      const current = [...(next.get(missionId) ?? [])];
      const index = current.findIndex(item => item.id === candidate.id);
      if (index >= 0) {
        current[index] = candidate;
      } else {
        current.unshift(candidate);
      }
      next.set(missionId, current);
      return next;
    });
  }

  private updateMissionCounter(missionId: number, value: number) {
    this.missions.update(list =>
      list.map(mission =>
        mission.id === missionId ? { ...mission, candidaturesCount: value } : mission
      )
    );
  }

  trackMission(_: number, mission: ClientMission) {
    return mission.id;
  }

  trackCandidate(_: number, candidate: CandidateView) {
    return candidate.id;
  }

  private composeName(firstName?: string | null, lastName?: string | null) {
    const parts = [firstName, lastName].filter(Boolean);
    return parts.length ? parts.join(' ') : 'Freelancer';
  }

  private formatDeadline(isoDate: string): string {
    const date = new Date(isoDate);
    return date.toLocaleDateString('fr-FR', { day: '2-digit', month: 'short' });
  }

  private missionStatusMeta(status: string) {
    switch (status) {
      case 'PUBLISHED':
        return { label: 'En ligne', className: 'mission-chip--live' };
      case 'IN_PROGRESS':
        return { label: 'En cours', className: 'mission-chip--progress' };
      case 'COMPLETED':
        return { label: 'Terminée', className: 'mission-chip--done' };
      case 'CANCELLED':
        return { label: 'Annulée', className: 'mission-chip--cancelled' };
      default:
        return { label: 'Brouillon', className: 'mission-chip--draft' };
    }
  }

  private candidateStatusMeta(status: CandidatureStatus) {
    switch (status) {
      case 'ACCEPTED':
        return { label: 'Acceptée', className: 'candidate-chip--accepted' };
      case 'REJECTED':
        return { label: 'Refusée', className: 'candidate-chip--rejected' };
      case 'WITHDRAWN':
        return { label: 'Retirée', className: 'candidate-chip--withdrawn' };
      default:
        return { label: 'En attente', className: 'candidate-chip--pending' };
    }
  }

  private buildToastMessage(action: 'message' | CandidatureStatus, freelancerName: string) {
    switch (action) {
      case 'ACCEPTED':
        return `${freelancerName} a été accepté pour la mission.`;
      case 'REJECTED':
        return `${freelancerName} a été informé de votre décision.`;
      case 'WITHDRAWN':
        return `${freelancerName} a été marqué comme retiré.`;
      default:
        return `Message envoyé à ${freelancerName}.`;
    }
  }

  private refreshMissionState(missionId: number, status: string) {
    const meta = this.missionStatusMeta(status);
    this.missions.update(list =>
      list.map(mission =>
        mission.id === missionId
          ? { ...mission, status, statusLabel: meta.label, statusClass: meta.className }
          : mission
      )
    );
  }

  private handleMissionAccepted() {
    const missionId = this.selectedMissionId();
    if (!missionId) {
      return;
    }

    const token = this.auth.getToken();
    if (!token) {
      this.snackBar.open(
        'Impossible de mettre à jour la mission sans authentification.',
        'Fermer',
        { duration: 2600 }
      );
      return;
    }

    this.missionService.updateMissionStatus(missionId, 'IN_PROGRESS', token).subscribe({
      next: () => {
        this.refreshMissionState(missionId, 'IN_PROGRESS');
      },
      error: err => {
        const message =
          err?.error?.message || err?.message || 'Impossible de verrouiller la mission.';
        this.snackBar.open(message, 'Fermer', { duration: 2800 });
      }
    });
  }

  toggleDark(enabled: boolean) {
    document.documentElement.classList.toggle('client-dark-mode', enabled);
  }
}
