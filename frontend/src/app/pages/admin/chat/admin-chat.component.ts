import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { debounceTime } from 'rxjs';
import { ChatAdminService } from '../../../core/services/chat-admin.service';
import {
  ChatAdminStats,
  ChatMessage,
  ChatSessionSummary,
} from '../../../core/models/ai-counsellor.models';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';

@Component({
  selector: 'app-admin-chat',
  imports: [ReactiveFormsModule, RouterLink, LoadingSpinnerComponent],
  templateUrl: './admin-chat.component.html',
})
export class AdminChatComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly chatAdmin = inject(ChatAdminService);

  readonly stats = signal<ChatAdminStats | null>(null);
  readonly sessions = signal<ChatSessionSummary[]>([]);
  readonly selectedMessages = signal<ChatMessage[]>([]);
  readonly loading = signal(false);
  readonly detailLoading = signal(false);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly errorMessage = signal<string | null>(null);

  readonly filterForm = this.fb.group({ search: [''] });

  ngOnInit(): void {
    this.loadStats();
    this.loadSessions();
    this.filterForm.valueChanges.pipe(debounceTime(350)).subscribe(() => {
      this.page.set(0);
      this.loadSessions();
    });
  }

  loadStats(): void {
    this.chatAdmin.stats().subscribe({
      next: (s) => this.stats.set(s),
      error: (e) => this.errorMessage.set(e.message ?? 'Failed to load stats'),
    });
  }

  loadSessions(): void {
    this.loading.set(true);
    this.chatAdmin
      .listSessions({ page: this.page(), size: 15, q: this.filterForm.value.search || undefined })
      .subscribe({
        next: (res) => {
          this.sessions.set(res.content);
          this.totalPages.set(res.totalPages);
          this.loading.set(false);
        },
        error: (e) => {
          this.loading.set(false);
          this.errorMessage.set(e.message ?? 'Failed to load sessions');
        },
      });
  }

  viewSession(id: number): void {
    this.detailLoading.set(true);
    this.chatAdmin.sessionDetail(id).subscribe({
      next: (d) => {
        this.selectedMessages.set(d.messages);
        this.detailLoading.set(false);
      },
      error: () => this.detailLoading.set(false),
    });
  }

  exportCsv(): void {
    this.chatAdmin.exportCsv().subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'chat-sessions.csv';
        a.click();
        URL.revokeObjectURL(url);
      },
    });
  }

  prevPage(): void {
    if (this.page() > 0) {
      this.page.update((p) => p - 1);
      this.loadSessions();
    }
  }

  nextPage(): void {
    if (this.page() < this.totalPages() - 1) {
      this.page.update((p) => p + 1);
      this.loadSessions();
    }
  }
}
