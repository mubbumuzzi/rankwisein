import { DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { AuthService } from '../../../core/services/auth.service';
import { ImportService } from '../../../core/services/import.service';
import { MetaService } from '../../../core/services/meta.service';
import { MetaResponse } from '../../../core/models/meta.models';
import {
  ApproveImportResponse,
  ImportStagingRow,
  ImportUploadResponse,
  PurgeCutoffsResponse,
} from '../../../core/models/import.models';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';

@Component({
  selector: 'app-admin-import',
  imports: [
    RouterLink,
    DecimalPipe,
    MatButtonModule,
    MatFormFieldModule,
    MatSelectModule,
    MatProgressBarModule,
    LoadingSpinnerComponent,
  ],
  templateUrl: './admin-import.component.html',
})
export class AdminImportComponent implements OnInit {
  readonly auth = inject(AuthService);
  private readonly importService = inject(ImportService);
  private readonly metaService = inject(MetaService);
  private readonly router = inject(Router);

  readonly meta = signal<MetaResponse | null>(null);
  readonly metaLoading = signal(true);

  readonly selectedFile = signal<File | null>(null);
  readonly year = signal(2025);
  readonly phase = signal('PHASE_1');

  readonly uploading = signal(false);
  readonly approving = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly uploadResult = signal<ImportUploadResponse | null>(null);
  readonly approveResult = signal<ApproveImportResponse | null>(null);
  readonly purgeResult = signal<PurgeCutoffsResponse | null>(null);

  readonly stagingRows = signal<ImportStagingRow[]>([]);
  readonly stagingPage = signal(0);
  readonly stagingTotalPages = signal(0);
  readonly stagingTotalElements = signal(0);
  readonly stagingLoading = signal(false);

  readonly filterInvalid = signal(false);
  readonly filterDuplicate = signal(false);

  ngOnInit(): void {
    this.metaService.getMeta().subscribe({
      next: (m) => {
        this.meta.set(m);
        if (m.years.includes(2025)) {
          this.year.set(2025);
        } else if (m.years.length) {
          this.year.set(m.years[m.years.length - 1]);
        }
        this.metaLoading.set(false);
      },
      error: () => this.metaLoading.set(false),
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.selectedFile.set(file);
    this.errorMessage.set(null);
  }

  upload(): void {
    const file = this.selectedFile();
    if (!file) {
      this.errorMessage.set('Please choose a PDF file.');
      return;
    }
    if (!file.name.toLowerCase().endsWith('.pdf')) {
      this.errorMessage.set('Only PDF files are supported.');
      return;
    }

    this.uploading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.approveResult.set(null);

    this.importService.uploadPdf(file, this.year(), this.phase()).subscribe({
      next: (res) => {
        this.uploadResult.set(res);
        this.uploading.set(false);
        this.successMessage.set(
          `Parsed ${res.totalParsed} rows — ${res.validRows} valid, ${res.duplicateRows} duplicates, ${res.invalidRows} invalid.`
        );
        this.purgeResult.set(null);
        this.loadStaging(0);
      },
      error: (e) => {
        this.uploading.set(false);
        this.errorMessage.set(e.message ?? 'Upload failed');
      },
    });
  }

  loadStaging(page: number): void {
    const importId = this.uploadResult()?.importId;
    if (!importId) return;

    this.stagingLoading.set(true);
    this.stagingPage.set(page);
    this.importService.getStaging(importId, page, 50).subscribe({
      next: (p) => {
        this.stagingRows.set(p.content);
        this.stagingTotalPages.set(p.totalPages);
        this.stagingTotalElements.set(p.totalElements);
        this.stagingLoading.set(false);
      },
      error: (e) => {
        this.stagingLoading.set(false);
        this.errorMessage.set(e.message ?? 'Failed to load preview');
      },
    });
  }

  filteredRows(): ImportStagingRow[] {
    let rows = this.stagingRows();
    if (this.filterInvalid()) {
      rows = rows.filter((r) => !r.valid);
    }
    if (this.filterDuplicate()) {
      rows = rows.filter((r) => r.duplicate);
    }
    return rows;
  }

  deleteRow(row: ImportStagingRow): void {
    const importId = this.uploadResult()?.importId;
    if (!importId) return;

    this.importService.deleteStagingRow(importId, row.id).subscribe({
      next: () => this.loadStaging(this.stagingPage()),
      error: (e) => this.errorMessage.set(e.message ?? 'Delete failed'),
    });
  }

  approve(): void {
    const importId = this.uploadResult()?.importId;
    if (!importId) return;

    if (!confirm('Approve import and insert valid rows into cutoff table?')) {
      return;
    }

    this.approving.set(true);
    this.errorMessage.set(null);
    this.importService.approve(importId).subscribe({
      next: (res) => {
        this.approveResult.set(res);
        this.approving.set(false);
        // Keep UI status in sync so the Approve button disables after success.
        this.uploadResult.update((u) => (u ? { ...u, status: res.status } : u));
        this.successMessage.set(
          `Import complete — ${res.inserted} inserted, ${res.skippedDuplicates} duplicates skipped in ${res.durationMs}ms.`
        );
      },
      error: (e) => {
        this.approving.set(false);
        const msg = (e?.message ?? 'Approve failed') as string;
        this.errorMessage.set(msg);
        // If backend says it's already IMPORTED (or other status), reflect it to disable Approve.
        const m = msg.match(/Current:\s*([A-Z_]+)/);
        if (m?.[1]) {
          this.uploadResult.update((u) => (u ? { ...u, status: m[1] } : u));
        }
      },
    });
  }

  purgeCutoffs(): void {
    const year = this.year();
    const phase = this.phase();
    const msg =
      `This will DELETE all cutoffs for ${year} / ${this.formatPhase(phase)}.\n\n` +
      `Use only if you selected the wrong phase while uploading.\n\nProceed?`;
    if (!confirm(msg)) return;

    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.purgeResult.set(null);

    this.importService.purgeCutoffs(year, phase).subscribe({
      next: (res) => {
        this.purgeResult.set(res);
        this.successMessage.set(
          `Rollback complete — deleted ${res.deleted} cutoff rows for ${res.year} / ${this.formatPhase(res.phase)}.`
        );
      },
      error: (e) => {
        this.errorMessage.set(e.message ?? 'Rollback failed');
      },
    });
  }

  logout(): void {
    this.auth.logout();
    void this.router.navigate(['/admin/login']);
  }

  formatPhase(phase: string): string {
    return phase.replace(/_/g, ' ');
  }

  prevPage(): void {
    const p = this.stagingPage();
    if (p > 0) this.loadStaging(p - 1);
  }

  nextPage(): void {
    const p = this.stagingPage();
    if (p + 1 < this.stagingTotalPages()) this.loadStaging(p + 1);
  }
}
