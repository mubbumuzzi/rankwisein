import { Injectable, signal } from '@angular/core';
import { PredictFormState, PredictResponse } from '../models/predict.models';

@Injectable({ providedIn: 'root' })
export class PredictStateService {
  readonly lastForm = signal<PredictFormState | null>(null);
  readonly lastResult = signal<PredictResponse | null>(null);

  setResult(form: PredictFormState, result: PredictResponse): void {
    this.lastForm.set(form);
    this.lastResult.set(result);
  }

  clear(): void {
    this.lastForm.set(null);
    this.lastResult.set(null);
  }
}
