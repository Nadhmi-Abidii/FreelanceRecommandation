import { CommonModule } from '@angular/common';
import { Component, DestroyRef, Input, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink, RouterLinkActive } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatMenuModule } from '@angular/material/menu';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatBadgeModule } from '@angular/material/badge';
import { MatDividerModule } from '@angular/material/divider';

import { MilestoneDto, MilestoneService } from '../../../services/milestone.service';

@Component({
  standalone: true,
  selector: 'app-client-mission-milestones',
  templateUrl: './client-mission-milestones.component.html',
  styleUrls: ['./client-mission-milestones.component.scss'],
  imports: [
    CommonModule,
    RouterLink,
    RouterLinkActive,
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
    MatSnackBarModule,
    MatToolbarModule,
    MatMenuModule,
    MatSlideToggleModule,
    MatBadgeModule,
    MatDividerModule
  ]
})
export default class ClientMissionMilestonesComponent implements OnInit {
  @Input() missionId: number | null | undefined;

  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(MilestoneService);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);

  readonly resolvedMissionId = signal<number | null>(null);
  readonly milestones = signal<MilestoneDto[]>([]);
  readonly loading = signal(false);
  readonly creating = signal(false);
  readonly actionLoading = signal(false);
  readonly error = signal<string | null>(null);
  readonly showForm = signal(false);

  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.minLength(3)]],
    description: [''],
    amount: [null as number | null, [Validators.required, Validators.min(1)]],
    dueDate: ['']
  });

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
      .listForMission(missionId)
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

  toggleForm() {
    this.showForm.update(value => !value);
  }

  create() {
    const missionId = this.resolvedMissionId();
    if (!missionId) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const payload: MilestoneDto = {
      mission: { id: missionId },
      title: this.form.controls.title.value,
      description: this.form.controls.description.value ?? '',
      amount: this.form.controls.amount.value ?? 0,
      dueDate: this.form.controls.dueDate.value || null
    };

    this.creating.set(true);
    // tu as modifié le service pour prendre missionId en 1er param
    this.service
      .create(missionId, payload)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.creating.set(false);
          if (!res.success || !res.data) {
            this.snackBar.open(res.message || 'Creation impossible.', 'Fermer', { duration: 2400 });
            return;
          }
          this.form.reset({ title: '', description: '', amount: null, dueDate: '' });
          if (missionId) {
            this.load(missionId);
          }
          this.showForm.set(false);
          this.snackBar.open('Jalon ajoute.', 'Fermer', { duration: 2000 });
        },
        error: err => {
          this.creating.set(false);
          const message = err?.error?.message || err?.message || 'Erreur reseau.';
          this.snackBar.open(message, 'Fermer', { duration: 2600 });
        }
      });
  }

  validate(id: number) {
    if (!confirm('Valider ce jalon et declencher le paiement ?')) {
      return;
    }
    this.runAction(this.service.validate(id), 'Jalon valide.');
  }

  refuse(id: number) {
    if (!confirm('Refuser ce jalon ?')) {
      return;
    }
    this.runAction(this.service.reject(id), 'Jalon refuse.');
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

  // 🔹 pour le mode sombre de la navbar
  toggleDark(enabled: boolean) {
    const root = document.documentElement;
    if (enabled) {
      root.classList.add('dash-dark');
    } else {
      root.classList.remove('dash-dark');
    }
  }
}
