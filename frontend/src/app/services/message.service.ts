import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../app/src/environments/environment';

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data?: T;
}

export interface MessageDto {
  id?: number;
  senderId?: number;
  receiverId?: number;
  sender?: { id?: number };
  receiver?: { id?: number };
  subject?: string | null;
  content: string;
  isRead?: boolean | null;
  messageType?: string | null;
  createdAt?: string | null;
}

@Injectable({ providedIn: 'root' })
export class MessageService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;

  create(payload: MessageDto) {
    const body = {
      sender: { id: payload.senderId },
      receiver: { id: payload.receiverId },
      subject: payload.subject ?? null,
      content: payload.content,
      messageType: payload.messageType ?? null
    };
    return this.http.post<ApiResponse<MessageDto>>(`${this.baseUrl}/messages`, body);
  }

  bySender(senderId: number) {
    return this.http.get<ApiResponse<MessageDto[]>>(`${this.baseUrl}/messages/sender/${senderId}`);
  }

  conversation(senderId: number, receiverId: number) {
    return this.http.get<ApiResponse<MessageDto[]>>(
      `${this.baseUrl}/messages/conversation`,
      { params: { senderId, receiverId } }
    );
  }

  byReceiver(receiverId: number) {
    return this.http.get<ApiResponse<MessageDto[]>>(`${this.baseUrl}/messages/receiver/${receiverId}`);
  }

  markRead(id: number) {
    return this.http.put<ApiResponse<MessageDto>>(`${this.baseUrl}/messages/${id}/read`, {});
  }
}
