// src/app/features/admin/domaines/competence.model.ts

export type CompetenceLevel = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED' | 'EXPERT';

export interface Competence {
  id: number;

  // 🔗 Lien vers le domaine
  domaineId?: number | null;
  domaineName?: string | null;

  freelancerId?: number | null;
  freelancerName?: string | null;

  name: string;
  description?: string | null;

  level?: CompetenceLevel | string | null;
  yearsOfExperience?: number | null;

  isCertified: boolean;
  certificationName?: string | null;
  certificationDate?: string | null;

  // ✅ important pour le statut + toggle
  isActive: boolean;

  createdAt: string;
  updatedAt?: string | null;
}

export interface CompetencePage {
  content: Competence[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

// src/app/features/admin/domaines/domaine.model.ts

export interface Domaine {
  id: number;
  name: string;
  description?: string;
  icon?: string;
  color?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt?: string;

  // ✅ rendre optionnel pour éviter TS si le back ne l'envoie pas
  skills?: string[];
}

export interface DomainePage {
  content: Domaine[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number; // page index
}
