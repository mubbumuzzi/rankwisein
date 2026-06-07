export interface StudentProfile {
  rank: number | null;
  category: string | null;
  gender: string | null;
  preferredBranches: string[];
  preferredLocation: string | null;
  budget: string | null;
  completeForPrediction: boolean;
}

export interface ChatSession {
  sessionId: number;
  chatUserId: number;
  visitorToken: string;
  title: string;
  messageCount: number;
  profile: StudentProfile;
  suggestedQuestions: string[];
}

export interface CollegeBucketItem {
  collegeCode: string;
  collegeName: string;
  branchCode: string;
  branchName: string;
  closingRank: number;
  ratio: number;
  bucket: string;
  confidence: string;
}

export interface CollegePredictionPayload {
  dream: CollegeBucketItem[];
  target: CollegeBucketItem[];
  safe: CollegeBucketItem[];
  confidenceNote: string;
}

export interface CollegeCompareCard {
  code: string;
  name: string;
  location: string;
  affiliation: string;
  popularBranches: string;
  cutoffSummary: string;
  pros: string;
  cons: string;
  feesNote: string;
  placementsNote: string;
}

export interface CollegeComparisonPayload {
  colleges: CollegeCompareCard[];
  summary: string;
}

export interface DocumentListPayload {
  required: string[];
  optional: string[];
  note: string;
}

export interface BranchAdvicePayload {
  recommendedBranches: string[];
  reasoning: string;
}

export interface ChatStructuredPayload {
  type: string;
  prediction?: CollegePredictionPayload;
  comparison?: CollegeComparisonPayload;
  documents?: DocumentListPayload;
  branchAdvice?: BranchAdvicePayload;
}

export interface ChatMessage {
  id: number;
  role: 'USER' | 'ASSISTANT' | 'SYSTEM';
  content: string;
  structured?: ChatStructuredPayload | null;
  createdAt: string;
  suggestedQuestions?: string[];
  showLeadCta?: boolean;
  missingProfileFields?: string[];
  pending?: boolean;
}

export interface ChatAdminStats {
  totalSessions: number;
  totalMessages: number;
  totalChatUsers: number;
  totalProfiles: number;
  chatOpens: number;
  messagesSent: number;
  whatsappClicks: number;
  predictorUsage: number;
  comparisonUsage: number;
  topEventTypes: { eventType: string; count: number }[];
  recentProfiles: Record<string, unknown>[];
}

export interface ChatSessionSummary {
  id: number;
  chatUserId: number;
  title: string;
  messageCount: number;
  leadCtaShown: boolean;
  createdAt: string;
  updatedAt: string;
  profileRank: number | null;
  profileCategory: string | null;
}
