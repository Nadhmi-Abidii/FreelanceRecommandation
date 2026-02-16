import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { map, startWith } from 'rxjs';

import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatMenuModule } from '@angular/material/menu';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatBadgeModule } from '@angular/material/badge';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { AuthService } from '../../core/auth/auth';
import { FreelancerMission } from '../../models/freelancer-mission.model';
import { FreelancerMissionService, MissionStatus } from '../../services/freelancer-mission.service';

interface FilterState {
  search: string;
  status: 'all' | MissionStatus;
  urgency: 'all' | 'STANDARD' | 'URGENT';
  sort: 'newest' | 'deadline' | 'budget';
}

@Component({
  standalone: true,
  selector: 'app-freelancer-dashboard',
  templateUrl: './freelancer-dashboard.component.html',
  styleUrls: ['./freelancer-dashboard.component.scss'],
  imports: [
    CommonModule,
    ReactiveFormsModule,

    // Router directives pour routerLink / routerLinkActive
    RouterLink,
    RouterLinkActive,

    // Angular Material
    MatToolbarModule,
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatDividerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatMenuModule,
    MatSlideToggleModule,
    MatBadgeModule,
    MatProgressBarModule
  ]
})
export default class FreelancerDashboardComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly missionsService = inject(FreelancerMissionService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly auth = inject(AuthService);

  readonly user = this.auth.currentUser;
  readonly missions = this.missionsService.missions;
  readonly loading = this.missionsService.loading;
  readonly error = this.missionsService.error;

  readonly filterForm = this.fb.nonNullable.group({
    search: [''],
    status: ['all' as FilterState['status']],
    urgency: ['all' as FilterState['urgency']],
    sort: ['newest' as FilterState['sort']]
  });

  private readonly filterState = signal<FilterState>(this.filterForm.getRawValue());

  readonly statusFilters = [
    { value: 'all' as const, label: 'Toutes les missions' },
    { value: 'OPEN' as const, label: 'Nouvelles' },
    { value: 'APPLIED' as const, label: 'Candidatures envoyÃ©es' },
    { value: 'INTERVIEWING' as const, label: 'Entretiens' },
    { value: 'IN_PROGRESS' as const, label: 'En cours' }
  ];

  readonly urgencyFilters = [
    { value: 'all' as const, label: 'Toutes les urgences' },
    { value: 'URGENT' as const, label: 'PrioritÃ© haute' },
    { value: 'STANDARD' as const, label: 'Standard' }
  ];

  readonly sortOptions = [
    { value: 'newest' as const, label: 'Les plus rÃ©centes' },
    { value: 'deadline' as const, label: 'Date limite la plus proche' },
    { value: 'budget' as const, label: 'Budget dÃ©croissant' }
  ];

  constructor() {
    this.filterForm.valueChanges
      .pipe(
        startWith(this.filterForm.getRawValue()),
        map(value => ({
          search: (value.search ?? '').trim(),
          status: (value.status ?? 'all') as FilterState['status'],
          urgency: (value.urgency ?? 'all') as FilterState['urgency'],
          sort: (value.sort ?? 'newest') as FilterState['sort']
        })),
        takeUntilDestroyed()
      )
      .subscribe(value => this.filterState.set(value));
  }

  ngOnInit() {
    this.missionsService.loadMissions();
  }

  readonly filteredMissions = computed(() => {
    const filter = this.filterState();
    let missions = [...this.missions()];

    missions = missions.filter(mission => mission.status !== 'IN_PROGRESS' || !!mission.appliedAt);

    if (filter.search) {
      const searchLower = filter.search.toLowerCase();
      missions = missions.filter(mission =>
        mission.title.toLowerCase().includes(searchLower) ||
        mission.clientName.toLowerCase().includes(searchLower) ||
        mission.summary.toLowerCase().includes(searchLower) ||
        mission.tags.some(tag => tag.toLowerCase().includes(searchLower))
      );
    }

    if (filter.status !== 'all') {
      missions = missions.filter(mission => mission.status === filter.status);
    }

    if (filter.urgency !== 'all') {
      missions = missions.filter(mission => mission.urgency === filter.urgency);
    }

    missions.sort((a, b) => {
      switch (filter.sort) {
        case 'deadline':
          return this.getDaysRemaining(a.deadline) - this.getDaysRemaining(b.deadline);
        case 'budget':
          return this.extractBudget(b.budget) - this.extractBudget(a.budget);
        case 'newest':
        default:
          return new Date(b.postedAt).getTime() - new Date(a.postedAt).getTime();
      }
    });

    return missions;
  });

  readonly metrics = computed(() => {
    const missions = this.missions();
    const urgent = missions.filter(mission => mission.urgency === 'URGENT').length;
    const interviewing = missions.filter(mission => mission.status === 'INTERVIEWING').length;
    const applied = missions.filter(mission => mission.status === 'APPLIED').length;

    return {
      total: missions.length,
      urgent,
      interviewing,
      applied
    };
  });

  readonly hasResults = computed(() => this.filteredMissions().length > 0);

  trackMission(_: number, mission: FreelancerMission) {
    return mission.id;
  }

  openMission(mission: FreelancerMission) {
    this.router.navigate(['/freelancer/missions', mission.id]);
  }

  toggleSaved(mission: FreelancerMission) {
    const updated = this.missionsService.toggleSaved(mission.id);
    if (!updated) {
      return;
    }

    this.snackBar.open(
      updated.isSaved ? 'Mission ajoutÃ©e Ã  vos favoris' : 'Mission retirÃ©e des favoris',
      'Fermer',
      { duration: 2600 }
    );
  }

  applyToMission(mission: FreelancerMission) {
    const freelancerId = this.auth.currentUser()?.userId;
    if (!freelancerId) {
      this.snackBar.open('Connectez-vous comme freelancer pour postuler.', 'Fermer', { duration: 2200 });
      return;
    }

    const coverLetter = (prompt('Message de candidature (lettre) ?') || '').trim();
    if (!coverLetter) {
      this.snackBar.open('Message requis pour envoyer la candidature.', 'Fermer', { duration: 2200 });
      return;
    }

    const priceInput = (prompt('Budget propose pour la mission (EUR) ?') || '').trim();
    const proposedPrice = Number(priceInput);
    if (!proposedPrice || proposedPrice <= 0) {
      this.snackBar.open('Indiquez un budget valide pour valider la candidature.', 'Fermer', { duration: 2200 });
      return;
    }

    const durationInput = (prompt('Duree estimee (en jours) ? Optionnel') || '').trim();
    const proposedDuration = durationInput ? Number(durationInput) : undefined;

    this.missionsService.createCandidature({
      freelancerId,
      missionId: mission.id,
      coverLetter,
      proposedPrice,
      proposedDuration
    }).subscribe({
      next: () => {
        this.snackBar.open('Candidature envoyee au client', 'Fermer', { duration: 2400 });
      },
      error: err => {
        this.snackBar.open(err?.message || 'Envoi impossible.', 'Fermer', { duration: 2400 });
      }
    });
  }

  getStatusLabel(status: MissionStatus) {
    return this.missionsService.getStatusLabel(status);
  }

  getUrgencyLabel(urgency: 'STANDARD' | 'URGENT') {
    return this.missionsService.getUrgencyLabel(urgency);
  }

  getTimeAgo(isoDate: string) {
    const now = new Date();
    const past = new Date(isoDate);
    const diffMs = now.getTime() - past.getTime();
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

    if (diffDays <= 0) {
      const diffHours = Math.max(1, Math.floor(diffMs / (1000 * 60 * 60)));
      return `PubliÃ© il y a ${diffHours} h`;
    }

    if (diffDays === 1) {
      return 'PubliÃ© hier';
    }

    if (diffDays < 7) {
      return `PubliÃ© il y a ${diffDays} jours`;
    }

    const diffWeeks = Math.floor(diffDays / 7);
    if (diffWeeks === 1) {
      return 'PubliÃ© il y a 1 semaine';
    }

    return `PubliÃ© il y a ${diffWeeks} semaines`;
  }

  getDaysRemaining(deadline: string) {
    const today = new Date();
    const endDate = new Date(deadline);
    const diff = Math.ceil((endDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
    return diff;
  }

  getDaysRemainingLabel(deadline: string) {
    const daysLeft = this.getDaysRemaining(deadline);
    if (daysLeft < 0) {
      return 'Date limite dÃ©passÃ©e';
    }

    if (daysLeft === 0) {
      return 'Dernier jour pour candidater';
    }

    if (daysLeft === 1) {
      return '1 jour restant';
    }

    return `${daysLeft} jours restants`;
  }

  private extractBudget(budget: string) {
    const match = budget.replace(/[^0-9]/g, '');
    return Number.parseInt(match || '0', 10);
  }

  reload() {
    this.missionsService.loadMissions(true);
  }

  toggleDark(enabled: boolean) {
    document.documentElement.classList.toggle('freelancer-dark-mode', enabled);
  }

  logout() {
  this.auth.logout();          // supprime token + reset user/role
  this.router.navigate(['/login']);
}
}






