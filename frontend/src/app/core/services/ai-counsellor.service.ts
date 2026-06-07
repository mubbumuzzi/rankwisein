import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ChatMessage,
  ChatSession,
  StudentProfile,
} from '../models/ai-counsellor.models';

const VISITOR_KEY = 'rw_visitor_token';
const SESSION_KEY = 'rw_chat_session_id';

@Injectable({ providedIn: 'root' })
export class AiCounsellorService {
  private readonly http = inject(HttpClient);

  getVisitorToken(): string {
    let token = localStorage.getItem(VISITOR_KEY);
    if (!token) {
      token = crypto.randomUUID();
      localStorage.setItem(VISITOR_KEY, token);
    }
    return token;
  }

  getStoredSessionId(): number | null {
    const raw = localStorage.getItem(SESSION_KEY);
    return raw ? Number(raw) : null;
  }

  storeSessionId(id: number): void {
    localStorage.setItem(SESSION_KEY, String(id));
  }

  private headers(): HttpHeaders {
    return new HttpHeaders({ 'X-Visitor-Token': this.getVisitorToken() });
  }

  createSession(): Observable<ChatSession> {
    return this.http.post<ChatSession>(
      `${environment.apiBaseUrl}/chat/sessions`,
      { visitorToken: this.getVisitorToken() }
    );
  }

  getSession(sessionId: number): Observable<ChatSession> {
    return this.http.get<ChatSession>(`${environment.apiBaseUrl}/chat/sessions/${sessionId}`, {
      headers: this.headers(),
    });
  }

  listMessages(sessionId: number): Observable<ChatMessage[]> {
    return this.http.get<ChatMessage[]>(
      `${environment.apiBaseUrl}/chat/sessions/${sessionId}/messages`,
      { headers: this.headers() }
    );
  }

  sendMessage(sessionId: number, content: string): Observable<ChatMessage> {
    return this.http.post<ChatMessage>(
      `${environment.apiBaseUrl}/chat/sessions/${sessionId}/messages`,
      { content },
      { headers: this.headers() }
    );
  }

  updateProfile(sessionId: number, profile: Partial<StudentProfile>): Observable<StudentProfile> {
    return this.http.put<StudentProfile>(
      `${environment.apiBaseUrl}/chat/sessions/${sessionId}/profile`,
      {
        rank: profile.rank ?? undefined,
        category: profile.category ?? undefined,
        gender: profile.gender ?? undefined,
        preferredBranches: profile.preferredBranches ?? undefined,
        preferredLocation: profile.preferredLocation ?? undefined,
        budget: profile.budget ?? undefined,
      },
      { headers: this.headers() }
    );
  }

  trackEvent(sessionId: number, eventType: string, metadata?: string): Observable<void> {
    return this.http.post<void>(
      `${environment.apiBaseUrl}/chat/sessions/${sessionId}/events`,
      { eventType, metadata },
      { headers: this.headers() }
    );
  }

  suggestedQuestions(): Observable<string[]> {
    return this.http.get<string[]>(`${environment.apiBaseUrl}/chat/suggested-questions`);
  }
}
