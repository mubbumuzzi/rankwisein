import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, shareReplay } from 'rxjs';
import { environment } from '../../../environments/environment';
import { MetaResponse } from '../models/meta.models';

@Injectable({ providedIn: 'root' })
export class MetaService {
  private readonly http = inject(HttpClient);
  private cache$?: Observable<MetaResponse>;

  getMeta(): Observable<MetaResponse> {
    if (!this.cache$) {
      this.cache$ = this.http
        .get<MetaResponse>(`${environment.apiBaseUrl}/meta`)
        .pipe(shareReplay(1));
    }
    return this.cache$;
  }
}
