import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MetaService } from '../../core/services/meta.service';
import { PredictService } from '../../core/services/predict.service';
import { PredictStateService } from '../../core/services/predict-state.service';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner.component';
import { MetaResponse } from '../../core/models/meta.models';
import { startWith } from 'rxjs';

@Component({
  selector: 'app-predict',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    LoadingSpinnerComponent,
  ],
  templateUrl: './predict.component.html',
})
export class PredictComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly metaService = inject(MetaService);
  private readonly predictService = inject(PredictService);
  private readonly state = inject(PredictStateService);
  private readonly router = inject(Router);

  readonly meta = signal<MetaResponse | null>(null);
  readonly metaLoading = signal(true);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly selectedBranches = signal<string[]>([]);

  readonly form = this.fb.group({
    rank: [null as number | null, [Validators.required, Validators.min(1)]],
    category: ['', Validators.required],
    gender: ['', Validators.required],
    preferredBranches: [[] as string[], Validators.required],
    year: [null as number | null, Validators.required],
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
        if (m.categories.length && !this.form.value.category) {
          this.form.patchValue({ category: m.categories[0] });
        }
        if (m.genders.length && !this.form.value.gender) {
          this.form.patchValue({ gender: m.genders[0] });
        }
        if (m.years.length && !this.form.value.year) {
          // Seed data currently exists for 2024; default to that when available.
          const preferredDefault = m.years.includes(2024) ? 2024 : m.years[m.years.length - 1];
          this.form.patchValue({ year: preferredDefault });
        }
        if (m.phases.length && !this.form.value.phase) {
          const preferredPhase = m.phases.includes('FINAL_PHASE') ? 'FINAL_PHASE' : m.phases[0];
          this.form.patchValue({ phase: preferredPhase });
        }
      },
      error: (e) => {
        this.metaLoading.set(false);
        this.errorMessage.set(e.message ?? 'Failed to load form options.');
      },
    });
  }

  isBranchSelected(code: string): boolean {
    return (this.form.value.preferredBranches ?? []).includes(code);
  }

  toggleBranch(code: string): void {
    const current = [...(this.form.value.preferredBranches ?? [])];
    const idx = current.indexOf(code);
    if (idx >= 0) {
      current.splice(idx, 1);
    } else {
      current.push(code);
    }
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
        this.errorMessage.set('Select at least one preferred branch.');
      }
      return;
    }

    const rank = this.form.value.rank!;
    const category = this.form.value.category!;
    const gender = this.form.value.gender!;
    const preferredBranches = this.form.value.preferredBranches!;
    const year = this.form.value.year!;
    const phase = this.form.value.phase!;

    this.submitting.set(true);
    this.predictService
      .predict({ rank, category, gender, preferredBranches, year, phase })
      .subscribe({
        next: (result) => {
          this.state.setResult({ rank, category, gender, preferredBranches, year, phase }, result);
          this.submitting.set(false);
          void this.router.navigate(['/results']);
        },
        error: (e) => {
          this.submitting.set(false);
          this.errorMessage.set(e.message ?? 'Prediction failed. Please try again.');
        },
      });
  }
}
