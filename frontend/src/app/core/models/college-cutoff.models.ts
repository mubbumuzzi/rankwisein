export interface CollegeSummary {
  id: number;
  code: string;
  name: string;
  location: string | null;
  district: string | null;
}

export interface CollegeCutoffEntry {
  year: number;
  phase: string;
  branchCode: string;
  branchName: string;
  closingRank: number;
}

export interface CollegeCutoffResponse {
  college: CollegeSummary;
  category: string;
  gender: string;
  cutoffs: CollegeCutoffEntry[];
}

export interface CutoffYearTable {
  year: number;
  phases: string[];
  rows: CutoffBranchRow[];
}

export interface CutoffBranchRow {
  branchCode: string;
  branchName: string;
  ranks: Record<string, number | null>;
}
