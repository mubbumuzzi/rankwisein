import { PredictResponse } from './predict.models';

export interface CreateLeadRequest {
  name?: string;
  mobile?: string;
  rank: number;
  category: string;
  gender: string;
  preferredBranches: string[];
  year?: number;
  phase?: string;
}

export interface LeadResponse {
  id: number;
  name: string | null;
  mobile: string | null;
  rank: number;
  category: string;
  gender: string;
  branch: string;
  createdAt: string;
}

export interface LeadPredictResponse {
  leadId: number;
  recommendations: PredictResponse;
}

export interface LeadPageResponse {
  content: LeadResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}
