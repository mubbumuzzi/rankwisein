export interface ImportUploadResponse {
  importId: number;
  status: string;
  year: number;
  phase: string;
  totalParsed: number;
  validRows: number;
  duplicateRows: number;
  invalidRows: number;
}

export interface ImportStagingRow {
  id: number;
  importFileId: number;
  collegeCode: string;
  collegeName: string;
  branchCode: string;
  branchName: string;
  category: string;
  gender: string;
  closingRank: number;
  valid: boolean;
  duplicate: boolean;
  errorMessage?: string;
}

export interface ApproveImportResponse {
  importId: number;
  status: string;
  inserted: number;
  skippedDuplicates: number;
  invalidRows: number;
  durationMs: number;
}

export interface ImportStatusResponse {
  importId: number;
  status: string;
  year: number;
  phase: string;
  totalParsed: number;
  validRows: number;
  duplicateRows: number;
  invalidRows: number;
  inserted: number;
  durationMs: number;
}

export interface PurgeCutoffsResponse {
  year: number;
  phase: string;
  matched: number;
  deleted: number;
}
