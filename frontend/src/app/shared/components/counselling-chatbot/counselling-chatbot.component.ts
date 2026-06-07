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
import { WHATSAPP_CTA_LABEL } from '../../../core/constants/site.constants';
import { WhatsappCounsellingLinkService } from '../../../core/services/whatsapp-counselling-link.service';
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
  private readonly whatsappLink = inject(WhatsappCounsellingLinkService);

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
    } catch {
      this.errorMessage.set('Could not start chat. Please refresh the page and try again.');
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

    const messageCountBefore = this.messages().length;

    this.ai.sendMessage(this.sessionId, text).subscribe({
      next: (reply) => {
        if (reply.pending) {
          this.pollForReply(this.sessionId!, messageCountBefore + 1);
          return;
        }
        this.typing.set(false);
        this.messages.update((m) => [...m, reply]);
        this.applyReplyMeta(reply);
        this.scrollSoon();
      },
      error: (e: { message?: string }) => {
        this.typing.set(false);
        this.messages.update((m) => m.slice(0, -1));
        this.errorMessage.set(e.message ?? 'Sorry, something went wrong. Please try again in a moment.');
      },
    });
  }

  private pollForReply(sessionId: number, minMessages: number, attempt = 0): void {
    if (attempt > 180) {
      this.typing.set(false);
      this.errorMessage.set('Reply is taking longer than expected. Please try again.');
      return;
    }

    this.ai.listMessages(sessionId).subscribe({
      next: (msgs) => {
        if (msgs.length >= minMessages) {
          const last = msgs[msgs.length - 1];
          if (last.role === 'ASSISTANT' && last.content.trim()) {
            this.typing.set(false);
            this.messages.set(msgs);
            this.applyReplyMeta(last);
            this.scrollSoon();
            return;
          }
        }
        setTimeout(() => this.pollForReply(sessionId, minMessages, attempt + 1), 1000);
      },
      error: (e: { message?: string }) => {
        this.typing.set(false);
        this.errorMessage.set(e.message ?? 'Failed to load reply. Please try again.');
      },
    });
  }

  private applyReplyMeta(reply: ChatMessage): void {
    if (reply.suggestedQuestions?.length) {
      this.suggested.set(reply.suggestedQuestions);
    } else {
      this.ai.suggestedQuestions().subscribe({
        next: (q) => this.suggested.set(q),
      });
    }
    if (reply.showLeadCta) {
      this.showLeadCta.set(true);
    }
    if (reply.missingProfileFields?.length) {
      this.showProfile.set(true);
    }
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
    window.open(this.whatsappLink.url(), '_blank', 'noopener,noreferrer');
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
