import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ApproveImportResponse,
  ImportStagingRow,
  ImportUploadResponse,
  PurgeCutoffsResponse,
} from '../models/import.models';
import { PageResponse } from '../models/page.models';

@Injectable({ providedIn: 'root' })
export class ImportService {
  private readonly http = inject(HttpClient);

  uploadPdf(file: File, year: number, phase: string): Observable<ImportUploadResponse> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<ImportUploadResponse>(
      `${environment.apiBaseUrl}/admin/imports/pdf?year=${year}&phase=${encodeURIComponent(phase)}`,
      form
    );
  }

  getStaging(importId: number, page = 0, size = 50): Observable<PageResponse<ImportStagingRow>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<ImportStagingRow>>(
      `${environment.apiBaseUrl}/admin/imports/${importId}/staging`,
      { params }
    );
  }

  deleteStagingRow(importId: number, rowId: number): Observable<void> {
    return this.http.delete<void>(
      `${environment.apiBaseUrl}/admin/imports/${importId}/staging/${rowId}`
    );
  }

  approve(importId: number): Observable<ApproveImportResponse> {
    return this.http.post<ApproveImportResponse>(
      `${environment.apiBaseUrl}/admin/imports/${importId}/approve`,
      {}
    );
  }

  purgeCutoffs(year: number, phase: string): Observable<PurgeCutoffsResponse> {
    const params = new HttpParams().set('year', year).set('phase', phase);
    return this.http.post<PurgeCutoffsResponse>(
      `${environment.apiBaseUrl}/admin/cutoffs/purge`,
      {},
      { params }
    );
  }
}
