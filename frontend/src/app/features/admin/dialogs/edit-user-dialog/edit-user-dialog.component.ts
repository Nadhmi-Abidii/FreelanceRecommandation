import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { ReactiveFormsModule, FormBuilder, Validators, FormGroup, FormControl } from '@angular/forms';
import { MatChipsModule } from '@angular/material/chips';

// adapte ce type d'import à ton projet si besoin
import type { ManagedUser } from '../../admin-dashboard.component';

export interface EditUserDialogData { user: ManagedUser; }
export interface EditUserDialogResult {
  name: string;
  email: string;
  role: 'ROLE_CLIENT' | 'ROLE_FREELANCER' | 'ROLE_ADMIN';
  status: 'Actif' | 'Inactif';
}

type EditUserForm = FormGroup<{
  name: FormControl<string>;
  email: FormControl<string>;
  role: FormControl<EditUserDialogResult['role']>;
  status: FormControl<EditUserDialogResult['status']>;
}>;

@Component({
  standalone: true,
  selector: 'app-edit-user-dialog',
  imports: [
    CommonModule, MatDialogModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, ReactiveFormsModule, MatChipsModule
  ],
  template: `
    <h2 mat-dialog-title>
      <mat-icon>edit</mat-icon>&nbsp;Modifier l’utilisateur
    </h2>

    <div mat-dialog-content class="dialog-body">
      <form [formGroup]="form" class="form-grid" (ngSubmit)="save()">
        <mat-form-field appearance="outline">
          <mat-label>Nom complet</mat-label>
          <input matInput formControlName="name" maxlength="80" />
          <mat-hint align="end">{{ form.controls.name.value.length || 0 }}/80</mat-hint>
          <mat-error *ngIf="form.controls.name.hasError('required')">Nom requis</mat-error>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Email</mat-label>
          <input matInput formControlName="email" type="email" />
          <mat-error *ngIf="form.controls.email.hasError('email')">Email invalide</mat-error>
          <mat-error *ngIf="form.controls.email.hasError('required')">Email requis</mat-error>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Rôle</mat-label>
          <mat-select formControlName="role">
            <mat-option value="ROLE_CLIENT">Client</mat-option>
            <mat-option value="ROLE_FREELANCER">Freelance</mat-option>
            <mat-option value="ROLE_ADMIN">Administrateur</mat-option>
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Statut</mat-label>
          <mat-select formControlName="status">
            <mat-option value="Actif">Actif</mat-option>
            <mat-option value="Inactif">Inactif</mat-option>
          </mat-select>
        </mat-form-field>

        <div class="meta">
          <mat-chip>
            <mat-icon>calendar_today</mat-icon>
            &nbsp;Inscrit le {{ joinedLabel }}
          </mat-chip>
          <mat-chip color="primary" selected>
            <mat-icon>work</mat-icon>
            &nbsp;{{ data.user.missions }} missions
          </mat-chip>
        </div>
      </form>
    </div>

    <div mat-dialog-actions align="end">
      <button mat-stroked-button (click)="close()">
        <mat-icon>close</mat-icon>&nbsp;Annuler
      </button>
      <button mat-flat-button color="primary" [disabled]="form.invalid" (click)="save()">
        <mat-icon>save</mat-icon>&nbsp;Enregistrer
      </button>
    </div>
  `,
  styles: [`
    .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
    @media (max-width: 600px){ .form-grid{ grid-template-columns: 1fr; } }
    .meta { display: flex; gap: 8px; margin-top: 6px; }
  `]
})
export default class EditUserDialogComponent {
  form!: EditUserForm;
  joinedLabel = '';

  constructor(
    private ref: MatDialogRef<EditUserDialogComponent, EditUserDialogResult>,
    @Inject(MAT_DIALOG_DATA) public data: EditUserDialogData,
    private fb: FormBuilder
  ) {
    // Initialisation dans le constructeur (l'injection de 'data' est disponible ici)
    this.form = this.fb.group({
      name: this.fb.nonNullable.control<string>(data.user.name, [Validators.required, Validators.maxLength(80)]),
      email: this.fb.nonNullable.control<string>(data.user.email, [Validators.required, Validators.email]),
      role: this.fb.nonNullable.control<EditUserDialogResult['role']>(data.user.role as EditUserDialogResult['role'], [Validators.required]),
      status: this.fb.nonNullable.control<EditUserDialogResult['status']>(data.user.status as EditUserDialogResult['status'], [Validators.required]),
    });

    this.joinedLabel = Intl.DateTimeFormat('fr-FR', { dateStyle: 'medium' })
      .format(new Date(data.user.joined));
  }

  close() { this.ref.close(); }

  save() {
    if (this.form.invalid) return;
    const v = this.form.getRawValue(); // v est bien typé
    const result: EditUserDialogResult = {
      name: v.name,
      email: v.email,
      role: v.role,
      status: v.status
    };
    this.ref.close(result);
  }
}
