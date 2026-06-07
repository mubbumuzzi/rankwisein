import {
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  ViewChild,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NavigationEnd, Router } from '@angular/router';
import { filter, firstValueFrom, Subscription } from 'rxjs';
import { WHATSAPP_CTA_LABEL, WHATSAPP_URL } from '../../../core/constants/site.constants';
import { ChatMessage, StudentProfile } from '../../../core/models/ai-counsellor.models';
import { AiCounsellorService } from '../../../core/services/ai-counsellor.service';
import { CheckCollegesLauncherService } from '../../../core/services/check-colleges-launcher.service';
import { ChatStructuredPanelComponent } from './chat-structured-panel.component';

@Component({
  selector: 'app-counselling-chatbot',
  imports: [FormsModule, ChatStructuredPanelComponent],
  templateUrl: './counselling-chatbot.component.html',
})
export class CounsellingChatbotComponent implements OnInit, OnDestroy {
  private readonly ai = inject(AiCounsellorService);
  private readonly launcher = inject(CheckCollegesLauncherService);
  private readonly router = inject(Router);

  @ViewChild('scrollHost') private scrollHost?: ElementRef<HTMLElement>;

  readonly visible = signal(true);
  readonly open = signal(false);
  readonly loading = signal(false);
  readonly typing = signal(false);
  readonly showProfile = signal(false);
  readonly messages = signal<ChatMessage[]>([]);
  readonly suggested = signal<string[]>([]);
  readonly profile = signal<StudentProfile>(this.emptyProfile());
  readonly showLeadCta = signal(false);

  draft = '';
  sessionId: number | null = null;
  pRank: number | null = null;
  pCategory: string | null = null;
  pGender: string | null = null;
  pLocation: string | null = null;
  pBudget: string | null = null;
  pBranches: string[] = [];
  private routerSub?: Subscription;

  readonly whatsappUrl = WHATSAPP_URL;
  readonly whatsappCtaLabel = WHATSAPP_CTA_LABEL;

  readonly categories = ['OC', 'BC-A', 'BC-B', 'BC-C', 'BC-D', 'BC-E', 'SC-I', 'SC-II', 'SC-III', 'ST', 'EWS'];
  readonly genders = [
    { value: 'BOYS', label: 'Male' },
    { value: 'GIRLS', label: 'Female' },
  ];
  readonly branchOptions = ['CSE', 'ECE', 'EEE', 'INF', 'MEC', 'CIV', 'CSM', 'AIM'];

  ngOnInit(): void {
    this.updateVisibility(this.router.url);
    this.routerSub = this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe((e) => this.updateVisibility(e.urlAfterRedirects));
  }

  ngOnDestroy(): void {
    this.routerSub?.unsubscribe();
  }

  panelPositionClass(): string {
    return this.router.url.startsWith('/results')
      ? 'bottom-24 right-4 sm:bottom-28 sm:right-6'
      : 'bottom-4 right-4 sm:bottom-6 sm:right-6';
  }

  async toggle(force?: boolean): Promise<void> {
    const next = force ?? !this.open();
    this.open.set(next);
    if (next && !this.sessionId) {
      await this.initSession();
    }
    if (next) {
      this.scrollSoon();
    }
  }

  private async initSession(): Promise<void> {
    this.loading.set(true);
    const storedId = this.ai.getStoredSessionId();
    try {
      if (storedId) {
        try {
          const session = await firstValueFrom(this.ai.getSession(storedId));
          this.applySession(session);
          const msgs = await firstValueFrom(this.ai.listMessages(storedId));
          this.messages.set(msgs);
          return;
        } catch {
          localStorage.removeItem('rw_chat_session_id');
        }
      }
      const created = await firstValueFrom(this.ai.createSession());
      this.applySession(created);
      this.ai.storeSessionId(created.sessionId);
      const msgs = await firstValueFrom(this.ai.listMessages(created.sessionId));
      this.messages.set(msgs);
    } finally {
      this.loading.set(false);
      this.scrollSoon();
    }
  }

