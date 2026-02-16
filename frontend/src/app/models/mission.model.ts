export enum BudgetType {
  FIXED = 'FIXED',
  HOURLY = 'HOURLY',
  NEGOTIABLE = 'NEGOTIABLE'
}

export enum TypeTravail {
  REMOTE = 'REMOTE',
  ON_SITE = 'ON_SITE',
  HYBRID = 'HYBRID'
}

export enum NiveauExperience {
  BEGINNER = 'BEGINNER',
  INTERMEDIATE = 'INTERMEDIATE',
  ADVANCED = 'ADVANCED',
  EXPERT = 'EXPERT'
}

export interface MissionPayload {
  client: { id: number };
  domaine: { id: number };
  title: string;
  description: string;
  requirements?: string | null;
  budgetMin?: number | null;
  budgetMax?: number | null;
  budgetType: BudgetType;
  typeTravail: TypeTravail;
  niveauExperience: NiveauExperience;
  deadline?: string | null;       // 'YYYY-MM-DD'
  estimatedDuration: number;      // days
  skillsRequired?: string | null;
  isUrgent: boolean;
  attachments?: string | null;
}

export interface MessageResponse<T> {
  success: boolean;
  message: string;
  data?: T;
}
