import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

import { environment } from '../../app/src/environments/environment';

export interface MessageResponse<T> {
  success: boolean;
  message: string;
  data?: T;
}

export interface FreelancerProfileDto {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string | null;
  title?: string | null;
  bio?: string | null;
  skills?: string[] | null;
  hourlyRate?: number | null;
  dailyRate?: number | null;
  availability?: string | null;
  address?: string | null;
  city?: string | null;
  country?: string | null;
  postalCode?: string | null;
  profilePicture?: string | null;
  portfolioUrl?: string | null;
  linkedinUrl?: string | null;
  githubUrl?: string | null;
  dateOfBirth?: string | null;
  gender?: string | null;
  isVerified?: boolean | null;
  isAvailable?: boolean | null;
  rating?: number | null;
  totalProjects?: number | null;
  successRate?: number | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface UpdateFreelancerProfileRequest {
  firstName?: string | null;
  lastName?: string | null;
  phone?: string | null;
  title?: string | null;
  bio?: string | null;
  skills?: string[] | null;
  hourlyRate?: number | null;
  dailyRate?: number | null;
  availability?: string | null;
  address?: string | null;
  city?: string | null;
  country?: string | null;
  postalCode?: string | null;
  profilePicture?: string | null;
  portfolioUrl?: string | null;
  linkedinUrl?: string | null;
  githubUrl?: string | null;
  dateOfBirth?: string | null;
  gender?: string | null;
  isAvailable?: boolean | null;
}

@Injectable({ providedIn: 'root' })
export class FreelancerApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;

  getMe() {
    return this.http.get<MessageResponse<FreelancerProfileDto>>(`${this.baseUrl}/freelancers/me`);
  }

  updateMyProfile(payload: UpdateFreelancerProfileRequest) {
    return this.http.put<MessageResponse<FreelancerProfileDto>>(
      `${this.baseUrl}/freelancers/me/profile`,
      payload
    );
  }
}
