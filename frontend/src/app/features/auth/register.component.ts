import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidatorFn, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink, RouterLinkActive } from '@angular/router';

import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { AuthService } from '../../core/auth/auth';

@Component({
  standalone: true,
  selector: 'app-register',
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
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss']
})
export default class RegisterComponent {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private auth = inject(AuthService);
  private snackBar = inject(MatSnackBar);

  loading = signal(false);
  submitted = signal(false);
  hidePassword = signal(true);
  hideConfirmPassword = signal(true);
  errorMessage = signal('');
  view = signal<'select' | 'form'>('select');
  typeSelectionError = signal(false);
  freelancerOnly = false;

  // ---- Form & Validators
  private readonly passwordsMatchValidator: ValidatorFn = (group: AbstractControl) => {
    const password = group.get('password')?.value;
    const confirmPassword = group.get('confirmPassword')?.value;
    if (!password || !confirmPassword) return null;
    return password === confirmPassword ? null : { passwordMismatch: true };
  };

  form = this.fb.group(
    {
      type: [null as 'client' | 'freelancer' | null, Validators.required],
      firstName: ['', [Validators.required, Validators.maxLength(40)]],
      lastName: ['', [Validators.required, Validators.maxLength(40)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required]],
      phone: [''],
      address: [''],
      // client
      city: [''],
      // freelancer
      hourlyRate: [null],
      availability: [''],
      agree: [false, Validators.requiredTrue]
    },
    { validators: this.passwordsMatchValidator }
  );

  constructor() {
    this.applyRouteDefaults();
  }

  private applyRouteDefaults() {
    this.freelancerOnly = this.route.snapshot.data['freelancerOnly'] === true;
    const typeFromRoute = (this.route.snapshot.data['defaultType']
      ?? this.route.snapshot.queryParamMap.get('type')) as 'client' | 'freelancer' | null;

    if (this.freelancerOnly && typeFromRoute !== 'freelancer') {
      this.selectType('freelancer');
      return;
    }

    if (typeFromRoute === 'client' || typeFromRoute === 'freelancer') {
      this.selectType(typeFromRoute);
    }
  }

  // ---- Errors
  firstNameError = computed(() => {
    const c = this.form.controls.firstName;
    if (!c.touched && !this.submitted()) return '';
    if (c.hasError('required')) return 'First name is required';
    if (c.hasError('maxlength')) return 'Maximum 40 characters';
    return '';
  });

  lastNameError = computed(() => {
    const c = this.form.controls.lastName;
    if (!c.touched && !this.submitted()) return '';
    if (c.hasError('required')) return 'Last name is required';
    if (c.hasError('maxlength')) return 'Maximum 40 characters';
    return '';
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

  confirmPasswordError = computed(() => {
    const c = this.form.controls.confirmPassword;
    if (!c.touched && !this.submitted() && !this.form.hasError('passwordMismatch')) return '';
    if (c.hasError('required')) return 'Please confirm your password';
    if (this.form.hasError('passwordMismatch')) return 'Passwords must match';
    return '';
  });

  // ---- Derived state
  selectedType = computed(() => this.form.controls.type.value as 'client' | 'freelancer' | null);

  // ---- UI actions
  selectType(type: 'client' | 'freelancer') {
    if (this.freelancerOnly && type !== 'freelancer') {
      return;
    }
    this.form.controls.type.setValue(type);
    this.typeSelectionError.set(false);

    // 👉 Afficher directement le formulaire
    this.view.set('form');

    if (type === 'client') {
      this.form.patchValue({ hourlyRate: null, availability: '' });
    } else {
      this.form.patchValue({ city: '' });
    }
  }

  goToForm() {
    if (!this.selectedType()) {
      this.typeSelectionError.set(true);
      return;
    }
    this.typeSelectionError.set(false);
    this.view.set('form');
    this.submitted.set(false);
    this.errorMessage.set('');
  }

  backToSelection() {
    this.view.set('select');
    this.submitted.set(false);
    this.errorMessage.set('');
    // reset léger si tu veux tout nettoyer :
    // this.form.reset({ type: null, agree: false });
  }

  // ---- Submit
  onSubmit() {
    this.submitted.set(true);
    this.errorMessage.set('');
    if (this.form.invalid || this.loading()) return;

    this.loading.set(true);
    const raw = this.form.getRawValue();
    const payload: any = {
      firstName: raw.firstName!,
      lastName: raw.lastName!,
      email: raw.email!,
      password: raw.password!,
      phone: raw.phone?.toString().trim() || null,
      address: raw.address?.toString().trim() || null
    };

    if (raw.type === 'client') {
      payload.city = raw.city?.toString().trim() || null;
    } else {
      const parsedRate = raw.hourlyRate === '' || raw.hourlyRate === null || raw.hourlyRate === undefined
        ? 0
        : Number(raw.hourlyRate);
      payload.hourlyRate = Number.isFinite(parsedRate) ? parsedRate : 0;
      const availability = raw.availability?.toString().trim();
      payload.availability = availability || 'Disponible';
    }

    this.auth.register(payload, raw.type as 'client' | 'freelancer').subscribe({
      next: () => {
        this.loading.set(false);
        const ref = this.snackBar.open('Account created successfully 🎉', 'OK', {
          duration: 1800,
          panelClass: ['snackbar-success']
        });
        ref.afterDismissed().subscribe(() => {
          this.router.navigateByUrl('/login');
        });

        this.form.reset({ type: null, agree: false });
        this.view.set('select');
        this.typeSelectionError.set(false);
      },
      error: (error) => {
        this.loading.set(false);
        const message = error?.message ?? 'Unable to create your account. Please try again.';
        this.errorMessage.set(message);
      }
    });
  }
}
