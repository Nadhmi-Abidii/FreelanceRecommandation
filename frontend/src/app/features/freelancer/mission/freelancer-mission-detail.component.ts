import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal, DestroyRef } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';

import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { FreelancerMission } from '../../../models/freelancer-mission.model';
import { FreelancerMissionService, MissionStatus } from '../../../services/freelancer-mission.service';
import FreelancerMissionMilestonesComponent from '../milestones/freelancer-mission-milestones.component';
import MissionFeedbackComponent from '../../feedback/mission-feedback/mission-feedback.component';

@Component({
  standalone: true,
  selector: 'app-freelancer-mission-detail',
  templateUrl: './freelancer-mission-detail.component.html',
  styleUrls: ['./freelancer-mission-detail.component.scss'],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatDividerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSnackBarModule,
    FreelancerMissionMilestonesComponent,
    MissionFeedbackComponent
  ]
})
export default class FreelancerMissionDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);
  private readonly missionService = inject(FreelancerMissionService);
  private readonly destroyRef = inject(DestroyRef);

  readonly mission = signal<FreelancerMission | null>(null);
  readonly submittingFinal = signal(false);
  readonly messageForm = this.fb.nonNullable.group({
    message: ['', [Validators.required, Validators.minLength(6)]],
    resumeUrl: ['', [Validators.pattern(/^https?:\/\/.+$/i)]],
    proposedPrice: [null as number | null, [Validators.required, Validators.min(1)]],
    proposedDuration: [null as number | null, [Validators.min(1)]]
  });

  readonly conversation = computed(() => this.mission()?.conversation ?? []);
  readonly statusLabel = computed(() =>
    this.mission() ? this.missionService.getStatusLabel(this.mission()!.status) : ''
  );
  readonly urgencyLabel = computed(() =>
    this.mission() ? this.missionService.getUrgencyLabel(this.mission()!.urgency) : ''
  );
  readonly backendStatus = computed(() => (this.mission()?.backendStatus || this.mission()?.status || '').toString().toUpperCase());
  readonly canSubmitFinal = computed(() => this.backendStatus() === 'IN_PROGRESS');
  readonly isCompleted = computed(() => {
    const status = (this.mission()?.status || '').toString().toUpperCase();
    return status === 'COMPLETED' || this.backendStatus() === 'COMPLETED';
  });

  constructor() {
    this.route.paramMap
      .pipe(
        map(params => Number(params.get('id'))),
        takeUntilDestroyed()
      )
      .subscribe(id => this.loadMission(id));
  }

  getDaysRemaining(deadline: string) {
    const today = new Date();
    const endDate = new Date(deadline);
    return Math.ceil((endDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
  }

  getDeadlineLabel(deadline: string) {
    const remaining = this.getDaysRemaining(deadline);
    if (remaining < 0) {
      return 'Date limite depassee';
    }
    if (remaining === 0) {
      return 'Dernier jour pour candidater';
    }
    if (remaining === 1) {
      return "Plus qu'un jour pour postuler";
    }
    return `${remaining} jours restants`;
  }

  goBack() {
    this.router.navigate(['/freelancer/dashboard']);
  }

  toggleSaved() {
    const mission = this.mission();
    if (!mission) return;
    const updated = this.missionService.toggleSaved(mission.id);
    if (!updated) return;
    this.mission.set(updated);
    this.snackBar.open(
      updated.isSaved ? 'Mission ajoutee a vos favoris' : 'Mission retiree des favoris',
      'Fermer',
      { duration: 2400 }
    );
  }

  markAsApplied() {
    const mission = this.mission();
    if (!mission) return;
    const updated = this.missionService.markAsApplied(mission.id);
    if (!updated) return;
    this.mission.set(updated);
    this.snackBar.open('Candidature marquee comme envoyee', 'Fermer', { duration: 2400 });
  }

  submitMessage() {
    if (this.messageForm.invalid || !this.mission()) {
      this.messageForm.markAllAsTouched();
      return;
    }

    const missionId = this.mission()!.id;
    const message = this.messageForm.controls.message.value;
    const resumeUrl = this.messageForm.controls.resumeUrl.value || undefined;
    const proposedPrice = this.messageForm.controls.proposedPrice.value;
    const proposedDuration = this.messageForm.controls.proposedDuration.value || undefined;

    if (!proposedPrice || proposedPrice <= 0) {
      this.messageForm.controls.proposedPrice.setErrors({ required: true });
      return;
    }

    this.missionService
      .sendMessage(missionId, message, resumeUrl, { proposedPrice, proposedDuration })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: updated => {
          if (updated) {
            this.mission.set(updated);
          }
          this.messageForm.reset({ message: '', resumeUrl: '', proposedPrice: null, proposedDuration: null });
          this.snackBar.open('Message envoyee au client', 'Fermer', { duration: 2200 });
        },
        error: err => {
          const text = err?.message || 'Impossible denvoyer la candidature';
          this.snackBar.open(text, 'Fermer', { duration: 2600 });
        }
      });
  }

  getStatusTone(status: MissionStatus) {
    switch (status) {
      case 'OPEN':
        return 'status-open';
      case 'APPLIED':
        return 'status-applied';
      case 'INTERVIEWING':
        return 'status-interviewing';
      case 'PENDING_CLOSURE':
      case 'IN_PROGRESS':
        return 'status-progress';
      default:
        return '';
    }
  }

  private loadMission(id: number) {
    if (!id) {
      this.mission.set(null);
      return;
    }

    const mission = this.missionService.getMissionById(id);
    if (mission) {
      this.mission.set(mission);
      return;
    }

    this.missionService
      .fetchMission(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: fetched => this.mission.set(fetched),
        error: () => this.mission.set(null)
      });
  }

  completeMission() {
    this.submitFinalDelivery();
  }

  submitFinalDelivery() {
    const missionId = this.mission()?.id;
    if (!missionId || this.submittingFinal()) return;

    this.submittingFinal.set(true);
    this.missionService
      .submitFinalDelivery(missionId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res: any) => {
          this.submittingFinal.set(false);
          if (res?.success) {
            const current = this.mission();
            if (current) {
              this.mission.set({
                ...current,
                status: 'PENDING_CLOSURE' as MissionStatus,
                backendStatus: 'PENDING_CLOSURE'
              });
            }
            this.snackBar.open('Livraison finale envoyee. En attente de cloture client.', 'Fermer', { duration: 2600 });
          } else {
            this.snackBar.open(res?.message || 'Impossible de soumettre la livraison finale', 'Fermer', { duration: 2600 });
          }
        },
        error: (err: any) => {
          this.submittingFinal.set(false);
          this.snackBar.open(err?.error?.message || err?.message || 'Impossible de soumettre la livraison finale', 'Fermer', { duration: 2600 });
        }
      });
  }
}
