import { CommonModule } from '@angular/common';
import { Component, Input, OnChanges, SimpleChanges, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { FeedbackResponse, FeedbackService } from '../../../services/feedback.service';
import { MissionFeedbackDialogComponent, MissionFeedbackDialogData, MissionFeedbackDialogResult } from './mission-feedback-dialog.component';

type FeedbackContext = 'client' | 'freelancer';

interface MissionLike {
  id: number;
  status?: string | null;
  title?: string | null;
}

@Component({
  standalone: true,
  selector: 'app-mission-feedback-button',
  templateUrl: './mission-feedback-button.component.html',
  styleUrls: ['./mission-feedback-button.component.scss'],
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatDialogModule,
    MatSnackBarModule
  ]
})
export class MissionFeedbackButtonComponent implements OnChanges {
  @Input({ required: true }) mission!: MissionLike | null;
  @Input({ required: true }) context!: FeedbackContext;

  private readonly feedbackService = inject(FeedbackService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly feedback = signal<FeedbackResponse | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  readonly isCompleted = computed(() => {
    const status = (this.mission?.status || '').toString().toUpperCase();
    return status === 'COMPLETED' || status === 'FINISHED' || status === 'TERMINATED';
  });

  readonly buttonLabel = computed(() => {
    return this.context === 'client'
      ? 'Donner un feedback au freelancer'
      : 'Donner un feedback au client';
  });

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['mission'] && this.isCompleted()) {
      this.loadMyFeedback();
    }
  }

  loadMyFeedback() {
    if (!this.mission?.id) return;
    this.loading.set(true);
    this.error.set(null);
    this.feedbackService.getMyFeedback(this.mission.id).subscribe({
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
        if (err?.status === 404 || err?.status === 204) {
          this.feedback.set(null);
          return;
        }
        this.error.set(err?.error?.message || err?.message || 'Impossible de charger le feedback.');
      }
    });
  }

  openDialog() {
    if (!this.mission?.id) return;
    const data: MissionFeedbackDialogData = {
      title: this.mission.title ?? 'Feedback',
      context: this.context
    };
    const ref = this.dialog.open<MissionFeedbackDialogComponent, MissionFeedbackDialogData, MissionFeedbackDialogResult>(
      MissionFeedbackDialogComponent,
      {
        width: '420px',
        data
      }
    );

    ref.afterClosed().subscribe(result => {
      if (!result || !result.rating) return;
      this.submitFeedback(result.rating, result.comment);
    });
  }

  private submitFeedback(rating: number, comment?: string | null) {
    if (!this.mission?.id) return;
    this.loading.set(true);
    this.feedbackService.createFeedback(this.mission.id, { rating, comment })
      .subscribe({
        next: res => {
          this.loading.set(false);
          if (res.success && res.data) {
            this.feedback.set(res.data);
            this.snackBar.open('Feedback envoyé', 'Fermer', { duration: 2500 });
          } else {
            this.snackBar.open(res.message || 'Impossible d\'envoyer le feedback', 'Fermer', { duration: 2600 });
          }
        },
        error: err => {
          this.loading.set(false);
          const message = err?.error?.message || err?.message || 'Impossible d\'envoyer le feedback';
          this.snackBar.open(message, 'Fermer', { duration: 2800 });
        }
      });
  }

  stars(count: number) {
    return Array.from({ length: count }).map((_, idx) => idx + 1);
  }
}
