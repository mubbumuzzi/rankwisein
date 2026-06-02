import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PredictRequest, PredictResponse } from '../models/predict.models';

@Injectable({ providedIn: 'root' })
export class PredictService {
  private readonly http = inject(HttpClient);

  predict(request: PredictRequest): Observable<PredictResponse> {
    return this.http.post<PredictResponse>(`${environment.apiBaseUrl}/predict`, request);
  }
}
