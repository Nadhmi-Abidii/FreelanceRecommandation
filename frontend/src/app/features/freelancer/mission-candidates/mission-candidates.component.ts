import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

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

import {
  CandidatureApiResponse,
  CandidatureClientService,
  CandidatureMessageApiResponse
} from '../../../services/candidature-client.service';
import { CandidatureStatus } from '../../../models/candidature.model';
import { AiFreelancerMatch, MissionService } from '../../../services/mission.service';

interface MissionHeader {
  id: number;
  title: string;
  domaine: string;
  statusLabel: string;
  budgetLabel: string;
  deadlineLabel: string;
}

interface CandidateCard {
  id: number;
  name: string;
  title: string;
  location: string;
  status: CandidatureStatus;
  statusLabel: string;
  tone: 'info' | 'success' | 'danger' | 'muted';
  resumeUrl?: string | null;
  email?: string | null;
  phone?: string | null;
  submittedAt?: string | null;
  coverLetter?: string | null;
  proposedPrice?: number | null;
  proposedDuration?: number | null;
  messages?: CandidatureMessageApiResponse[] | null;
  lastMessagePreview?: string | null;
}

interface TimelineMessage {
  id: number;
  sender: 'client' | 'freelancer';
  authorLabel: string;
  content: string;
  resumeUrl?: string | null;
  sentAt?: string | null;
  isFlagged?: boolean | null;
  flagLabel?: string | null;
}

