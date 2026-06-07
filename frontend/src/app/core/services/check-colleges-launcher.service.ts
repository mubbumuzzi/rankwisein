import { Injectable, inject } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Router } from '@angular/router';
import {
  CheckCollegesModalComponent,
  CheckCollegesModalResult,
} from '../../shared/components/check-colleges-modal/check-colleges-modal.component';
import { PredictStateService } from './predict-state.service';

@Injectable({ providedIn: 'root' })
export class CheckCollegesLauncherService {
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);
  private readonly predictState = inject(PredictStateService);

  open(): void {
    const ref = this.dialog.open(CheckCollegesModalComponent, {
      width: '100%',
      maxWidth: '520px',
      panelClass: 'rw-check-dialog',
      autoFocus: 'first-titled-element',
    });

    ref.afterClosed().subscribe((result: CheckCollegesModalResult | undefined) => {
      if (!result) {
        return;
      }
      const { form, response } = result;
      this.predictState.setResult(
        {
          rank: form.rank,
          category: form.category,
          gender: form.gender,
          preferredBranches: form.preferredBranches,
          year: form.year,
          phase: form.phase,
        },
        response.recommendations
      );
      void this.router.navigate(['/results']);
    });
  }
}
