import { CommonModule } from '@angular/common';
import {
  Component,
  DestroyRef,
  OnInit,
  ViewChild,
  ElementRef,
  computed,
  inject,
  signal
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { interval } from 'rxjs';

import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import {
  ConversationMessage,
  ConversationService,
  ConversationSummary
} from '../../../services/conversation.service';
import { AiSummaryResponse } from '../../../services/ai.service';
import { AuthService } from '../../../core/auth/auth';

interface MessageView extends ConversationMessage {
  isMine: boolean;
}

@Component({
  standalone: true,
  selector: 'app-conversation-detail',
  templateUrl: './conversation-detail.component.html',
  styleUrls: ['./conversation-detail.component.scss'],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
    MatSnackBarModule
  ]
})
export default class ConversationDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly service = inject(ConversationService);
  private readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);

  @ViewChild('chatBody') chatBody?: ElementRef<HTMLElement>;

  readonly conversationId = signal<number | null>(null);
  readonly header = signal<ConversationSummary | null>(null);
  readonly messages = signal<MessageView[]>([]);
  readonly loading = signal(false);
  readonly sending = signal(false);
  readonly error = signal<string | null>(null);
  readonly summary = signal<AiSummaryResponse | null>(null);
  readonly summaryLoading = signal(false);
  readonly summaryError = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    message: ['', [Validators.required, Validators.minLength(2)]]
  });

  private pollingStarted = false;

  readonly userId = computed(() => this.auth.currentUser()?.userId ?? null);
  readonly userRole = computed(() => (this.auth.currentUser()?.role ?? '').replace('ROLE_', ''));

  ngOnInit() {
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(params => {
      const id = Number(params.get('conversationId'));
      this.conversationId.set(id || null);
      if (!id) {
        this.error.set('Conversation introuvable.');
        return;
      }
      this.hydrateHeaderFromState(id);
      this.loadMessages(id);
      this.startPolling(id);
      if (!this.header()) {
        this.fetchHeader(id);
      }
    });
  }

  send() {
    const conversationId = this.conversationId();
    if (!conversationId) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const content = (this.form.controls.message.value || '').trim();
    if (!content) {
      this.form.controls.message.setErrors({ required: true });
      return;
    }

    this.sending.set(true);
    this.service
      .sendMessage(conversationId, content)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: message => {
          this.sending.set(false);
          if (!message) {
            this.snackBar.open('Envoi impossible pour le moment.', 'Fermer', { duration: 2200 });
            return;
          }
          const mapped = this.mapToView(message);
          this.messages.update(list => [...list, mapped]);
          this.form.reset({ message: '' });
          this.scrollToBottom();
        },
        error: err => {
          this.sending.set(false);
          const text = err?.error?.message || err?.message || 'Erreur lors de l’envoi.';
          this.snackBar.open(text, 'Fermer', { duration: 2600 });
        }
      });
  }

  loadSummary() {
    const conversationId = this.conversationId();
    if (!conversationId || this.summaryLoading()) {
      return;
    }
    this.summaryLoading.set(true);
    this.summaryError.set(null);
    this.service
      .getSummary(conversationId, 'fr')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: data => {
          this.summaryLoading.set(false);
          this.summary.set(data);
          if (!data?.summary) {
            this.summaryError.set('Resume indisponible pour le moment.');
          }
        },
        error: err => {
          this.summaryLoading.set(false);
          this.summaryError.set(err?.error?.message || err?.message || 'Erreur lors du resume.');
        }
      });
  }

  private loadMessages(conversationId: number, silent = false) {
    if (!silent) {
      this.loading.set(true);
    }
    this.service
      .getMessages(conversationId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: list => {
          this.loading.set(false);
          this.error.set(null);
          this.messages.set(list.map(message => this.mapToView(message)));
          this.scrollToBottom();
        },
        error: err => {
          this.loading.set(false);
          const message =
            err?.error?.message || err?.message || 'Impossible de charger les messages.';
          this.error.set(message);
        }
      });
  }

  private mapToView(message: ConversationMessage): MessageView {
    const userId = this.userId();
    const role = this.userRole().toUpperCase();
    const author = (message.author ?? '').toUpperCase();
    const isMine = (userId != null && message.authorId === userId) || (!!role && role === author);
    return {
      ...message,
      isMine,
      authorName: message.authorName ?? (author === 'CLIENT' ? 'Client' : 'Freelancer')
    };
  }

  private hydrateHeaderFromState(id: number) {
    const state = history.state as { conversation?: ConversationSummary };
    if (state?.conversation && state.conversation.id === id) {
      this.header.set(state.conversation);
    }
  }

  private fetchHeader(id: number) {
    this.service
      .listMine()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: list => {
          const found = list.find(item => item.id === id) ?? null;
          if (found) {
            this.header.set(found);
          }
        }
      });
  }

  private startPolling(conversationId: number) {
    if (this.pollingStarted) {
      return;
    }
    this.pollingStarted = true;
    interval(7000)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        const currentId = this.conversationId();
        if (currentId) {
          this.loadMessages(currentId, true);
        }
      });
  }

  private scrollToBottom() {
    queueMicrotask(() => {
      const el = this.chatBody?.nativeElement;
      if (el) {
        el.scrollTop = el.scrollHeight;
      }
    });
  }
}
