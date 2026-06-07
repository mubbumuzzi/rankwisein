import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CreateLeadRequest,
  LeadPageResponse,
  LeadPredictResponse,
} from '../models/lead.models';

@Injectable({ providedIn: 'root' })
export class LeadService {
  private readonly http = inject(HttpClient);

  submit(request: CreateLeadRequest): Observable<LeadPredictResponse> {
    return this.http.post<LeadPredictResponse>(`${environment.apiBaseUrl}/leads`, request);
  }

  listAdmin(params: {
    page: number;
    size: number;
    search?: string;
    category?: string;
    gender?: string;
  }): Observable<LeadPageResponse> {
    let httpParams = new HttpParams()
      .set('page', params.page)
      .set('size', params.size);
    if (params.search) httpParams = httpParams.set('search', params.search);
    if (params.category) httpParams = httpParams.set('category', params.category);
    if (params.gender) httpParams = httpParams.set('gender', params.gender);
    return this.http.get<LeadPageResponse>(`${environment.apiBaseUrl}/admin/leads`, {
      params: httpParams,
    });
  }

  exportCsv(params: { search?: string; category?: string; gender?: string }): Observable<Blob> {
    let httpParams = new HttpParams();
    if (params.search) httpParams = httpParams.set('search', params.search);
    if (params.category) httpParams = httpParams.set('category', params.category);
    if (params.gender) httpParams = httpParams.set('gender', params.gender);
    return this.http.get(`${environment.apiBaseUrl}/admin/leads/export`, {
      params: httpParams,
      responseType: 'blob',
    });
  }
}
