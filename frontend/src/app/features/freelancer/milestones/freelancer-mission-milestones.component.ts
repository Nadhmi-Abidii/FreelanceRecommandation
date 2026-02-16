import { CommonModule } from '@angular/common';
import { Component, DestroyRef, Input, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { MilestoneDto, MilestoneService } from '../../../services/milestone.service';

@Component({
  standalone: true,
  selector: 'app-freelancer-mission-milestones',
  templateUrl: './freelancer-mission-milestones.component.html',
  styleUrls: ['./freelancer-mission-milestones.component.scss'],
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatIconModule,
    MatProgressBarModule,
    MatSnackBarModule
  ]
})
export default class FreelancerMissionMilestonesComponent implements OnInit {
  @Input() missionId: number | null | undefined;

  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(MilestoneService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);

  readonly resolvedMissionId = signal<number | null>(null);
  readonly milestones = signal<MilestoneDto[]>([]);
  readonly loading = signal(false);
  readonly actionLoading = signal(false);
  readonly error = signal<string | null>(null);

  ngOnInit() {
    const idFromInput = this.missionId ?? null;
    const idFromRoute = Number(
      this.route.snapshot.paramMap.get('missionId') ?? this.route.snapshot.paramMap.get('id')
    );
    const missionId = idFromInput || idFromRoute || null;
    this.resolvedMissionId.set(missionId);
    if (missionId) {
      this.load(missionId);
    }
  }

  load(missionId: number) {
    this.loading.set(true);
    this.error.set(null);

    this.service
      .listForFreelancer(missionId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.loading.set(false);
          if (!res.success || !Array.isArray(res.data)) {
            this.milestones.set([]);
            this.error.set(res.message || 'Impossible de charger les jalons.');
            return;
          }
          this.milestones.set(res.data);
        },
        error: err => {
          this.loading.set(false);
          const message = err?.error?.message || err?.message || 'Erreur reseau.';
          this.error.set(message);
          this.milestones.set([]);
        }
      });
  }

  deliver(id: number) {
    const note = prompt('Lien ou note pour ce jalon (optionnel) ?') || undefined;
    this.runAction(this.service.deliver(id, note), 'Jalon livre.');
  }

  statusLabel(status?: string | null) {
    switch ((status || '').toUpperCase()) {
      case 'PENDING':
        return 'Cree';
      case 'IN_PROGRESS':
        return 'En cours';
      case 'SUBMITTED':
        return 'Soumis';
      case 'VALIDATED':
        return 'Valide';
      case 'REJECTED':
        return 'Refuse';
      case 'PAID':
        return 'Paye';
      default:
        return 'Cree';
    }
  }

  statusColor(status?: string | null) {
    switch ((status || '').toUpperCase()) {
      case 'VALIDATED':
      case 'PAID':
        return 'primary';
      case 'REJECTED':
        return 'warn';
      case 'SUBMITTED':
        return 'accent';
      default:
        return undefined;
    }
  }

  canDeliver(status?: string | null) {
    const normalized = (status || '').toUpperCase();
    return normalized === 'PENDING' || normalized === 'IN_PROGRESS';
  }

  private runAction(observable: any, successMessage: string) {
    const missionId = this.resolvedMissionId();
    if (!missionId) {
      return;
    }
    this.actionLoading.set(true);
    observable.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        this.actionLoading.set(false);
        if (!res?.success) {
          this.snackBar.open(res?.message || 'Action impossible.', 'Fermer', { duration: 2400 });
          return;
        }
        this.snackBar.open(successMessage, 'Fermer', { duration: 2000 });
        this.load(missionId);
      },
      error: (err: any) => {
        this.actionLoading.set(false);
        const message = err?.error?.message || err?.message || 'Action impossible.';
        this.snackBar.open(message, 'Fermer', { duration: 2600 });
      }
    });
  }
}
