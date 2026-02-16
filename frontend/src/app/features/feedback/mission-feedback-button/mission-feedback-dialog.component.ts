import { CommonModule } from '@angular/common';
import { Component, Inject, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

export interface MissionFeedbackDialogData {
  title: string;
  context: 'client' | 'freelancer';
}

export interface MissionFeedbackDialogResult {
  rating: number;
  comment?: string | null;
}

@Component({
  standalone: true,
  selector: 'app-mission-feedback-dialog',
  templateUrl: './mission-feedback-dialog.component.html',
  styleUrls: ['./mission-feedback-dialog.component.scss'],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule
  ]
})
export class MissionFeedbackDialogComponent {
  private readonly fb = inject(FormBuilder);
  readonly stars = [1, 2, 3, 4, 5];

  form = this.fb.nonNullable.group({
    rating: [5, [Validators.required, Validators.min(1), Validators.max(5)]],
    comment: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(2000)]]
  });

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: MissionFeedbackDialogData,
    private readonly ref: MatDialogRef<MissionFeedbackDialogComponent, MissionFeedbackDialogResult>
  ) {}

  setRating(value: number) {
    this.form.controls.rating.setValue(value);
  }

  submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.ref.close({
      rating: this.form.controls.rating.value,
      comment: (this.form.controls.comment.value || '').trim() || undefined
    });
  }

  cancel() {
    this.ref.close();
  }
}
