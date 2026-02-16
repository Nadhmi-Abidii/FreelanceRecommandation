import { CommonModule } from '@angular/common';
import { Component, DestroyRef, Input, OnChanges, SimpleChanges, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { FeedbackDirection, FeedbackResponse, FeedbackService } from '../../../services/feedback.service';

@Component({
  selector: 'app-mission-feedback',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSnackBarModule
  ],
  templateUrl: './mission-feedback.component.html',
  styleUrls: ['./mission-feedback.component.scss']
})
export default class MissionFeedbackComponent implements OnChanges {
  @Input({ required: true }) missionId!: number;
  @Input({ required: true }) mode!: 'client' | 'freelancer';
  @Input() missionStatus: string | null | undefined = null;

  private readonly feedbackService = inject(FeedbackService);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);

  readonly feedback = signal<FeedbackResponse | null>(null);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly maxCommentLength = 2000;
  readonly stars = [1, 2, 3, 4, 5];

  readonly form = this.fb.nonNullable.group({
    rating: [0, [Validators.required, Validators.min(1), Validators.max(5)]],
    comment: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(this.maxCommentLength)]]
  });

  get showSection(): boolean {
    return (this.missionStatus || '').toUpperCase() === 'COMPLETED';
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (!this.showSection || !this.missionId) return;
    if (changes['missionId'] || changes['missionStatus']) {
      this.loadMyFeedback();
    }
  }

  setRating(value: number) {
    this.form.controls.rating.setValue(value);
  }

  loadMyFeedback() {
    this.loading.set(true);
    this.error.set(null);
    this.feedbackService.getMyFeedback(this.missionId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.loading.set(false);
          if (res.success && res.data) {
            this.feedback.set(res.data);
          } else {
            this.feedback.set(null);
          }
        },
        error: err => {
          this.loading.set(false);
          if (err?.status === 404) {
            this.feedback.set(null);
            return;
          }
          const message = err?.error?.message || err?.message || 'Impossible de charger le feedback';
          this.error.set(message);
        }
      });
  }

  submit() {
    if (!this.missionId) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.error.set(null);

    const payload = {
      rating: this.form.controls.rating.value,
      comment: (this.form.controls.comment.value || '').trim() || undefined,
      direction: this.mode === 'client'
        ? ('CLIENT_TO_FREELANCER' as FeedbackDirection)
        : ('FREELANCER_TO_CLIENT' as FeedbackDirection)
    };

    this.feedbackService.createFeedback(this.missionId, payload)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.saving.set(false);
          if (res.success && res.data) {
            this.feedback.set(res.data);
            this.snackBar.open('Feedback envoye', 'Fermer', { duration: 2500 });
          } else {
            const message = res.message || 'Impossible denvoyer le feedback';
            this.error.set(message);
            this.snackBar.open(message, 'Fermer', { duration: 2600 });
          }
        },
        error: err => {
          this.saving.set(false);
          const message = err?.error?.message || err?.message || 'Impossible denvoyer le feedback';
          this.error.set(message);
          this.snackBar.open(message, 'Fermer', { duration: 2800 });
        }
      });
  }
}
