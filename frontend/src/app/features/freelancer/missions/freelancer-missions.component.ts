import { CommonModule } from '@angular/common';
import { Component, DestroyRef, Input, OnInit, computed, inject, signal } from '@angular/core';
import { RouterModule } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatMenuModule } from '@angular/material/menu';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatBadgeModule } from '@angular/material/badge';
import { MatDividerModule } from '@angular/material/divider';
import { MatCardModule } from '@angular/material/card';

import {
  FreelancerMilestoneDto,
  FreelancerMissionWithMilestonesDto,
  MissionService
} from '../../../services/mission.service';
import { MilestoneService } from '../../../services/milestone.service';
import { AuthService } from '../../../services/auth.service';

@Component({
  standalone: true,
  selector: 'app-freelancer-missions',
  templateUrl: './freelancer-missions.component.html',
  styleUrls: ['./freelancer-missions.component.scss'],
  imports: [
    CommonModule,
    RouterModule,
    // navbar
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatSlideToggleModule,
    MatBadgeModule,
    MatDividerModule,
    // contenu
    MatCardModule,
    MatChipsModule,
    MatProgressBarModule,
    MatSnackBarModule
  ]
})
export default class FreelancerMissionsComponent implements OnInit {
  @Input() role: 'CLIENT' | 'FREELANCER' = 'FREELANCER';

  private readonly missionService = inject(MissionService);
  private readonly milestoneService = inject(MilestoneService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);
  private readonly authService = inject(AuthService);

  missions = signal<FreelancerMissionWithMilestonesDto[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);
  totalMissions = computed(() => this.missions().length);
  private readonly actionLoading = signal<Record<number, boolean>>({});
  private readonly missionActionLoading = signal<Record<number, boolean>>({});

  ngOnInit() {
    this.restoreDarkMode();
    this.loadMissions();
  }

  get isFreelancer() {
    return this.role === 'FREELANCER';
  }

  get isClient() {
    return this.role === 'CLIENT';
  }

  // Button is shown only when every milestone is already paid.
  canCompleteMission(mission: FreelancerMissionWithMilestonesDto | null | undefined) {
    if (!mission || !mission.milestones?.length) {
      return false;
    }
    const status = (mission.status || '').toUpperCase();
    if (status === 'COMPLETED' || status === 'PENDING_CLOSURE') {
      return false;
    }
    return mission.milestones.every(m => (m.status || '').toUpperCase() === 'PAID');
  }

  isCompletingMission(missionId?: number | null) {
    if (!missionId) {
      return false;
    }
    return !!this.missionActionLoading()[missionId];
  }

