export type CandidatureStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'WITHDRAWN';

export interface FreelancerSummary {
  id?: number | null;
  firstName?: string | null;
  lastName?: string | null;
  title?: string | null;
  email?: string | null;
  phone?: string | null;
  city?: string | null;
  country?: string | null;
}

export interface CandidatureMessage {
  id?: number | null;
  author?: 'CLIENT' | 'FREELANCER';
  content?: string | null;
  resumeUrl?: string | null;
  createdAt?: string | null;
  isFlagged?: boolean | null;
  flagScore?: number | null;
  flagLabel?: string | null;
  flagReason?: string | null;
}

export interface Candidature {
  id?: number | null;
  missionId?: number | null;
  freelancer?: FreelancerSummary | null;
  coverLetter?: string | null;
  resumeUrl?: string | null;
  clientMessage?: string | null;
  proposedPrice?: number | null;
  proposedDuration?: number | null;
  status?: CandidatureStatus | null;
  createdAt?: string | null;
  updatedAt?: string | null;
  messages?: CandidatureMessage[] | null;
}
