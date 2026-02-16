export interface Domaine {
  id: number;
  name: string;
  description?: string;
  icon?: string;
  color?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt?: string;

  // ✅ rendre optionnel pour éviter TS2339 si le back ne l'envoie pas
  skills?: string[];
}

export interface DomainePage {
  content: Domaine[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number; // page index
}
