  import { CommonModule } from '@angular/common';
  import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
  import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
  import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
  import { ActivatedRoute } from '@angular/router';

  import { MatButtonModule } from '@angular/material/button';
  import { MatCardModule } from '@angular/material/card';
  import { MatChipsModule } from '@angular/material/chips';
  import { MatFormFieldModule } from '@angular/material/form-field';
  import { MatIconModule } from '@angular/material/icon';
  import { MatInputModule } from '@angular/material/input';
  import { MatListModule } from '@angular/material/list';
  import { MatProgressBarModule } from '@angular/material/progress-bar';
  import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
  import { MatToolbarModule } from '@angular/material/toolbar';

  import { AuthService } from '../../../core/auth/auth';
  import { MessageDto, MessageService } from '../../../services/message.service';
  import {
    CandidatureClientService,
    CandidatureMessageApiResponse
  } from '../../../services/candidature-client.service';

  interface ConversationThread {
    freelancerId: number;
    name: string;
    headline?: string | null;
    location?: string | null;
    lastMessage?: string | null;
    lastDate?: string | null;
  }

  interface TimelineMessage {
    id: number;
    author: 'client' | 'freelancer';
    content: string;
    sentAt?: string | null;
    authorLabel?: string;
  }

  interface CandidatureContext {
    candidatureId: number;
    freelancerId?: number | null;
    freelancerName?: string | null;
    missionId?: number | null;
    missionTitle?: string | null;
  }

  @Component({
    standalone: true,
    selector: 'app-client-discussions',
    templateUrl: './client-discussions.component.html',
    styleUrls: ['./client-discussions.component.scss'],
    imports: [
      CommonModule,
      ReactiveFormsModule,
      MatToolbarModule,
      MatButtonModule,
      MatCardModule,
      MatChipsModule,
      MatFormFieldModule,
      MatIconModule,
      MatInputModule,
      MatListModule,
      MatProgressBarModule,
      MatSnackBarModule
    ]
  })
  export default class ClientDiscussionsComponent implements OnInit {
    private readonly messageService = inject(MessageService);
    private readonly candidatureService = inject(CandidatureClientService);
    private readonly auth = inject(AuthService);
    private readonly fb = inject(FormBuilder);
    private readonly snackBar = inject(MatSnackBar);
    private readonly destroyRef = inject(DestroyRef);
    private readonly route = inject(ActivatedRoute);

    threads = signal<ConversationThread[]>([]);
    selectedFreelancer = signal<number | null>(null);
    conversation = signal<TimelineMessage[]>([]);
    loading = signal(false);
    sending = signal(false);
    error = signal<string | null>(null);
    candidatureContext = signal<CandidatureContext | null>(null);

    form = this.fb.nonNullable.group({
      message: ['', [Validators.required, Validators.minLength(3)]],
      subject: ['']
    });

    readonly selectedThread = computed(() =>
      this.threads().find(t => t.freelancerId === this.selectedFreelancer()) ?? null
    );

    readonly inCandidatureMode = computed(() => !!this.candidatureContext());

    ngOnInit() {
      const clientId = this.auth.currentUser()?.userId;
      if (!clientId) {
        this.error.set('Connectez-vous en tant que client pour voir vos discussions.');
        return;
      }

      this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(params => {
        const candidatureId = Number(params.get('candidatureId'));
        if (candidatureId) {
          const ctx: CandidatureContext = {
            candidatureId,
            freelancerId: Number(params.get('freelancerId')) || null,
            freelancerName: params.get('freelancerName'),
            missionId: Number(params.get('missionId')) || null,
            missionTitle: params.get('missionTitle') || undefined
          };
          this.candidatureContext.set(ctx);
          const label = ctx.freelancerName || `Freelancer #${ctx.freelancerId ?? candidatureId}`;
          this.loadCandidatureConversation(candidatureId, label);
          return;
        }

        this.candidatureContext.set(null);
        this.loadThreads(clientId);
      });
    }

    loadThreads(clientId: number) {
      this.loading.set(true);
      this.error.set(null);
      this.messageService.bySender(clientId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: res => {
          this.loading.set(false);
          const msgs = res.data ?? [];
          const grouped = new Map<number, MessageDto>();
          msgs.forEach(m => {
            const receiverId = (m as any).receiver?.id ?? m.receiverId;
            if (!receiverId) return;
            grouped.set(receiverId, m);
          });
          const threads = Array.from(grouped.values()).map(m => ({
            freelancerId: (m as any).receiver?.id ?? m.receiverId!,
            name: `Freelancer #${(m as any).receiver?.id ?? m.receiverId}`,
            lastMessage: m.content,
            lastDate: m.createdAt ?? null
          }));
          this.threads.set(threads);
          if (threads.length) {
            this.select(threads[0].freelancerId);
          }
        },
        error: err => {
          this.loading.set(false);
          this.error.set(err?.error?.message || err?.message || 'Erreur reseau.');
        }
      });
    }

    private loadCandidatureConversation(candidatureId: number, freelancerName: string) {
      this.loading.set(true);
      this.error.set(null);
      this.candidatureService
        .getMessages(candidatureId)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: res => {
            this.loading.set(false);
            const list = this.mapCandidatureMessages(res.data ?? [], freelancerName);
            this.conversation.set(list);

            const context = this.candidatureContext();
            const thread: ConversationThread = {
              freelancerId: context?.freelancerId ?? candidatureId,
              name: freelancerName,
              headline: context?.missionTitle ?? null,
              lastMessage: list[list.length - 1]?.content ?? null,
              lastDate: list[list.length - 1]?.sentAt ?? null
            };
            this.threads.set([thread]);
            this.selectedFreelancer.set(thread.freelancerId);
          },
          error: err => {
            this.loading.set(false);
            this.error.set(err?.error?.message || err?.message || 'Impossible de charger la conversation.');
            this.conversation.set([]);
          }
        });
    }

    select(freelancerId: number) {
      if (this.inCandidatureMode()) {
        return;
      }

      this.selectedFreelancer.set(freelancerId);
      const clientId = this.auth.currentUser()?.userId;
      if (!clientId) return;
      this.loading.set(true);
      this.messageService
        .conversation(clientId, freelancerId)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: res => {
            this.loading.set(false);
            const list = (res.data ?? []).map(m => this.mapMessage(m, clientId));
            this.conversation.set(list);
          },
          error: err => {
            this.loading.set(false);
            this.error.set(err?.error?.message || err?.message || 'Impossible de charger la conversation.');
          }
        });
    }

    send() {
      const clientId = this.auth.currentUser()?.userId;
      const freelancerId = this.selectedFreelancer();
      if (!clientId || (!freelancerId && !this.inCandidatureMode())) return;
      if (this.form.invalid) {
        this.form.markAllAsTouched();
        return;
      }

      const content = (this.form.controls.message.value ?? '').trim();
      if (!content) {
        this.form.controls.message.setErrors({ required: true });
        return;
      }

      const context = this.candidatureContext();
      if (context) {
        this.sending.set(true);
        this.candidatureService
          .sendMessage(context.candidatureId, content, undefined, 'CLIENT')
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: res => {
              this.sending.set(false);
              if (!res.success || !res.data) {
                this.snackBar.open(res.message || 'Envoi impossible', 'Fermer', { duration: 2200 });
                return;
              }
              const mapped = this.mapCandidatureMessages([res.data], context.freelancerName || 'Freelancer')[0];
              this.conversation.update(list => [...list, mapped]);
              this.form.reset({ message: '', subject: '' });
            },
            error: err => {
              this.sending.set(false);
              this.snackBar.open(err?.error?.message || err?.message || 'Erreur reseau', 'Fermer', { duration: 2400 });
            }
          });
        return;
      }

      this.sending.set(true);
      this.messageService
        .create({
          senderId: clientId,
          receiverId: freelancerId as number,
          subject: this.form.controls.subject.value ?? undefined,
          content
        })
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: res => {
            this.sending.set(false);
            if (!res.success || !res.data) {
              this.snackBar.open(res.message || 'Envoi impossible', 'Fermer', { duration: 2200 });
              return;
            }
            const mapped = this.mapMessage(res.data, clientId);
            this.conversation.update(list => [...list, mapped]);
            this.form.reset({ message: '', subject: '' });
          },
          error: err => {
            this.sending.set(false);
            this.snackBar.open(err?.error?.message || err?.message || 'Erreur reseau', 'Fermer', { duration: 2400 });
          }
        });
    }

    private mapMessage(msg: MessageDto, clientId: number): TimelineMessage {
      const senderId = (msg as any).sender?.id ?? msg.senderId;
      return {
        id: msg.id ?? Math.floor(Math.random() * 10_000_000),
        author: senderId === clientId ? 'client' : 'freelancer',
        content: msg.content,
        sentAt: msg.createdAt ?? null
      };
    }

    private mapCandidatureMessages(
      messages: CandidatureMessageApiResponse[],
      freelancerName: string
    ): TimelineMessage[] {
      return [...messages]
        .map(msg => {
          const author: 'client' | 'freelancer' = msg.author === 'CLIENT' ? 'client' : 'freelancer';
          return {
            id: msg.id ?? Math.floor(Math.random() * 10_000_000),
            author,
            content: msg.content ?? '',
            sentAt: msg.createdAt ?? null,
            authorLabel: author === 'client' ? 'Vous' : freelancerName
          } as TimelineMessage;
        })
        .sort(
          (a, b) => new Date(a.sentAt || '').getTime() - new Date(b.sentAt || '').getTime()
        );
    }
  }
