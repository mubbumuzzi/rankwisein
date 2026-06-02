import { DecimalPipe } from '@angular/common';
import { Component, input } from '@angular/core';
import { CollegeRecommendation } from '../../../core/models/predict.models';

@Component({
  selector: 'app-college-card',
  imports: [DecimalPipe],
  template: `
    <article
      class="rounded-xl border border-slate-200 bg-white p-5 shadow-sm transition hover:shadow-md"
    >
      <div class="mb-3 flex flex-wrap items-start justify-between gap-2">
        <div>
          <h3 class="text-lg font-semibold text-rw-primary">{{ college().collegeName }}</h3>
          <p class="-mt-0.5 text-sm font-semibold text-slate-800">{{ college().branchName }}</p>
          <p class="mt-0.5 text-sm text-slate-500">{{ college().collegeCode }} · {{ college().branchCode }}</p>
        </div>
        <span class="rounded-full border px-3 py-1 text-xs font-bold tracking-wide" [class]="badgeClass()">
          {{ college().bucket }}
        </span>
      </div>
      <dl class="grid grid-cols-2 gap-3 text-sm sm:grid-cols-4">
        <div>
          <dt class="text-slate-500">Closing rank</dt>
          <dd class="font-semibold text-rw-primary">{{ college().closingRank | number }}</dd>
        </div>
        <div>
          <dt class="text-slate-500">Your ratio</dt>
          <dd class="font-semibold">{{ college().ratio }}</dd>
        </div>
        <div>
          <dt class="text-slate-500">Category</dt>
          <dd class="font-medium">{{ college().category }}</dd>
        </div>
        <div>
          <dt class="text-slate-500">Year / Phase</dt>
          <dd class="font-medium">{{ college().year }} · {{ formatPhase(college().phase) }}</dd>
        </div>
      </dl>
      @if (college().preferredBranch) {
        <p class="mt-3 text-xs font-medium text-rw-accent">★ Preferred branch match</p>
      }
    </article>
  `,
})
export class CollegeCardComponent {
  readonly college = input.required<CollegeRecommendation>();

  badgeClass(): string {
    switch (this.college().bucket) {
      case 'DREAM':
        return 'bucket-dream';
      case 'TARGET':
        return 'bucket-target';
      default:
        return 'bucket-safe';
    }
  }

  formatPhase(phase: string): string {
    return phase.replace(/_/g, ' ');
  }
}
