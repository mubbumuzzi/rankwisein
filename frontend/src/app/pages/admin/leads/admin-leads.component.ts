import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { RouterLink } from '@angular/router';
import { debounceTime } from 'rxjs';
import { LeadService } from '../../../core/services/lead.service';
import { LeadResponse } from '../../../core/models/lead.models';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';

@Component({
  selector: 'app-admin-leads',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    LoadingSpinnerComponent,
  ],
  templateUrl: './admin-leads.component.html',
})
export class AdminLeadsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly leadService = inject(LeadService);

  readonly leads = signal<LeadResponse[]>([]);
  readonly loading = signal(false);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly errorMessage = signal<string | null>(null);

  readonly filterForm = this.fb.group({
    search: [''],
    category: [''],
    gender: [''],
  });

  ngOnInit(): void {
    this.load();
    this.filterForm.valueChanges.pipe(debounceTime(350)).subscribe(() => {
      this.page.set(0);
      this.load();
    });
  }

  load(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    const v = this.filterForm.getRawValue();
    this.leadService
      .listAdmin({
        page: this.page(),
        size: 20,
        search: v.search || undefined,
        category: v.category || undefined,
        gender: v.gender || undefined,
      })
      .subscribe({
        next: (res) => {
          this.leads.set(res.content);
          this.totalPages.set(res.totalPages);
          this.totalElements.set(res.totalElements);
          this.loading.set(false);
        },
        error: (e) => {
          this.loading.set(false);
          this.errorMessage.set(e.message ?? 'Failed to load leads');
        },
      });
  }

  prevPage(): void {
    if (this.page() > 0) {
      this.page.update((p) => p - 1);
      this.load();
    }
  }

  nextPage(): void {
    if (this.page() < this.totalPages() - 1) {
      this.page.update((p) => p + 1);
      this.load();
    }
  }

  exportCsv(): void {
    const v = this.filterForm.getRawValue();
    this.leadService
      .exportCsv({
        search: v.search || undefined,
        category: v.category || undefined,
        gender: v.gender || undefined,
      })
      .subscribe({
        next: (blob) => {
          const url = URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = 'rankwise-leads.csv';
          a.click();
          URL.revokeObjectURL(url);
        },
        error: (e) => {
          this.errorMessage.set(e.message ?? 'Export failed');
        },
      });
  }

  formatDate(iso: string): string {
    try {
      return new Date(iso).toLocaleString();
    } catch {
      return iso;
    }
  }
}
