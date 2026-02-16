import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';

import { MatToolbarModule } from '@angular/material/toolbar';
import { MatMenuModule } from '@angular/material/menu';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatBadgeModule } from '@angular/material/badge';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDividerModule } from '@angular/material/divider';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import {
  ClientApiService,
  ClientDto,
  UpdateClientProfileRequest
} from '../../services/client-api.service';
import {
  FreelancerApiService,
  FreelancerProfileDto,
  UpdateFreelancerProfileRequest
} from '../../services/freelancer-api.service';
import { AuthService as CoreAuthService } from '../../core/auth/auth';
import { FeedbackResponse, FeedbackService } from '../../services/feedback.service';
import { AiResumeExtractionResponse, AiService } from '../../services/ai.service';

interface ViewUserProfile {
  fullName: string;
  role: string;
  email: string;
  phone: string;
  companyName: string;
  city: string;
  country: string;
  bio: string;
  updatedAt: string;
  status: 'verified' | 'pending';
}

interface FreelancerProfileView {
  fullName: string;
  role: string;
  email: string;
  phone: string;
  city: string;
  country: string;
  title: string;
  bio: string;
  skills: string[];
  hourlyRate?: number | null;
  dailyRate?: number | null;
  availability?: string | null;
  updatedAt: string;
  status: 'verified' | 'pending';
  linkedinUrl?: string | null;
  githubUrl?: string | null;
  portfolioUrl?: string | null;
}

interface FeedbackMessage {
  type: 'profile' | 'password';
  message: string;
}

