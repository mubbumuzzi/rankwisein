import { DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, filter, switchMap } from 'rxjs';
import { MetaService } from '../../core/services/meta.service';
import { CollegeCutoffService } from '../../core/services/college-cutoff.service';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner.component';
import { MetaResponse } from '../../core/models/meta.models';
import {
  CollegeCutoffResponse,
  CollegeSummary,
  CutoffYearTable,
} from '../../core/models/college-cutoff.models';

const PHASE_ORDER = ['PHASE_1', 'PHASE_2', 'FINAL_PHASE'];

@Component({
  selector: 'app-college-cutoffs',
  imports: [ReactiveFormsModule, LoadingSpinnerComponent, DecimalPipe],
  templateUrl: './college-cutoffs.component.html',
})
export class CollegeCutoffsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly metaService = inject(MetaService);
  private readonly cutoffService = inject(CollegeCutoffService);
  private readonly searchInput$ = new Subject<string>();

  readonly meta = signal<MetaResponse | null>(null);
  readonly metaLoading = signal(true);
  readonly searching = signal(false);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly searchResults = signal<CollegeSummary[]>([]);
  readonly showDropdown = signal(false);
  readonly selectedCollege = signal<CollegeSummary | null>(null);
  readonly result = signal<CollegeCutoffResponse | null>(null);

  readonly yearTables = computed(() => buildYearTables(this.result()?.cutoffs ?? []));

  readonly form = this.fb.group({
    collegeQuery: ['', Validators.required],
    category: ['', Validators.required],
    gender: ['', Validators.required],
  });

  ngOnInit(): void {
    this.metaService.getMeta().subscribe({
      next: (m) => {
        this.meta.set(m);
        this.metaLoading.set(false);
      },
      error: (e) => {
        this.metaLoading.set(false);
        this.errorMessage.set(e.message ?? 'Failed to load form options.');
      },
    });

    this.searchInput$
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        filter((q) => q.trim().length >= 2),
        switchMap((q) => {
          this.searching.set(true);
          return this.cutoffService.searchColleges(q.trim());
        })
      )
      .subscribe({
        next: (colleges) => {
          this.searchResults.set(colleges);
          this.searching.set(false);
          this.showDropdown.set(colleges.length > 0);
        },
        error: () => {
          this.searchResults.set([]);
          this.searching.set(false);
        },
      });
  }

  onCollegeQueryInput(): void {
    const q = this.form.value.collegeQuery ?? '';
    this.selectedCollege.set(null);
    this.result.set(null);
    if (q.trim().length < 2) {
      this.searchResults.set([]);
      this.showDropdown.set(false);
      return;
    }
    this.searchInput$.next(q);
  }

  selectCollege(college: CollegeSummary): void {
    this.selectedCollege.set(college);
    this.form.patchValue({ collegeQuery: `${college.name} (${college.code})` });
    this.showDropdown.set(false);
    this.searchResults.set([]);
  }

  clearCollege(): void {
    this.selectedCollege.set(null);
    this.form.patchValue({ collegeQuery: '' });
    this.result.set(null);
    this.searchResults.set([]);
    this.showDropdown.set(false);
  }

  submit(): void {
    this.errorMessage.set(null);
    const college = this.selectedCollege();
    if (!college) {
      this.errorMessage.set('Select a college from the search results.');
      this.form.get('collegeQuery')?.markAsTouched();
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.result.set(null);
    this.cutoffService
      .getCutoffs(college.id, this.form.value.category!, this.form.value.gender!)
      .subscribe({
        next: (response) => {
          this.result.set(response);
          this.loading.set(false);
          if (response.cutoffs.length === 0) {
            this.errorMessage.set(
              'No cutoff data found for this college with the selected category and gender.'
            );
          }
        },
        error: (e) => {
          this.loading.set(false);
          this.errorMessage.set(e.message ?? 'Failed to load cutoffs.');
        },
      });
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
}

function buildYearTables(cutoffs: CollegeCutoffResponse['cutoffs']): CutoffYearTable[] {
  const byYear = new Map<number, typeof cutoffs>();
  for (const entry of cutoffs) {
    const list = byYear.get(entry.year) ?? [];
    list.push(entry);
    byYear.set(entry.year, list);
  }

  return [...byYear.entries()]
    .sort(([a], [b]) => b - a)
    .map(([year, entries]) => {
      const phases = PHASE_ORDER.filter((p) => entries.some((e) => e.phase === p));
      const branchMap = new Map<string, { branchName: string; ranks: Record<string, number | null> }>();

      for (const entry of entries) {
        let row = branchMap.get(entry.branchCode);
        if (!row) {
          row = { branchName: entry.branchName, ranks: {} };
          branchMap.set(entry.branchCode, row);
        }
        row.ranks[entry.phase] = entry.closingRank;
      }

      const rows = [...branchMap.entries()]
        .map(([branchCode, row]) => ({
          branchCode,
          branchName: row.branchName,
          ranks: Object.fromEntries(phases.map((p) => [p, row.ranks[p] ?? null])),
        }))
        .sort((a, b) => a.branchCode.localeCompare(b.branchCode));

      return { year, phases, rows };
    });
}
