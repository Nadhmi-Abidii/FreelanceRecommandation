import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatMenuModule } from '@angular/material/menu';
import { MatBadgeModule } from '@angular/material/badge';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { RouterLinkActive, Router } from '@angular/router';

import { AuthService } from '../../../core/auth/auth';
import {
  CandidatureApiResponse,
  CandidatureClientService,
  CandidatureMessageApiResponse
} from '../../../services/candidature-client.service';
import { CandidatureStatus } from '../../../models/candidature.model';

interface ApplicationCard {
  id: number;
  missionId: number | null;
  missionLabel: string;
  status: CandidatureStatus;
  statusLabel: string;
  tone: 'info' | 'success' | 'danger' | 'muted';
  coverLetter: string;
  proposedPrice?: number | null;
  proposedDuration?: number | null;
  clientMessage?: string | null;
  resumeUrl?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
  messages?: CandidatureMessageApiResponse[] | null;
  lastPreview?: string | null;
}

interface TimelineMessage {
  id: number;
  sender: 'client' | 'freelancer';
  content: string;
  resumeUrl?: string | null;
  sentAt?: string | null;
  authorLabel: string;
  isFlagged?: boolean | null;
  flagLabel?: string | null;
}

@Component({
  standalone: true,
  selector: 'app-freelancer-candidatures',
  templateUrl: './freelancer-candidatures.component.html',
  styleUrls: ['./freelancer-candidatures.component.scss'],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatDividerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatToolbarModule,
    MatMenuModule,
    MatBadgeModule,
    MatSlideToggleModule,
    MatProgressBarModule,
    MatSnackBarModule
  ]
})
export default class FreelancerCandidaturesComponent implements OnInit {
  private readonly candidatureService = inject(CandidatureClientService);
  private readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  private initialMissionId: number | null = null;

  candidatures = signal<ApplicationCard[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);
  selectedId = signal<number | null>(null);
  conversationStore = signal(new Map<number, TimelineMessage[]>());

  messageForm = this.fb.nonNullable.group({
    content: ['', [Validators.required, Validators.minLength(3)]],
    resumeUrl: ['', [Validators.pattern(/^https?:\/\/.+$/i)]]
  });

  readonly selected = computed(() =>
    this.candidatures().find(c => c.id === this.selectedId()) ?? null
  );

  readonly conversation = computed(() => {
    const id = this.selectedId();
    return id ? this.conversationStore().get(id) ?? [] : [];
  });

  ngOnInit() {
    const missionParam = Number(this.route.snapshot.queryParamMap.get('missionId'));
    if (missionParam) {
      this.initialMissionId = missionParam;
    }

    const freelancerId = this.auth.currentUser()?.userId;
    if (!freelancerId) {
      this.error.set('Connectez-vous en tant que freelancer pour voir vos candidatures.');
      return;
    }
    this.loadCandidatures(freelancerId);
  }

  reload() {
    const freelancerId = this.auth.currentUser()?.userId;
    if (!freelancerId) return;
    this.loadCandidatures(freelancerId);
  }

  select(id: number) {
    if (this.selectedId() === id) return;
    this.selectedId.set(id);
    const card = this.candidatures().find(c => c.id === id);
    if (card) this.primeConversation(card);
  }