@Component({
  standalone: true,
  selector: 'app-profile',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatMenuModule,
    MatSlideToggleModule,
    MatBadgeModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatDividerModule,
    MatChipsModule,
    MatProgressBarModule,
    MatSnackBarModule
  ],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export default class ProfileComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ClientApiService);
  private readonly freelancerApi = inject(FreelancerApiService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly auth = inject(CoreAuthService);
  private readonly feedbackService = inject(FeedbackService);
  private readonly aiService = inject(AiService);

  readonly loading = signal(false);
  readonly loadError = signal<string | null>(null);
  readonly savingFreelancer = signal(false);
  readonly feedbackSummary = signal<{ averageRating: number; count: number } | null>(null);
  readonly freelancerFeedbacks = signal<FeedbackResponse[]>([]);
  readonly resumeFile = signal<File | null>(null);
  readonly resumeLoading = signal(false);
  readonly resumeError = signal<string | null>(null);
  readonly resumeResult = signal<AiResumeExtractionResponse | null>(null);

  private readonly _clientDto = signal<ClientDto | null>(null);
  private readonly _freelancerDto = signal<FreelancerProfileDto | null>(null);

  private readonly _user = signal<ViewUserProfile>({
    fullName: 'Profil',
    role: 'Client',
    email: '',
    phone: '',
    companyName: '',
    city: '',
    country: '',
    bio: '',
    updatedAt: new Date().toISOString(),
    status: 'pending'
  });

  readonly role = computed(() => (this.auth.role() || '').toUpperCase());
  readonly isClient = computed(() => this.role() === 'ROLE_CLIENT');
  readonly isFreelancer = computed(() => this.role() === 'ROLE_FREELANCER');

  readonly user = computed(() => this._user());

  readonly freelancer = computed<FreelancerProfileView>(() => {
    const f = this._freelancerDto();
    const skills = (f?.skills ?? []).filter(Boolean) as string[];

    return {
      fullName: `${f?.firstName ?? ''} ${f?.lastName ?? ''}`.trim() || 'Freelancer',
      role: 'Freelancer',
      email: f?.email ?? '',
      phone: f?.phone ?? '',
      city: f?.city ?? '',
      country: f?.country ?? '',
      title: f?.title ?? '',
      bio: f?.bio ?? '',
      skills,
      hourlyRate: f?.hourlyRate ?? null,
      dailyRate: f?.dailyRate ?? null,
      availability: f?.availability ?? '',
      updatedAt: f?.updatedAt ?? new Date().toISOString(),
      status: f?.isVerified ? 'verified' : 'pending',
      linkedinUrl: f?.linkedinUrl ?? null,
      githubUrl: f?.githubUrl ?? null,
      portfolioUrl: f?.portfolioUrl ?? null
    };
  });

  readonly clientLastUpdated = computed(() =>
    this.formatDate(this._clientDto()?.updatedAt ?? this.user().updatedAt)
  );

  readonly freelancerLastUpdated = computed(() =>
    this.formatDate(this._freelancerDto()?.updatedAt ?? new Date().toISOString())
  );

  readonly initials = computed(() => this.initialsFrom(this.user().fullName));
  readonly freelancerInitials = computed(() => this.initialsFrom(this.freelancer().fullName));

  readonly profileCompletion = computed(() => {
    const values = [
      this.user().fullName,
      this.user().email,
      this.user().phone,
      this.user().companyName,
      this.user().city,
      this.user().country,
      this.user().bio
    ];
    const filled = values.filter(v => (v ?? '').toString().trim().length > 0).length;
    return Math.round((filled / values.length) * 100);
  });

  readonly freelancerCompletion = computed(() => {
    const f = this.freelancer();
    const values = [
      f.fullName,
      f.email,
      f.phone,
      f.city,
      f.country,
      f.title,
      f.bio,
      (f.skills ?? []).join(','),
      f.hourlyRate ?? f.dailyRate ?? ''
    ];
    const filled = values.filter(v => (v ?? '').toString().trim().length > 0).length;
    return Math.round((filled / values.length) * 100);
  });

  readonly freelancerMissingFields = computed(() => {
    const f = this.freelancer();
    const missing: string[] = [];
    if (!f.phone) missing.push('telephone');
    if (!f.city) missing.push('ville');
    if (!f.country) missing.push('pays');
    if (!f.title) missing.push('titre');
    if (!f.bio) missing.push('bio');
    if (!f.skills.length) missing.push('competences');
    if (f.hourlyRate == null && f.dailyRate == null) missing.push('tarif');
    return missing;
  });

  readonly profileForm = this.fb.nonNullable.group({
    firstName: ['', [Validators.required, Validators.minLength(2)]],
    lastName: ['', [Validators.required, Validators.minLength(2)]],
    email: [{ value: '', disabled: true }, [Validators.required, Validators.email]],
    phone: ['', [Validators.pattern(/^\+?[0-9 ]{7,15}$/)]],
    companyName: ['', [Validators.minLength(2)]],
    city: ['', [Validators.minLength(2)]],
    country: ['', [Validators.minLength(2)]],
    bio: ['', [Validators.minLength(10)]]
  });

  readonly freelancerForm = this.fb.nonNullable.group({
    firstName: ['', [Validators.required, Validators.minLength(2)]],
    lastName: ['', [Validators.required, Validators.minLength(2)]],
    email: [{ value: '', disabled: true }, [Validators.required, Validators.email]],
    phone: ['', [Validators.required, Validators.pattern(/^\+?[0-9 ]{7,15}$/)]],
    title: ['', [Validators.minLength(2)]],
    city: ['', [Validators.minLength(2)]],
    country: ['', [Validators.minLength(2)]],
    hourlyRate: [null as number | null, [Validators.min(1)]],
    dailyRate: [null as number | null, [Validators.min(1)]],
    availability: [''],
    skills: [''],
    bio: ['', [Validators.minLength(10)]],
    linkedinUrl: [''],
    githubUrl: [''],
    portfolioUrl: ['']
  });

  readonly passwordForm = this.fb.group(
    {
      currentPassword: [''],
      newPassword: [''],
      confirmPassword: ['']
    },
    { validators: c => this.passwordsMatch(c) }
  );

  readonly feedback = signal<FeedbackMessage | null>(null);
  private feedbackTimer: ReturnType<typeof setTimeout> | null = null;

  ngOnInit(): void {
    if (this.isFreelancer()) {
      this.loadFreelancerProfile();
    } else {
      this.loadClientProfile();
    }
  }

  toggleDark(on: boolean) {
    document.documentElement.classList.toggle('dash-dark', !!on);
  }

  reload() {
    this.isFreelancer() ? this.loadFreelancerProfile() : this.loadClientProfile();
  }

  private loadClientProfile(): void {
    this.loading.set(true);
    this.loadError.set(null);
    this.feedbackSummary.set(null);
    this.freelancerFeedbacks.set([]);
    this.api.getMe().subscribe({
      next: res => {
        this.loading.set(false);
        if (!res?.success || !res.data) {
          this.loadError.set('Impossible de charger votre profil.');
          return;
        }

        const u = res.data;
        this._clientDto.set(u);
        this._user.set({
          fullName: `${u.firstName ?? ''} ${u.lastName ?? ''}`.trim() || 'Profil',
          role: 'Client',
          email: u.email,
          phone: u.phone ?? '',
          companyName: u.companyName ?? '',
          city: u.city ?? '',
          country: u.country ?? '',
          bio: u.bio ?? '',
          updatedAt: u.updatedAt ?? new Date().toISOString(),
          status: u.isVerified ? 'verified' : 'pending'
        });
        this.loadFeedbackSummary(u.id);

        this.profileForm.reset(
          {
            firstName: u.firstName ?? '',
            lastName: u.lastName ?? '',
            email: u.email ?? '',
            phone: u.phone ?? '',
            companyName: u.companyName ?? '',
            city: u.city ?? '',
            country: u.country ?? '',
            bio: u.bio ?? ''
          },
          { emitEvent: false }
        );
      },
      error: err => {
        this.loading.set(false);
        const message = err?.error?.message || 'Impossible de charger votre profil.';
        this.loadError.set(message);
      }
    });
  }

  private loadFreelancerProfile(): void {
    this.loading.set(true);
    this.loadError.set(null);
    this.feedbackSummary.set(null);
    this.freelancerApi.getMe().subscribe({
      next: res => {
        this.loading.set(false);
        if (!res?.success || !res.data) {
          this.loadError.set('Impossible de charger votre profil freelance.');
          return;
        }

        const f = res.data;
        this._freelancerDto.set(f);
        if (f.id != null) {
          this.loadFreelancerFeedbacks(f.id);
        }
        this.freelancerForm.reset(
          {
            firstName: f.firstName ?? '',
            lastName: f.lastName ?? '',
            email: f.email ?? '',
            phone: f.phone ?? '',
            title: f.title ?? '',
            city: f.city ?? '',
            country: f.country ?? '',
            hourlyRate: f.hourlyRate ?? null,
            dailyRate: f.dailyRate ?? null,
            availability: f.availability ?? '',
            skills: (f.skills ?? []).join(', '),
            bio: f.bio ?? '',
            linkedinUrl: f.linkedinUrl ?? '',
            githubUrl: f.githubUrl ?? '',
            portfolioUrl: f.portfolioUrl ?? ''
          },
          { emitEvent: false }
        );
        this.freelancerForm.markAsPristine();
      },
      error: err => {
        this.loading.set(false);
        const message = err?.error?.message || 'Impossible de charger votre profil freelance.';
        this.loadError.set(message);
      }
    });
  }

  submitProfile(): void {
    if (this.isFreelancer()) {
      this.submitFreelancerProfile();
      return;
    }
    this.submitClientProfile();
  }

  private submitClientProfile(): void {
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }
    if (!this.profileForm.dirty) {
      this.showFeedback('profile', 'Aucune modification a enregistrer pour le moment.');
      return;
    }

    const f = this.profileForm.getRawValue();
    const payload: UpdateClientProfileRequest = {
      firstName: this.normalizeString(f.firstName),
      lastName: this.normalizeString(f.lastName),
      phone: this.normalizeString(f.phone),
      companyName: this.normalizeString(f.companyName),
      city: this.normalizeString(f.city),
      country: this.normalizeString(f.country),
      bio: this.normalizeString(f.bio)
    };

    this.api.updateMyProfile(payload).subscribe({
      next: res => {
        if (res?.success && res.data) {
          const u = res.data;
          this._clientDto.set(u);
          this._user.set({
            fullName: `${u.firstName ?? ''} ${u.lastName ?? ''}`.trim() || 'Profil',
            role: 'Client',
            email: u.email,
            phone: u.phone ?? '',
            companyName: u.companyName ?? '',
            city: u.city ?? '',
            country: u.country ?? '',
            bio: u.bio ?? '',
            updatedAt: u.updatedAt ?? new Date().toISOString(),
            status: u.isVerified ? 'verified' : 'pending'
          });
          this.profileForm.markAsPristine();
          this.showFeedback('profile', 'Vos informations ont ete mises a jour avec succes.');
        } else {
          this.showFeedback('profile', res?.message || 'Mise a jour du profil impossible.');
        }
      },
      error: err => this.showFeedback('profile', err?.error?.message || 'Erreur lors de la mise a jour.')
    });
  }

  submitFreelancerProfile(): void {
    if (this.freelancerForm.invalid) {
      this.freelancerForm.markAllAsTouched();
      return;
    }
    if (!this.freelancerForm.dirty) {
      this.snackBar.open('Aucune modification a enregistrer pour le moment.', 'Fermer', {
        duration: 2600
      });
      return;
    }

    const f = this.freelancerForm.getRawValue();
    const payload: UpdateFreelancerProfileRequest = {
      firstName: this.normalizeString(f.firstName),
      lastName: this.normalizeString(f.lastName),
      phone: this.normalizeString(f.phone),
      title: this.normalizeString(f.title),
      city: this.normalizeString(f.city),
      country: this.normalizeString(f.country),
      hourlyRate: f.hourlyRate != null ? Number(f.hourlyRate) : null,
      dailyRate: f.dailyRate != null ? Number(f.dailyRate) : null,
      availability: this.normalizeString(f.availability),
      skills: this.parseSkills(f.skills),
      bio: this.normalizeString(f.bio),
      linkedinUrl: this.normalizeString(f.linkedinUrl),
      githubUrl: this.normalizeString(f.githubUrl),
      portfolioUrl: this.normalizeString(f.portfolioUrl)
    };

    this.savingFreelancer.set(true);
    this.freelancerApi.updateMyProfile(payload).subscribe({
      next: res => {
        this.savingFreelancer.set(false);
        if (res?.success && res.data) {
          this._freelancerDto.set(res.data);
          this.snackBar.open('Profil mis a jour avec succes.', 'Fermer', { duration: 2600 });
          this.freelancerForm.reset(
            {
              firstName: res.data.firstName ?? '',
              lastName: res.data.lastName ?? '',
              email: res.data.email ?? '',
              phone: res.data.phone ?? '',
              title: res.data.title ?? '',
              city: res.data.city ?? '',
              country: res.data.country ?? '',
              hourlyRate: res.data.hourlyRate ?? null,
              dailyRate: res.data.dailyRate ?? null,
              availability: res.data.availability ?? '',
              skills: (res.data.skills ?? []).join(', '),
              bio: res.data.bio ?? '',
              linkedinUrl: res.data.linkedinUrl ?? '',
              githubUrl: res.data.githubUrl ?? '',
              portfolioUrl: res.data.portfolioUrl ?? ''
            },
            { emitEvent: false }
          );
          this.freelancerForm.markAsPristine();
        } else {
          this.snackBar.open(res?.message || 'Mise a jour du profil impossible.', 'Fermer', {
            duration: 2600
          });
        }
      },
      error: err => {
        this.savingFreelancer.set(false);
        const message = err?.error?.message || 'Erreur lors de la mise a jour du profil.';
        this.snackBar.open(message, 'Fermer', { duration: 2600 });
      }
    });
  }

  onResumeSelected(event: Event): void {
    const input = event.target as HTMLInputElement | null;
    const file = input?.files?.[0] ?? null;
    this.resumeFile.set(file);
    this.resumeError.set(null);
    this.resumeResult.set(null);
  }

  extractResume(): void {
    const file = this.resumeFile();
    if (!file) {
      this.resumeError.set('Please select a PDF file first.');
      return;
    }
    const freelancerId = this._freelancerDto()?.id ?? null;
    this.resumeLoading.set(true);
    this.resumeError.set(null);
    this.aiService.extractResume(file, freelancerId ?? undefined, 'fr').subscribe({
      next: res => {
        this.resumeLoading.set(false);
        this.resumeResult.set(res ?? null);
        const skills = (res?.skills ?? []).map(s => s.name).filter(Boolean);
        if (skills.length) {
          const merged = this.mergeSkills(this.freelancerForm.controls.skills.value, skills);
          this.freelancerForm.controls.skills.setValue(merged.join(', '));
          this.freelancerForm.markAsDirty();
        }
      },
      error: err => {
        this.resumeLoading.set(false);
        this.resumeError.set(err?.error?.message || err?.message || 'Resume extraction failed.');
      }
    });
  }

  hasError(control: AbstractControl | null, code: string) {
    return !!control && control.hasError(code) && (control.dirty || control.touched);
  }

  passwordMismatch(): boolean {
    return !!this.passwordForm.touched && this.passwordForm.hasError('passwordMismatch');
  }

  submitPassword(): void {
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }
    this.passwordForm.reset({ currentPassword: '', newPassword: '', confirmPassword: '' });
    this.showFeedback('password', 'Votre mot de passe a ete mis a jour.');
  }

  private passwordsMatch(group: AbstractControl): ValidationErrors | null {
    const a = group.get('newPassword')?.value;
    const b = group.get('confirmPassword')?.value;
    if (!a || !b) return null;
    return a === b ? null : { passwordMismatch: true };
  }

  private parseSkills(input?: string | null): string[] {
    if (!input) {
      return [];
    }
    return input
      .split(',')
      .map(skill => skill.trim())
      .filter(Boolean);
  }

  private mergeSkills(existing: string, incoming: string[]): string[] {
    const current = this.parseSkills(existing);
    const merged = new Set<string>([...current, ...incoming.map(s => s.trim()).filter(Boolean)]);
    return Array.from(merged);
  }

  private initialsFrom(name: string) {
    return name
      .split(' ')
      .filter(Boolean)
      .map(part => part[0]!.toUpperCase())
      .slice(0, 2)
      .join('');
  }

  freelancerLocation() {
    const f = this.freelancer();
    const loc = [f.city, f.country].filter(Boolean).join(', ');
    return loc || 'A renseigner';
  }

  private normalizeString(value?: string | null): string | undefined {
    const trimmed = (value ?? '').trim();
    return trimmed.length ? trimmed : undefined;
  }

  private loadFreelancerFeedbacks(userId: number) {
    if (!userId) return;
    this.feedbackSummary.set(null);
    this.freelancerFeedbacks.set([]);
    this.feedbackService.getFreelancerFeedbacks(userId).subscribe({
      next: res => {
        if (res?.success && res.data) {
          this.feedbackSummary.set(res.data.summary ?? null);
          this.freelancerFeedbacks.set(res.data.feedbacks ?? []);
        } else {
          this.feedbackSummary.set(null);
          this.freelancerFeedbacks.set([]);
        }
      },
      error: () => {
        this.feedbackSummary.set(null);
        this.freelancerFeedbacks.set([]);
      }
    });
  }

  private formatDate(iso: string) {
    const d = new Date(iso);
    return new Intl.DateTimeFormat('fr-FR', { dateStyle: 'long', timeStyle: 'short' }).format(d);
  }

  private showFeedback(type: FeedbackMessage['type'], message: string) {
    this.feedback.set({ type, message });
    if (this.feedbackTimer) clearTimeout(this.feedbackTimer);
    this.feedbackTimer = setTimeout(() => {
      this.feedback.set(null);
      this.feedbackTimer = null;
    }, 4000);
  }

  private loadFeedbackSummary(userId: number) {
    if (!userId) return;
    this.feedbackService.getUserFeedbackSummary(userId).subscribe({
      next: res => {
        if (res?.success && res.data) {
          this.feedbackSummary.set({
            averageRating: res.data.averageRating ?? 0,
            count: res.data.count ?? 0
          });
        } else {
          this.feedbackSummary.set(null);
        }
      },
      error: () => this.feedbackSummary.set(null)
    });
  }

  logout() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
