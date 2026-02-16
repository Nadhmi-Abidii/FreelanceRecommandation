import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map } from 'rxjs';

import { environment } from '../../app/src/environments/environment';
import { AiSummaryResponse } from './ai.service';

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data?: T;
}

export interface ConversationSummary {
  id: number;
  missionTitle?: string | null;
  counterpartName?: string | null;   // nom de l’autre personne si dispo
  lastMessage?: string | null;
  lastMessageAt?: string | null;
  lastAuthorName?: string | null;    // 👈 auteur du dernier message
}

export interface ConversationMessage {
  id: number;
  author?: 'CLIENT' | 'FREELANCER' | 'SYSTEM' | null;
  authorId?: number | null;
  authorName?: string | null;
  content: string;
  createdAt?: string | null;
  isFlagged?: boolean | null;
  flagScore?: number | null;
  flagLabel?: string | null;
  flagReason?: string | null;
}

@Injectable({ providedIn: 'root' })
export class ConversationService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;

  listMine() {
    return this.http
      .get<ApiResponse<ConversationSummary[] | { content?: ConversationSummary[] }>>(
        `${this.baseUrl}/conversations/mine`
      )
      .pipe(map(res => this.normalizeConversations(res?.data)));
  }

  getMessages(conversationId: number) {
    return this.http
      .get<ApiResponse<ConversationMessage[]>>(
        `${this.baseUrl}/conversations/${conversationId}/messages`
      )
      .pipe(
        map(res =>
          (res?.data ?? [])
            .map(msg => this.mapMessage(msg))
            .filter(Boolean) as ConversationMessage[]
        )
      );
  }

  sendMessage(conversationId: number, content: string) {
    return this.http
      .post<ApiResponse<ConversationMessage>>(
        `${this.baseUrl}/conversations/${conversationId}/messages`,
        { content }
      )
      .pipe(map(res => (res?.data ? this.mapMessage(res.data) : null)));
  }

  getSummary(conversationId: number, language = 'fr') {
    const params = language ? `?language=${encodeURIComponent(language)}` : '';
    return this.http
      .get<ApiResponse<AiSummaryResponse>>(
        `${this.baseUrl}/conversations/${conversationId}/summary${params}`
      )
      .pipe(map(res => res?.data ?? null));
  }

  // ----------------- private helpers -----------------

  private normalizeConversations(payload: any): ConversationSummary[] {
    const list: any[] = Array.isArray(payload)
      ? payload
      : Array.isArray(payload?.content)
      ? payload.content
      : [];

    return list
      .map(item => this.mapConversation(item))
      .filter(conv => conv.id != null) as ConversationSummary[];
  }

  private mapConversation(raw: any): ConversationSummary {
    return {
      id: Number(raw?.id ?? raw?.conversationId ?? raw?.conversation?.id ?? 0),
      missionTitle: raw?.missionTitle ?? raw?.mission?.title ?? raw?.missionName ?? null,
      counterpartName:
        raw?.counterpartName ??
        raw?.otherUserName ??
        raw?.freelancerName ??
        raw?.clientName ??
        raw?.receiverName ??
        null,
      lastMessage: raw?.lastMessage ?? raw?.lastMessageContent ?? raw?.preview ?? null,
      lastMessageAt: raw?.lastMessageAt ?? raw?.updatedAt ?? raw?.lastMessageDate ?? null,
      // 👇 essaye plusieurs noms possibles que l’API peut renvoyer
      lastAuthorName:
        raw?.lastAuthorName ??
        raw?.lastSenderName ??
        raw?.lastMessageAuthorName ??
        raw?.lastMessageSenderName ??
        raw?.senderName ??
        null
    };
  }

  private mapMessage(raw: any): ConversationMessage {
    return {
      id: Number(raw?.id ?? raw?.messageId ?? Math.floor(Math.random() * 1_000_000)),
      author:
        (raw?.author as ConversationMessage['author']) ??
        (raw?.senderRole as ConversationMessage['author']) ??
        (raw?.senderType as ConversationMessage['author']) ??
        null,
      authorId: raw?.authorId ?? raw?.senderId ?? raw?.sender?.id ?? null,
      authorName: raw?.authorName ?? raw?.senderName ?? raw?.sender?.name ?? null,
      content: raw?.content ?? raw?.message ?? '',
      createdAt: raw?.createdAt ?? raw?.sentAt ?? raw?.timestamp ?? null,
      isFlagged: raw?.isFlagged ?? raw?.flagged ?? null,
      flagScore: raw?.flagScore ?? null,
      flagLabel: raw?.flagLabel ?? null,
      flagReason: raw?.flagReason ?? null
    };
  }
}
