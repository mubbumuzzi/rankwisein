import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse } from '../models/page.models';
import {
  ChatAdminStats,
  ChatMessage,
  ChatSessionSummary,
  StudentProfile,
} from '../models/ai-counsellor.models';

@Injectable({ providedIn: 'root' })
export class ChatAdminService {
  private readonly http = inject(HttpClient);

  stats(): Observable<ChatAdminStats> {
    return this.http.get<ChatAdminStats>(`${environment.apiBaseUrl}/admin/chat/stats`);
  }

  listSessions(params: {
    page?: number;
    size?: number;
    q?: string;
  }): Observable<PageResponse<ChatSessionSummary>> {
    const sp = new URLSearchParams();
    if (params.page != null) sp.set('page', String(params.page));
    if (params.size != null) sp.set('size', String(params.size));
    if (params.q) sp.set('q', params.q);
    const qs = sp.toString();
    return this.http.get<PageResponse<ChatSessionSummary>>(
      `${environment.apiBaseUrl}/admin/chat/sessions${qs ? '?' + qs : ''}`
    );
  }

  sessionDetail(id: number): Observable<{
    id: number;
    chatUserId: number;
    title: string;
    profile: StudentProfile;
    messages: ChatMessage[];
  }> {
    return this.http.get<{
      id: number;
      chatUserId: number;
      title: string;
      profile: StudentProfile;
      messages: ChatMessage[];
    }>(`${environment.apiBaseUrl}/admin/chat/sessions/${id}`);
  }

  exportCsv(): Observable<Blob> {
    return this.http.get(`${environment.apiBaseUrl}/admin/chat/export`, {
      responseType: 'blob',
    });
  }
}