  // Backend also checks the authenticated freelancer matches the mission before switching to COMPLETED.
  onCompleteMission(mission: FreelancerMissionWithMilestonesDto) {
    if (!mission?.id) {
      return;
    }
    this.setMissionActionLoading(mission.id, true);
    this.missionService
      .completeMission(mission.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.setMissionActionLoading(mission.id, false);
          const nextStatus = ((res as any)?.data?.status || (res as any)?.status || 'COMPLETED') as string;
          this.updateMissionStatus(mission.id, nextStatus);
          this.snackBar.open('Mission terminee.', 'Fermer', { duration: 2400 });
        },
        error: err => {
          this.setMissionActionLoading(mission.id, false);
          const message = err?.error?.message || err?.message || 'Impossible de terminer la mission.';
          this.snackBar.open(message, 'Fermer', { duration: 2800 });
        }
      });
  }

  private loadMissions() {
    this.loading.set(true);
    this.error.set(null);

    this.missionService
      .getFreelancerMissions()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.loading.set(false);
          if (!res.success || !Array.isArray(res.data)) {
            this.error.set(res.message || 'Impossible de charger vos missions.');
            this.missions.set([]);
            return;
          }
          this.missions.set(res.data);
        },
        error: err => {
          this.loading.set(false);
          const message =
            err?.error?.message || err?.message || 'Erreur reseau.';
          this.error.set(message);
          this.missions.set([]);
          this.snackBar.open(message, 'Fermer', { duration: 2500 });
        }
      });
  }

  /* ---------------- Dark mode simple ---------------- */

  toggleDark(enabled: boolean) {
    const root = document.documentElement;
    if (enabled) {
      root.classList.add('tw-dark');
      localStorage.setItem('tw-dark', '1');
    } else {
      root.classList.remove('tw-dark');
      localStorage.setItem('tw-dark', '0');
    }
  }

  private restoreDarkMode() {
    const stored = localStorage.getItem('tw-dark');
    if (stored === '1') {
      document.documentElement.classList.add('tw-dark');
    }
  }

  /* ---------------- Logout ---------------- */

  logout() {
    this.authService.logout();
  }

  canUpload(milestone: FreelancerMilestoneDto | null | undefined) {
    const status = (milestone?.status || '').toUpperCase();
    return ['DRAFT', 'IN_PROGRESS', 'REJECTED', 'PENDING'].includes(status);
  }

  triggerFileInput(input: HTMLInputElement | null) {
    input?.click();
  }

  onFileSelected(event: Event, milestone: FreelancerMilestoneDto) {
    const input = event.target as HTMLInputElement;
    const file = input?.files?.[0] || null;
    input.value = '';
    if (!file || !milestone?.id) {
      return;
    }

    this.setActionLoading(milestone.id, true);
    this.milestoneService
      .uploadDeliverable(milestone.id, file, 'Livraison du jalon')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.setActionLoading(milestone.id, false);
          const updated = this.extractMilestone(res);
          if (!updated) {
            this.snackBar.open('Reponse invalide du serveur.', 'Fermer', {
              duration: 2400
            });
            return;
          }
          this.replaceMilestone(updated);
          this.snackBar.open('Livrable envoye.', 'Fermer', { duration: 2200 });
        },
        error: err => {
          this.setActionLoading(milestone.id, false);
          const message = err?.error?.message || err?.message || 'Envoi impossible.';
          this.snackBar.open(message, 'Fermer', { duration: 2600 });
        }
      });
  }

  acceptMilestone(milestone: FreelancerMilestoneDto) {
    if (!milestone?.id) {
      return;
    }
    this.setActionLoading(milestone.id, true);
    this.milestoneService
      .accept(milestone.id, 'OK')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.setActionLoading(milestone.id, false);
          const updated = this.extractMilestone(res);
          if (!updated) {
            this.snackBar.open('Reponse invalide du serveur.', 'Fermer', {
              duration: 2400
            });
            return;
          }
          this.replaceMilestone(updated);
          this.snackBar.open('Jalon accepte.', 'Fermer', { duration: 2200 });
        },
        error: err => {
          this.setActionLoading(milestone.id, false);
          const message = err?.error?.message || err?.message || 'Validation impossible.';
          this.snackBar.open(message, 'Fermer', { duration: 2600 });
        }
      });
  }

  rejectMilestone(milestone: FreelancerMilestoneDto) {
    if (!milestone?.id) {
      return;
    }
    const reason = prompt('Pourquoi rejeter ce jalon ?', 'Livrable incomplet') || 'Livrable incomplet';

    this.setActionLoading(milestone.id, true);
    this.milestoneService
      .reject(milestone.id, reason)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.setActionLoading(milestone.id, false);
          const updated = this.extractMilestone(res);
          if (!updated) {
            this.snackBar.open('Reponse invalide du serveur.', 'Fermer', {
              duration: 2400
            });
            return;
          }
          this.replaceMilestone(updated);
          this.snackBar.open('Jalon rejete.', 'Fermer', { duration: 2200 });
        },
        error: err => {
          this.setActionLoading(milestone.id, false);
          const message = err?.error?.message || err?.message || 'Action impossible.';
          this.snackBar.open(message, 'Fermer', { duration: 2600 });
        }
      });
  }

  /* ---------------- Statuts pour les chips ---------------- */

  missionStatusLabel(status?: string | null) {
    switch ((status || '').toUpperCase()) {
      case 'IN_PROGRESS':
        return 'EN COURS';
      case 'PENDING_CLOSURE':
        return 'EN ATTENTE CLOTURE CLIENT';
      case 'COMPLETED':
        return 'TERMINEE';
      default:
        return this.statusLabel(status);
    }
  }

  statusLabel(status?: string | null) {
    switch ((status || '').toUpperCase()) {
      case 'DRAFT':
        return 'DRAFT';
      case 'PENDING':
      case 'PUBLISHED':
        return 'EN ATTENTE';
      case 'IN_PROGRESS':
        return 'EN COURS';
      case 'PENDING_CLOSURE':
        return 'EN ATTENTE CLOTURE';
      case 'SUBMITTED':
        return 'EN REVUE';
      case 'VALIDATED':
      case 'COMPLETED':
        return 'VALIDE';
      case 'REJECTED':
        return 'REJETE';
      case 'PAID':
        return 'PAYE';
      case 'CANCELLED':
        return 'ANNULEE';
      default:
        return status || 'N/A';
    }
  }

  statusColor(status?: string | null) {
    switch ((status || '').toUpperCase()) {
      case 'IN_PROGRESS':
      case 'PENDING_CLOSURE':
      case 'VALIDATED':
      case 'COMPLETED':
      case 'PAID':
        return 'primary';
      case 'SUBMITTED':
        return 'accent';
      case 'REJECTED':
      case 'CANCELLED':
        return 'warn';
      default:
        return undefined;
    }
  }

  isProcessing(milestoneId?: number | null) {
    if (!milestoneId) {
      return false;
    }
    return !!this.actionLoading()[milestoneId];
  }

  private setActionLoading(milestoneId: number, loading: boolean) {
    this.actionLoading.update(state => ({ ...state, [milestoneId]: loading }));
  }

  private setMissionActionLoading(missionId: number, loading: boolean) {
    this.missionActionLoading.update(state => ({ ...state, [missionId]: loading }));
  }

  private updateMissionStatus(missionId: number, status: string) {
    const normalized = (status || '').toUpperCase();
    this.missions.update(missions =>
      missions.map(m => (m.id === missionId ? { ...m, status: normalized || m.status } : m))
    );
  }

  private extractMilestone(res: any): FreelancerMilestoneDto | null {
    if (!res) {
      return null;
    }
    if ('data' in res && res.data) {
      return res.data as FreelancerMilestoneDto;
    }
    return res as FreelancerMilestoneDto;
  }

  private replaceMilestone(updated: FreelancerMilestoneDto) {
    if (!updated?.id) {
      return;
    }
    this.missions.update(missions =>
      missions.map(mission => {
        if (!mission.milestones?.some(m => m.id === updated.id)) {
          return mission;
        }
        const milestones = mission.milestones.map(m =>
          m.id === updated.id ? { ...m, ...updated } : m
        );
        return { ...mission, milestones };
      })
    );
  }
}
