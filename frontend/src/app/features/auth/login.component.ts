import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';

import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { HttpErrorResponse } from '@angular/common/http';

import { AuthService } from '../../core/auth/auth';

@Component({
  standalone: true,
  selector: 'app-login',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    RouterLinkActive,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatCheckboxModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule
  ],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export default class LoginComponent {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private auth = inject(AuthService);
  private snackBar = inject(MatSnackBar);

  loading = signal(false);
  hidePassword = signal(true);
  submitted = signal(false);
  errorMessage = signal(''); // utilisé pour la petite alerte rouge

  form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    remember: [true]
  });

  emailError = computed(() => {
    const c = this.form.controls.email;
    if (!c.touched && !this.submitted()) return '';
    if (c.hasError('required')) return 'Email is required';
    if (c.hasError('email')) return 'Please enter a valid email';
    return '';
  });

  passwordError = computed(() => {
    const c = this.form.controls.password;
    if (!c.touched && !this.submitted()) return '';
    if (c.hasError('required')) return 'Password is required';
    if (c.hasError('minlength')) return 'Minimum 6 characters';
    return '';
  });

  onSubmit() {
    this.submitted.set(true);
    this.errorMessage.set('');

    if (this.form.invalid || this.loading()) return;

    this.loading.set(true);
    const { email, password, remember } = this.form.getRawValue();

    this.auth.login(email!, password!).subscribe({
      next: (data) => {
        const shouldRemember = remember ?? true;
        this.auth.persistSession(data, shouldRemember);

        this.loading.set(false);

        // ✅ Snackbar succès
        this.snackBar.open('Connexion réussie ! Ravi de vous revoir 👋', 'Fermer', {
          duration: 3000,
          panelClass: ['snackbar-success']
        });

        const destination =
          data.role === 'ROLE_ADMIN'
            ? '/admin'
            : data.role === 'ROLE_FREELANCER'
              ? '/freelancer/dashboard'
              : '/dashboard';

        this.router.navigateByUrl(destination);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);

        // ✅ Message par défaut
        let msg = 'Unable to sign in. Please try again.';

        // 🔥 Cas credentials invalides (400 / 401 venant du backend)
        if (err.status === 400 || err.status === 401) {
          // si ton backend renvoie { message: '...' }
          msg = (err.error && (err.error.message || err.error.error)) ||
            'Email ou mot de passe incorrect.';
        }
        // 🌐 Problème de connexion / backend off
        else if (err.status === 0) {
          msg = 'Impossible de joindre le serveur. Vérifiez votre connexion.';
        }

        // On met le message propre dans l’alerte rouge
        this.errorMessage.set(msg);

        // ✅ Snackbar erreur (popup)
        this.snackBar.open(msg, 'Fermer', {
          duration: 4000,
          panelClass: ['snackbar-error']
        });
      }
    });
  }
}
