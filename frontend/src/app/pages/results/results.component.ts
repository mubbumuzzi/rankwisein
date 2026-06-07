import { DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { PredictStateService } from '../../core/services/predict-state.service';
import { CollegeCardComponent } from '../../shared/components/college-card/college-card.component';
import { WhatsappStickyCtaComponent } from '../../shared/components/whatsapp-sticky-cta/whatsapp-sticky-cta.component';
import { CollegeRecommendation, PredictResponse } from '../../core/models/predict.models';
import { WhatsappCounsellingLinkService } from '../../core/services/whatsapp-counselling-link.service';

@Component({
  selector: 'app-results',
  imports: [RouterLink, CollegeCardComponent, DecimalPipe, WhatsappStickyCtaComponent],
  templateUrl: './results.component.html',
})
export class ResultsComponent implements OnInit {
  private readonly state = inject(PredictStateService);
  private readonly router = inject(Router);
  private readonly whatsappLink = inject(WhatsappCounsellingLinkService);

  readonly result = signal<PredictResponse | null>(null);
  readonly form = signal<{ preferredBranches: string[] } | null>(null);
  readonly whatsappUrl = this.whatsappLink.url;
  readonly totalCount = computed(() => {
    const r = this.result();
    if (!r) return 0;
    return r.dream.length + r.target.length + r.safe.length;
  });

  ngOnInit(): void {
    const r = this.state.lastResult();
    const f = this.state.lastForm();
    if (!r) {
      void this.router.navigate(['/'], { queryParams: { check: '1' } });
      return;
    }
    this.result.set(r);
    this.form.set(f ? { preferredBranches: f.preferredBranches ?? [] } : null);
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

  trackByCollege(_: number, item: CollegeRecommendation): string {
    return `${item.collegeCode}-${item.branchCode}-${item.bucket}`;
  }
}
