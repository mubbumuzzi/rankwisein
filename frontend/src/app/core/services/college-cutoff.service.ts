import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CollegeCutoffResponse,
  CollegeSummary,
} from '../models/college-cutoff.models';

@Injectable({ providedIn: 'root' })
export class CollegeCutoffService {
  private readonly http = inject(HttpClient);

  searchColleges(query: string, limit = 25): Observable<CollegeSummary[]> {
    const params = new HttpParams().set('q', query).set('limit', limit);
    return this.http.get<CollegeSummary[]>(`${environment.apiBaseUrl}/colleges/search`, { params });
  }

  getCutoffs(collegeId: number, category: string, gender: string): Observable<CollegeCutoffResponse> {
    const params = new HttpParams().set('category', category).set('gender', gender);
    return this.http.get<CollegeCutoffResponse>(
      `${environment.apiBaseUrl}/colleges/${collegeId}/cutoffs`,
      { params }
    );
  }
}
