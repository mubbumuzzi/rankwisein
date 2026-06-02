export interface PredictRequest {
  rank: number;
  category: string;
  gender: string;
  preferredBranches: string[];
  year?: number;
  phase?: string;
}

export interface CollegeRecommendation {
  collegeCode: string;
  collegeName: string;
  branchCode: string;
  branchName: string;
  closingRank: number;
  category: string;
  gender: string;
  year: number;
  phase: string;
  bucket: 'DREAM' | 'TARGET' | 'SAFE';
  ratio: number;
  preferredBranch: boolean;
}

export interface PredictResponse {
  rank: number;
  category: string;
  gender: string;
  year: number;
  phase: string;
  dream: CollegeRecommendation[];
  target: CollegeRecommendation[];
  safe: CollegeRecommendation[];
}

export interface PredictFormState {
  rank: number | null;
  category: string;
  gender: string;
  preferredBranches: string[];
  year?: number;
  phase?: string;
}
