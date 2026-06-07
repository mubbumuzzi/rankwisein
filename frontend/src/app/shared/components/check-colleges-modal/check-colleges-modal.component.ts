import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MetaService } from '../../../core/services/meta.service';
import { LeadService } from '../../../core/services/lead.service';
import { MetaResponse } from '../../../core/models/meta.models';
import { LeadPredictResponse } from '../../../core/models/lead.models';
import { startWith } from 'rxjs';

export interface CheckCollegesModalResult {
  leadId: number;
  response: LeadPredictResponse;
  form: {
    rank: number;
    category: string;
    gender: string;
    preferredBranches: string[];
    year: number;
    phase: string;
    mobile?: string;
  };
}

@Component({
  selector: 'app-check-colleges-modal',
  imports: [ReactiveFormsModule, MatDialogModule],
  templateUrl: './check-colleges-modal.component.html',
  styles: [
    `
      :host {
        display: block;
      }

      .rw-field {
        display: block;
      }

      .rw-label {
        display: block;
        margin-bottom: 0.375rem;
        font-size: 0.875rem;
        font-weight: 500;
        color: #334155;
      }

      .rw-input,
      .rw-select {
        display: block;
        width: 100%;
        box-sizing: border-box;
        border: 1px solid #e2e8f0;
        border-radius: 0.75rem;
        background: #fff;
        padding: 0.625rem 0.75rem;
        font-size: 0.875rem;
        line-height: 1.25rem;
        color: #0f172a;
        outline: none;
        box-shadow: 0 1px 2px rgb(15 23 42 / 0.04);
      }

      .rw-input::placeholder {
        color: #94a3b8;
      }

      .rw-input:focus,
      .rw-select:focus {
        border-color: #f97316;
        box-shadow: 0 0 0 3px rgb(249 115 22 / 0.15);
      }

      .rw-input-invalid,
      .rw-select.rw-input-invalid {
        border-color: #f87171;
      }

      .rw-select {
        appearance: none;
        padding-right: 2.5rem;
        cursor: pointer;
      }

      .rw-select-wrap {
        position: relative;
      }

      .rw-select-chevron {
        pointer-events: none;
        position: absolute;
        right: 0.75rem;
        top: 50%;
        transform: translateY(-50%);
        font-size: 0.65rem;
        color: #94a3b8;
      }

      .rw-hint {
        margin-top: 0.25rem;
        font-size: 0.75rem;
        color: #64748b;
      }

      .rw-error {
        margin-top: 0.25rem;
        font-size: 0.875rem;
        color: #dc2626;
      }

      /* Material outline fields must not be used in this modal */
      :host ::ng-deep .mat-mdc-form-field {
        display: none !important;
      }
    `,
  ],
})
export class CheckCollegesModalComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly metaService = inject(MetaService);
  private readonly leadService = inject(LeadService);
  private readonly dialogRef = inject(MatDialogRef<CheckCollegesModalComponent>);

  readonly meta = signal<MetaResponse | null>(null);
  readonly metaLoading = signal(true);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly selectedBranches = signal<string[]>([]);

  readonly form = this.fb.group({
    rank: [null as number | null, [Validators.required, Validators.min(1)]],
    category: ['', Validators.required],
    gender: ['', Validators.required],
    mobile: ['', Validators.pattern(/^(|[6-9]\d{9})$/)],
    preferredBranches: [[] as string[], Validators.required],
    year: ['', Validators.required],
    phase: ['', Validators.required],
  });

  ngOnInit(): void {
    this.form
      .get('preferredBranches')!
      .valueChanges.pipe(startWith(this.form.value.preferredBranches ?? []))
      .subscribe((v) => this.selectedBranches.set([...(v ?? [])]));

    this.metaService.getMeta().subscribe({
      next: (m) => {
        this.meta.set(m);
        this.metaLoading.set(false);
      },
      error: (e) => {
        this.metaLoading.set(false);
        this.errorMessage.set(e.message ?? 'Failed to load form.');
      },
    });
  }

  close(): void {
    this.dialogRef.close();
  }

  isBranchSelected(code: string): boolean {
    return (this.form.value.preferredBranches ?? []).includes(code);
  }

  toggleBranch(code: string): void {
    const current = [...(this.form.value.preferredBranches ?? [])];
    const idx = current.indexOf(code);
    if (idx >= 0) current.splice(idx, 1);
    else current.push(code);
    this.form.patchValue({ preferredBranches: current });
    this.form.get('preferredBranches')?.markAsTouched();
  }

  selectAllBranches(): void {
    const branches = this.meta()?.branches ?? [];
    this.form.patchValue({ preferredBranches: [...branches] });
  }

  genderLabel(g: string): string {
    return g === 'BOYS' ? 'Male' : g === 'GIRLS' ? 'Female' : g;
  }

  formatPhase(phase: string): string {
    return phase.replace(/_/g, ' ');
  }

  branchLabel(code: string): string {
    switch ((code ?? '').toUpperCase()) {
      case 'INF':
        return 'IT';
      case 'MEC':
        return 'MECH';
      case 'CIV':
        return 'CIVIL';
      case 'AIM':
        return 'AIML';
      default:
        return code;
    }
  }

  submit(): void {
    this.errorMessage.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      if ((this.form.value.preferredBranches ?? []).length === 0) {
        this.errorMessage.set('Select at least one branch.');
      }
      return;
    }

    const rank = this.form.value.rank!;
    const category = this.form.value.category!;
    const gender = this.form.value.gender!;
    const preferredBranches = this.form.value.preferredBranches!;
    const year = Number(this.form.value.year);
    const phase = this.form.value.phase!;
    const mobile = this.form.value.mobile?.trim() || undefined;

    this.submitting.set(true);
    this.leadService
      .submit({ rank, category, gender, preferredBranches, year, phase, mobile })
      .subscribe({
        next: (response) => {
          this.submitting.set(false);
          this.dialogRef.close({
            leadId: response.leadId,
            response,
            form: { rank, category, gender, preferredBranches, year, phase, mobile },
          } satisfies CheckCollegesModalResult);
        },
        error: (e) => {
          this.submitting.set(false);
          this.errorMessage.set(e.message ?? 'Could not load recommendations. Try again.');
        },
      });
  }
}
