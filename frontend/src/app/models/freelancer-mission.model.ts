import { MissionStatus } from '../services/freelancer-mission.service';

export type MissionUrgency = 'STANDARD' | 'URGENT';

export interface MissionMessage {
  id: number;
  author: 'freelancer' | 'client';
  content: string;
  sentAt: string; // ISO date
}

export interface FreelancerMission {
  id: number;
  clientId?: number | null;
  domaineId?: number | null;
  backendStatus?: string | null;
  candidatureId?: number | null;
  clientCompanyName?: string | null;
  clientCity?: string | null;
  clientCountry?: string | null;
  clientProfilePicture?: string | null;
  title: string;
  clientName: string;
  domaine: string;
  summary: string;
  description: string;
  location: string;
  budget: string;
  postedAt: string;
  deadline: string;
  status: MissionStatus;
  urgency: MissionUrgency;
  experienceLevel: 'Junior' | 'Intermédiaire' | 'Senior';
  workModel: 'Remote' | 'Hybrid' | 'On-site';
  duration: string;
  weeklyRhythm: string;
  deliverables: string[];
  tags: string[];
  requirements: string[];
  tools: string[];
  isSaved: boolean;
  appliedAt?: string | null;
  conversation: MissionMessage[];
}
