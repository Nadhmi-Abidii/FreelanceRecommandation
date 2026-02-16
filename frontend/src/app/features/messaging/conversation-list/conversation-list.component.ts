import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { forkJoin, map } from 'rxjs';

import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';

// navbar
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatMenuModule } from '@angular/material/menu';
import { MatBadgeModule } from '@angular/material/badge';
import { MatDividerModule } from '@angular/material/divider';

import {
  ConversationService,
  ConversationSummary,
  ConversationMessage
} from '../../../services/conversation.service';

import { AuthService } from '../../../core/auth/auth';

@Component({
  standalone: true,
  selector: 'app-conversation-list',
  templateUrl: './conversation-list.component.html',
  styleUrls: ['./conversation-list.component.scss'],
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatProgressBarModule,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatSlideToggleModule,
    MatMenuModule,
    MatBadgeModule,
    MatDividerModule,
  ]
})
export default class ConversationListComponent implements OnInit {
  private readonly service = inject(ConversationService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly auth = inject(AuthService);

  readonly conversations = signal<ConversationSummary[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  readonly selected = signal<ConversationSummary | null>(null);

  // pour savoir “moi” (comme dans conversation-detail)
  private get userId(): number | null {
    return this.auth.currentUser()?.userId ?? null;
  }

  private get userRole(): string {
    return (this.auth.currentUser()?.role ?? '').replace('ROLE_', '').toUpperCase();
  }

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading.set(true);
    this.error.set(null);

    this.service
      .listMine()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: list => {
          this.loading.set(false);
          this.conversations.set(list);

          // garder sélection si possible
          const current = this.selected();
          if (current) {
            const stillThere = list.find(c => c.id === current.id) ?? null;
            this.selected.set(stillThere);
          }

          // ⬇️ on enrichit avec les messages (nom + date + preview)
          if (list.length) {
            const requests = list.map(conv =>
              this.service.getMessages(conv.id).pipe(
                map(messages => ({ convId: conv.id, messages }))
              )
            );

            forkJoin(requests)
              .pipe(takeUntilDestroyed(this.destroyRef))
              .subscribe({
                next: results => {
                  const enriched = this.conversations().map(conv => {
                    const match = results.find(r => r.convId === conv.id);
                    if (!match || !match.messages.length) {
                      // aucune message → on garde ce qu’on a
                      return conv;
                    }

                    // on transforme les messages comme dans mapToView()
                    const views = match.messages.map(m => this.mapToView(m));

                    const last = views[views.length - 1];          // dernier message
                    const firstOther = views.find(m => !m.isMine); // premier message de l’autre

                    return {
                      ...conv,
                      // nom de l’autre personne si on le connaît
                      counterpartName: firstOther?.authorName ?? conv.counterpartName ?? null,
                      // contenu + date du dernier message
                      lastMessage: last?.content ?? conv.lastMessage ?? null,
                      lastMessageAt: last?.createdAt ?? conv.lastMessageAt ?? null,
                      // nom de l’auteur du dernier message
                      lastAuthorName: last?.authorName ?? null
                    };
                  });

                  this.conversations.set(enriched);

                  // remettre la sélection avec les données enrichies
                  const sel = this.selected();
                  if (sel) {
                    const newSel = enriched.find(c => c.id === sel.id) ?? sel;
                    this.selected.set(newSel);
                  }
                },
                error: err => {
                  console.error('Erreur lors du chargement des messages', err);
                }
              });
          }
        },
        error: err => {
          this.loading.set(false);
          const message =
            err?.error?.message || err?.message || 'Impossible de charger vos conversations.';
          this.error.set(message);
        }
      });
  }

  open(conversation: ConversationSummary) {
    if (!conversation?.id) return;
    this.router.navigate(['/messages', conversation.id], {
      state: { conversation }
    });
  }

  select(conversation: ConversationSummary) {
    if (!conversation) return;
    this.selected.set(conversation);
  }

  /**
   * Même logique que mapToView() dans ConversationDetail :
   * - détermine si le message est à moi
   * - donne un nom “Client / Freelancer” si authorName est null
   */
  private mapToView(message: ConversationMessage): { isMine: boolean; authorName: string | null; content: string; createdAt: string | null } {
    const authorRole = (message.author ?? '').toUpperCase();
    const isMine =
      (this.userId != null && message.authorId === this.userId) ||
      (!!this.userRole && this.userRole === authorRole);

    const authorName =
      message.authorName ??
      (authorRole === 'CLIENT' ? 'Client' : authorRole === 'FREELANCER' ? 'Freelancer' : null);

    return {
      isMine,
      authorName,
      content: message.content,
      createdAt: message.createdAt ?? null
    };
  }

  // Nom affiché dans la vue liste + panneau droit
  displayName(conv: ConversationSummary | null): string {
    if (!conv) return 'Interlocuteur';
    return conv.counterpartName || conv.lastAuthorName || 'Interlocuteur';
  }

  // Dark mode navbar
  toggleDark(on: boolean) {
    const root = document.documentElement;
    root.classList.toggle('dash-dark', !!on);
  }

  logout() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
