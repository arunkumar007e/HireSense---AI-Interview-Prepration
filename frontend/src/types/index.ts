export interface UserProfile {
  id: number;
  headline?: string | null;
  summary?: string | null;
  experienceYears?: number | null;
  education?: string | null;
  location?: string | null;
  phone?: string | null;
  githubUrl?: string | null;
  linkedinUrl?: string | null;
  portfolioUrl?: string | null;
  updatedAt?: string | null;
}

export interface AppConfig {
  aiProvider: string;
  aiConfigured: boolean;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  email: string;
  fullName: string;
  role: string;
  userId: number;
  expiresAt: string;
  profile?: UserProfile | null;
}

export interface UserResponse {
  id: number;
  email: string;
  fullName: string;
  role: string;
  enabled: boolean;
  createdAt: string;
}

export interface SkillResponse {
  id: number;
  name: string;
  category?: string | null;
  description?: string | null;
}

export interface JobRoleResponse {
  id: number;
  title: string;
  description?: string | null;
  category?: string | null;
  requiredSkills: SkillResponse[];
}

export interface SkillGapItemResponse {
  skill: string;
  requiredLevel: string;
  currentLevel: string;
  gap: string;
  explanation: string;
}

export interface SkillGapResponse {
  jobRoleId: number;
  jobRoleTitle: string;
  matchScore: number;
  summary: string;
  skills: SkillGapItemResponse[];
}

export interface ResumeSummaryResponse {
  id: number;
  fileName: string;
  fileType?: string | null;
  fileSize?: number | null;
  analysisStatus: string;
  atsScore?: number | null;
  keywordMatchScore?: number | null;
  analysisSummary?: string | null;
  parsedSkills: string[];
  createdAt: string;
  analyzedAt?: string | null;
}

export interface ResumeAnalysisResponse {
  resumeId: number;
  fileName: string;
  atsScore: number;
  keywordMatchScore: number;
  extractedSkills: string[];
  summary: string;
  strengths: string[];
  improvements: string[];
  status: string;
}

export type InterviewStatus = 'CONFIGURED' | 'IN_PROGRESS' | 'COMPLETED' | 'ABANDONED';
export type DifficultyLevel = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';

export interface InterviewSessionResponse {
  id: number;
  jobRoleId?: number | null;
  jobRoleTitle?: string | null;
  status: InterviewStatus;
  difficulty: DifficultyLevel;
  numberOfQuestions: number;
  timeLimitSeconds?: number | null;
  currentQuestionIndex: number;
  startedAt?: string | null;
  completedAt?: string | null;
  createdAt: string;
}

export interface InterviewQuestionResponse {
  id: number;
  sessionId: number;
  content: string;
  category: string;
  difficulty: DifficultyLevel;
  orderIndex: number;
  answered: boolean;
}

export interface AnswerEvaluationResponse {
  answerId: number;
  questionId: number;
  question: string;
  score: number;
  relevanceScore: number;
  clarityScore: number;
  depthScore: number;
  feedback: string;
  topic?: string | null;
  strengths: string[];
  improvements: string[];
  missingKeywords: string[];
  suggestedAnswer?: string | null;
}

export interface QuestionResultResponse {
  questionId: number;
  question: string;
  category: string;
  userAnswer: string;
  score: number;
  feedback: string;
  strengths: string[];
  improvements: string[];
  missingKeywords: string[];
  suggestedAnswer: string;
}

export interface InterviewResultResponse {
  sessionId: number;
  jobRoleTitle: string;
  overallScore: number;
  summary: string;
  questions: QuestionResultResponse[];
}

export interface LearningPlanItemResponse {
  id: number;
  title: string;
  description?: string | null;
  resourceUrl?: string | null;
  skillName?: string | null;
  orderIndex: number;
  targetDays?: number | null;
  completed: boolean;
  completedAt?: string | null;
}

export interface LearningPlanResponse {
  id: number;
  title: string;
  description?: string | null;
  focusSkills: string[];
  status: string;
  totalItems: number;
  completedItems: number;
  progressPercent: number;
  items: LearningPlanItemResponse[];
  createdAt: string;
}

export type ProgressMetricType =
  | 'INTERVIEW_SCORE'
  | 'QUESTIONS_ANSWERED'
  | 'SKILL_GAP'
  | 'PLAN_COMPLETION'
  | 'RESUME_ANALYSIS';

export interface ProgressResponse {
  id: number;
  metricType: ProgressMetricType;
  metricValue: number;
  relatedTitle?: string | null;
  note?: string | null;
  recordedAt: string;
}

export interface ProgressSummaryResponse {
  interviewsCompleted: number;
  interviewScoreAverage: number;
  questionsAnswered: number;
  learningPlansCreated: number;
  learningPlanCompletionPercent: number;
  recentProgress: ProgressResponse[];
}

export interface AdminStatsResponse {
  totalUsers: number;
  adminUsers: number;
  candidateUsers: number;
  totalResumes: number;
  totalInterviews: number;
  completedInterviews: number;
  totalLearningPlans: number;
  interviewsByStatus: Record<string, number>;
  questionsAnswered: number;
  averageInterviewScore: number;
}

export interface ApiErrorBody {
  status: number;
  message: string;
  fieldErrors?: Record<string, string> | null;
  timestamp: string;
}