  private applySession(session: {
    sessionId: number;
    profile: StudentProfile;
    suggestedQuestions: string[];
  }): void {
    this.sessionId = session.sessionId;
    this.profile.set(session.profile);
    this.pRank = session.profile.rank;
    this.pCategory = session.profile.category;
    this.pGender = session.profile.gender;
    this.pLocation = session.profile.preferredLocation;
    this.pBudget = session.profile.budget;
    this.pBranches = [...session.profile.preferredBranches];
    this.suggested.set(session.suggestedQuestions);
  }

  send(event?: Event): void {
    event?.preventDefault();
    const text = this.draft.trim();
    if (!text || !this.sessionId || this.typing()) {
      return;
    }

    const userMsg: ChatMessage = {
      id: Date.now(),
      role: 'USER',
      content: text,
      createdAt: new Date().toISOString(),
    };
    this.messages.update((m) => [...m, userMsg]);
    this.draft = '';
    this.typing.set(true);
    this.scrollSoon();

    this.ai.sendMessage(this.sessionId, text).subscribe({
      next: (reply) => {
        this.typing.set(false);
        this.messages.update((m) => [...m, reply]);
        if (reply.suggestedQuestions?.length) {
          this.suggested.set(reply.suggestedQuestions);
        }
        if (reply.showLeadCta) {
          this.showLeadCta.set(true);
        }
        if (reply.missingProfileFields?.length) {
          this.showProfile.set(true);
        }
        this.scrollSoon();
      },
      error: () => {
        this.typing.set(false);
        this.messages.update((m) => [
          ...m,
          {
            id: Date.now(),
            role: 'ASSISTANT',
            content: 'Sorry, something went wrong. Please try again in a moment.',
            createdAt: new Date().toISOString(),
          },
        ]);
      },
    });
  }

  pickSuggestion(q: string): void {
    this.draft = q;
    this.send();
  }

  saveProfile(): void {
    if (!this.sessionId) return;
    this.ai
      .updateProfile(this.sessionId, {
        rank: this.pRank ?? undefined,
        category: this.pCategory ?? undefined,
        gender: this.pGender ?? undefined,
        preferredBranches: this.pBranches,
        preferredLocation: this.pLocation ?? undefined,
        budget: this.pBudget ?? undefined,
      })
      .subscribe({
        next: (updated) => {
          this.profile.set({
            ...updated,
            rank: updated.rank ?? null,
            category: updated.category ?? null,
            gender: updated.gender ?? null,
            preferredLocation: updated.preferredLocation ?? null,
            budget: updated.budget ?? null,
          });
        },
      });
  }

  toggleBranch(code: string): void {
    const set = new Set(this.pBranches);
    if (set.has(code)) set.delete(code);
    else set.add(code);
    this.pBranches = [...set];
  }

  trackWhatsapp(): void {
    if (!this.sessionId) return;
    this.ai.trackEvent(this.sessionId, 'WHATSAPP_COUNSELOR_CLICK').subscribe();
    window.open(this.whatsappUrl, '_blank', 'noopener,noreferrer');
  }

  toggleProfile(): void {
    this.showProfile.update((v) => !v);
  }

  openChecker(): void {
    this.toggle(false);
    this.launcher.open();
  }

  formatTime(iso: string): string {
    try {
      return new Date(iso).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } catch {
      return '';
    }
  }

  private updateVisibility(url: string): void {
    this.visible.set(!url.startsWith('/admin'));
  }

  private emptyProfile(): StudentProfile {
    return {
      rank: null,
      category: null,
      gender: null,
      preferredBranches: [],
      preferredLocation: null,
      budget: null,
      completeForPrediction: false,
    };
  }

  private scrollSoon(): void {
    setTimeout(() => {
      const el = this.scrollHost?.nativeElement;
      if (el) el.scrollTop = el.scrollHeight;
    }, 50);
  }
}
