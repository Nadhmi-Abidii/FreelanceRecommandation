import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import {
  CreateUserInput,
  ManagedRole,
  ManagedStatus
} from '../../../../core/users/users.service';

export type AdminUserDialogResult = CreateUserInput;

@Component({
  standalone: true,
  selector: 'app-admin-user-dialog',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './admin-user-dialog.component.html',
  styleUrls: ['./admin-user-dialog.component.scss']
})
export default class AdminUserDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef =
    inject<MatDialogRef<AdminUserDialogComponent, AdminUserDialogResult>>(MatDialogRef);

  loading = false;

  form = this.fb.group({
    firstName: [''],
    lastName: [''],
    email: ['', [Validators.required, Validators.email]],
    role: ['ROLE_CLIENT' as ManagedRole, Validators.required],
    status: ['Actif' as ManagedStatus, Validators.required],
    password: ['', [Validators.required, Validators.minLength(6)]]
  });

  close() {
    this.dialogRef.close();
  }

  submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.value as CreateUserInput;
    this.loading = true;
    this.dialogRef.close(value);
  }
}