@Component({
  standalone: true,
  selector: 'app-mission-candidates',
  templateUrl: './mission-candidates.component.html',
  styleUrls: ['./mission-candidates.component.scss'],
  imports: [
    CommonModule,
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatDividerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
    MatSnackBarModule,
    MatTooltipModule
  ]
})
export default class MissionCandidatesComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly missionService = inject(MissionService);
  private readonly candidatureService = inject(CandidatureClientService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  mission = signal<MissionHeader | null>(null);
  candidates = signal<CandidateCard[]>([]);
  selectedId = signal<number | null>(null);
  conversationStore = signal(new Map<number, TimelineMessage[]>());
  loadingCandidates = signal(false);
  loadingConversation = signal(false);
  error = signal<string | null>(null);
  recommendations = signal<AiFreelancerMatch[]>([]);
  loadingRecommendations = signal(false);
  recommendationsError = signal<string | null>(null);
  recommendationCards = computed(() =>
    this.recommendations().map(rec => this.mapRecommendation(rec))
  );

  messageForm = this.fb.nonNullable.group({
    content: ['', [Validators.required, Validators.minLength(3)]],
    resumeUrl: ['', [Validators.pattern(/^https?:\/\/.+$/i)]]
  });

  readonly selectedCandidate = computed(() =>
    this.candidates().find(c => c.id === this.selectedId()) ?? null
  );

  readonly conversation = computed(() => {
    const id = this.selectedId();
    return id ? this.conversationStore().get(id) ?? [] : [];
  });

  ngOnInit() {
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(params => {
      const missionId = Number(params.get('missionId') ?? params.get('id'));
      if (!missionId) {
        this.error.set('Aucune mission selectionnee.');
        return;
      }
      this.loadMission(missionId);
      this.loadCandidates(missionId);
      this.loadRecommendations(missionId);
    });
  }

  reloadCurrent() {
    const id = this.mission()?.id;
    if (!id) return;
    this.loadMission(id);
    this.loadCandidates(id);
  }

  reloadConversation() {
    const candidate = this.selectedCandidate();
    if (!candidate) return;
    this.ensureConversation(candidate.id, candidate.name, true);
  }

  selectCandidate(id: number) {
    if (this.selectedId() === id) return;
    const candidate = this.candidates().find(c => c.id === id);
    this.selectedId.set(id);
    if (candidate) {
      this.ensureConversation(id, candidate.name);
    }
  }

  sendMessage() {
    const candidate = this.selectedCandidate();
    if (!candidate) return;
    if (candidate.status !== 'ACCEPTED') {
      this.snackBar.open('Messagerie active après acceptation de la candidature.', 'Fermer', { duration: 2000 });
      return;
    }

    if (this.messageForm.invalid) {
      this.messageForm.markAllAsTouched();
      return;
    }

    const content = (this.messageForm.controls.content.value ?? '').trim();
    const resumeUrl = (this.messageForm.controls.resumeUrl.value ?? '').trim() || undefined;
    if (!content) {
      this.messageForm.controls.content.setErrors({ required: true });
      return;
    }

    this.loadingConversation.set(true);
    this.candidatureService
      .sendMessage(candidate.id, content, resumeUrl, 'CLIENT')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.loadingConversation.set(false);
          if (!res.success || !res.data) {
            this.snackBar.open(res.message || 'Envoi impossible', 'Fermer', { duration: 2200 });
            return;
          }
          const mapped = this.mapMessages([res.data], candidate.name)[0];
          this.appendMessage(candidate.id, mapped);
          this.messageForm.reset({ content: '', resumeUrl: '' });
          this.snackBar.open('Message envoye', 'Fermer', { duration: 1800 });
        },
        error: err => {
          this.loadingConversation.set(false);
          this.snackBar.open(
            err?.error?.message || err?.message || 'Erreur reseau',
            'Fermer',
            { duration: 2400 }
          );
        }
      });
  }

  private loadMission(id: number) {
    this.missionService
      .getMissionById(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          if (res.success && res.data) {
            this.mission.set(this.mapMission(res.data));
          }
        },
        error: () => { /* silent */ }
      });
  }

  private loadCandidates(missionId: number) {
    this.loadingCandidates.set(true);
    this.error.set(null);

    this.candidatureService
      .listByMission(missionId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.loadingCandidates.set(false);
          if (!res.success || !Array.isArray(res.data)) {
            this.candidates.set([]);
            this.error.set(res.message || 'Impossible de charger les candidatures.');
            return;
          }
          const list = res.data.map(c => this.mapCandidate(c));
          this.candidates.set(list);
          if (list.length) {
            this.selectedId.set(list[0].id);
            this.primeConversation(list[0]);
          } else {
            this.selectedId.set(null);
          }
        },
        error: err => {
          this.loadingCandidates.set(false);
          this.candidates.set([]);
          this.error.set(err?.error?.message || err?.message || 'Erreur reseau.');
        }
      });
  }

  private loadRecommendations(missionId: number) {
    this.loadingRecommendations.set(true);
    this.recommendationsError.set(null);

    this.missionService
      .getRecommendations(missionId, 5)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.loadingRecommendations.set(false);
          if (!res?.success) {
            this.recommendations.set([]);
            this.recommendationsError.set(res?.message || 'Unable to load recommendations.');
            return;
          }
          this.recommendations.set(res.data ?? []);
        },
        error: err => {
          this.loadingRecommendations.set(false);
          this.recommendations.set([]);
          this.recommendationsError.set(err?.error?.message || err?.message || 'Unable to load recommendations.');
        }
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

  private ensureConversation(candidatureId: number, freelancerName: string, force = false) {
    const cached = this.conversationStore().get(candidatureId);
    if (cached && !force) return;

    this.loadingConversation.set(true);
    this.candidatureService
      .getMessages(candidatureId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.loadingConversation.set(false);
          if (!res.success || !res.data) return;
          this.upsertConversation(candidatureId, this.mapMessages(res.data, freelancerName));
        },
        error: () => this.loadingConversation.set(false)
      });
  }

  private primeConversation(candidate: CandidateCard) {
    if (candidate.messages?.length) {
      this.upsertConversation(candidate.id, this.mapMessages(candidate.messages, candidate.name));
      return;
    }
    if (candidate.coverLetter) {
      const base: CandidatureMessageApiResponse = {
        author: 'FREELANCER',
        content: candidate.coverLetter,
        resumeUrl: candidate.resumeUrl ?? undefined,
        createdAt: candidate.submittedAt ?? new Date().toISOString()
      };
      this.upsertConversation(candidate.id, this.mapMessages([base], candidate.name));
    }
  }

  private upsertConversation(candidatureId: number, messages: TimelineMessage[]) {
    this.conversationStore.update(store => {
      const next = new Map(store);
      next.set(candidatureId, messages);
      return next;
    });
  }

  private appendMessage(candidatureId: number, message: TimelineMessage) {
    this.conversationStore.update(store => {
      const next = new Map(store);
      const existing = next.get(candidatureId) ?? [];
      const merged = [...existing, message].sort(
        (a, b) => new Date(a.sentAt || '').getTime() - new Date(b.sentAt || '').getTime()
      );
      next.set(candidatureId, merged);
      return next;
    });
  }

  private mapMission(payload: any): MissionHeader {
    const deadline = payload?.deadline ?? payload?.createdAt ?? new Date().toISOString();
    return {
      id: payload?.id ?? payload?.missionId ?? 0,
      title: payload?.title ?? 'Mission',
      domaine: payload?.domaineName ?? 'General',
      statusLabel: (payload?.status ?? 'DRAFT').toString().toUpperCase(),
      budgetLabel: this.formatBudget(payload?.budgetMin, payload?.budgetMax),
      deadlineLabel: new Date(deadline).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short' })
    };
  }

  private mapCandidate(payload: CandidatureApiResponse): CandidateCard {
    const freelancer = payload.freelancer ?? {};
    const statusMeta = this.candidateStatusMeta(payload.status ?? 'PENDING');
    const messages = payload.messages ?? null;
    const lastMessage = messages && messages.length ? messages[messages.length - 1] : null;
    const preview = lastMessage?.content ?? payload.coverLetter ?? null;

    return {
      id: payload.id ?? 0,
      name: this.composeName(freelancer.firstName, freelancer.lastName),
      title: freelancer.title ?? 'Freelancer',
      location: [freelancer.city, freelancer.country].filter(Boolean).join(', ') || 'Localisation inconnue',
      status: payload.status ?? 'PENDING',
      statusLabel: statusMeta.label,
      tone: statusMeta.tone,
      resumeUrl: payload.resumeUrl ?? null,
      email: freelancer.email ?? null,
      phone: freelancer.phone ?? null,
      submittedAt: payload.createdAt ?? null,
      coverLetter: payload.coverLetter ?? null,
      proposedPrice: payload.proposedPrice ?? null,
      proposedDuration: payload.proposedDuration ?? null,
      messages,
      lastMessagePreview: preview ? preview.slice(0, 120) : null
    };
  }

  private mapMessages(messages: CandidatureMessageApiResponse[] | null | undefined, freelancerName: string): TimelineMessage[] {
    if (!messages?.length) return [];
    return [...messages]
      .sort((a, b) => new Date(a.createdAt || '').getTime() - new Date(b.createdAt || '').getTime())
      .map(msg => ({
        id: msg.id ?? Math.floor(Math.random() * 10_000_000),
        sender: msg.author === 'CLIENT' ? 'client' : 'freelancer',
        authorLabel: msg.author === 'CLIENT' ? 'Vous' : freelancerName,
        content: msg.content ?? '',
        resumeUrl: msg.resumeUrl ?? null,
        sentAt: msg.createdAt ?? undefined,
        isFlagged: msg.isFlagged ?? null,
        flagLabel: msg.flagLabel ?? null
      }));
  }

  private candidateStatusMeta(status: CandidatureStatus) {
    switch (status) {
      case 'ACCEPTED':
        return { label: 'Acceptee', tone: 'success' as const };
      case 'REJECTED':
        return { label: 'Refusee', tone: 'danger' as const };
      case 'WITHDRAWN':
        return { label: 'Retiree', tone: 'muted' as const };
      default:
        return { label: 'En attente', tone: 'info' as const };
    }
  }

  private composeName(first?: string | null, last?: string | null) {
    const parts = [first, last].filter(Boolean);
    return parts.length ? parts.join(' ') : 'Freelancer';
  }

  private formatBudget(min?: number | null, max?: number | null) {
    if (min == null && max == null) return 'Budget a definir';
    const fmt = (v: number) => new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'EUR',
      maximumFractionDigits: 0
    }).format(v);
    if (min != null && max != null) return `${fmt(min)} - ${fmt(max)}`;
    if (min != null) return `A partir de ${fmt(min)}`;
    return `Jusqu'a ${fmt(max as number)}`;
  }
}