  sendMessage() {
    const selected = this.selected();
    if (!selected) return;
    if (selected.status !== 'ACCEPTED') {
      this.snackBar.open('Messagerie active après acceptation par le client.', 'Fermer', { duration: 2000 });
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

    this.loading.set(true);
    this.candidatureService
      .sendMessage(selected.id, content, resumeUrl, 'FREELANCER')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.loading.set(false);
          if (!res.success || !res.data) {
            this.snackBar.open(res.message || 'Envoi impossible.', 'Fermer', { duration: 2200 });
            return;
          }
          const mapped = this.mapMessages([res.data], this.getClientLabel())[0];
          this.appendMessage(selected.id, mapped);
          this.messageForm.reset({ content: '', resumeUrl: '' });
          this.snackBar.open('Message envoyé au client', 'Fermer', { duration: 1800 });
        },
        error: err => {
          this.loading.set(false);
          this.snackBar.open(
            err?.error?.message || err?.message || 'Erreur réseau.',
            'Fermer',
            { duration: 2200 }
          );
        }
      });
  }

  private loadCandidatures(freelancerId: number) {
    this.loading.set(true);
    this.error.set(null);

    this.candidatureService
      .listByFreelancer(freelancerId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.loading.set(false);
          if (!res.success || !Array.isArray(res.data)) {
            this.error.set(res.message || 'Impossible de charger vos candidatures.');
            this.candidatures.set([]);
            return;
          }
          const list = res.data
            .filter(c => c.id != null)
            .map(dto => this.mapCard(dto));
          this.candidatures.set(list);
          if (list.length) {
            const targetId = this.initialMissionId != null
              ? (list.find(item => item.missionId === this.initialMissionId)?.id ?? list[0].id)
              : list[0].id;
            this.selectedId.set(targetId);
            const target = list.find(item => item.id === targetId);
            if (target) {
              this.primeConversation(target);
            }
            this.initialMissionId = null;
          } else {
            this.selectedId.set(null);
          }
        },
        error: err => {
          this.loading.set(false);
          this.error.set(err?.error?.message || err?.message || 'Erreur réseau.');
          this.candidatures.set([]);
        }
      });
  }

  private mapCard(dto: CandidatureApiResponse): ApplicationCard {
    const meta = this.statusMeta(dto.status ?? 'PENDING');
    const missionId = dto.missionId ?? null;
    const messages = dto.messages ?? [];
    const last = messages.length ? messages[messages.length - 1] : null;
    const preview = last?.content ?? dto.coverLetter ?? '';

    return {
      id: dto.id ?? 0,
      missionId,
      missionLabel: missionId ? `Mission #${missionId}` : 'Mission',
      status: dto.status ?? 'PENDING',
      statusLabel: meta.label,
      tone: meta.tone,
      coverLetter: dto.coverLetter ?? '',
      proposedPrice: dto.proposedPrice ?? null,
      proposedDuration: dto.proposedDuration ?? null,
      clientMessage: dto.clientMessage ?? null,
      resumeUrl: dto.resumeUrl ?? null,
      createdAt: dto.createdAt ?? null,
      updatedAt: dto.updatedAt ?? null,
      messages,
      lastPreview: preview ? preview.slice(0, 120) : null
    };
  }

  private primeConversation(card: ApplicationCard) {
    if (card.messages?.length) {
      this.upsertConversation(card.id, this.mapMessages(card.messages, this.getClientLabel()));
      return;
    }
    if (card.coverLetter) {
      const base: CandidatureMessageApiResponse = {
        author: 'FREELANCER',
        content: card.coverLetter,
        resumeUrl: card.resumeUrl ?? undefined,
        createdAt: card.createdAt ?? new Date().toISOString()
      };
      this.upsertConversation(card.id, this.mapMessages([base], this.getClientLabel()));
    }
  }

  private statusMeta(status: CandidatureStatus) {
    switch (status) {
      case 'ACCEPTED':
        return { label: 'Acceptée', tone: 'success' as const };
      case 'REJECTED':
        return { label: 'Refusée', tone: 'danger' as const };
      case 'WITHDRAWN':
        return { label: 'Retirée', tone: 'muted' as const };
      default:
        return { label: 'En attente', tone: 'info' as const };
    }
  }

  private mapMessages(messages: CandidatureMessageApiResponse[], clientLabel: string): TimelineMessage[] {
    return [...messages]
      .sort((a, b) => new Date(a.createdAt || '').getTime() - new Date(b.createdAt || '').getTime())
      .map(msg => ({
        id: msg.id ?? Math.floor(Math.random() * 10_000_000),
        sender: msg.author === 'CLIENT' ? 'client' : 'freelancer',
        authorLabel: msg.author === 'CLIENT' ? clientLabel : 'Vous',
        content: msg.content ?? '',
        resumeUrl: msg.resumeUrl ?? null,
        sentAt: msg.createdAt ?? undefined,
        isFlagged: msg.isFlagged ?? null,
        flagLabel: msg.flagLabel ?? null
      }));
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

  private getClientLabel() {
    return 'Client';
  }

  logout() {
  this.auth.logout();          // supprime token + reset user/role
  this.router.navigate(['/login']);
}
}
