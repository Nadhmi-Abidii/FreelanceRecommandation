export type UserRole = 'ROLE_CLIENT' | 'ROLE_FREELANCER' | 'ROLE_ADMIN' | string;

export interface AuthResponse {
  token: string;
  tokenType: string; // "Bearer"
  userId: number;
  email: string;
  role: UserRole;
  firstName?: string;
  lastName?: string;
}

export interface MessageResponse<T> {
  success: boolean;
  message: string;
  data?: T;
}
